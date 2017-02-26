package gov.alaska.dggs.photodb.api;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import mjson.Json;

import org.apache.ibatis.session.SqlSession;

import gov.alaska.dggs.photodb.PhotoDBFactory;
import gov.alaska.dggs.photodb.model.Image;
import gov.alaska.dggs.photodb.model.Tag;

import gov.alaska.dggs.solr.SolrUpdate;


public class ImageServlet extends HttpServlet
{
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		ServletContext context = getServletContext();
		String solr_url = context.getInitParameter("solr_url");

		// Aggressively disable cache
		response.setHeader("Cache-Control","no-cache");
		response.setHeader("Pragma","no-cache");
		response.setDateHeader("Expires", 0);

		SqlSession sess = PhotoDBFactory.openSession();
		try {
			SolrUpdate solr = new SolrUpdate(solr_url);

			String ids = request.getParameter("ids");
			if(ids == null) throw new Exception("No image ids found.");

			for(String id : ids.split(",")){
				Image image = sess.selectOne(
					"gov.alaska.dggs.photodb.Image.getByID", 
					Integer.valueOf(id)
				);
				if(image == null) throw new Exception("Image not found.");

				if("delete".equals(request.getParameter("action"))){
					sess.delete("gov.alaska.dggs.photodb.Image.deleteAllTags", image);
					int r = sess.delete("gov.alaska.dggs.photodb.Image.delete", image);
					if(r < 1) throw new Exception("Delete failed.");
					solr.delete(image.getID());
				} else {
					List<Tag> otags = new LinkedList<Tag>();
					if(image.getTags() != null) otags.addAll(image.getTags());

					Set<Tag> ttags = new HashSet<Tag>(50);
					String str_tags = request.getParameter("tags");
					if(str_tags != null){
						String tags[] = str_tags.split(",");
						for(String t : tags){
							String tx = t.trim().toLowerCase();
							if(tx.length() > 0){
								Tag tag = getOrAddTag(tx);
								ttags.add(tag);
							}
						}
					}
					List<Tag> ntags = new LinkedList<Tag>(ttags);

					// Generate a list of tags that need to be removed
					// and remove them
					List<Tag> remove_tags = new LinkedList<Tag>(otags);
					remove_tags.removeAll(ntags);
					for(Tag tag : remove_tags){
						HashMap m = new HashMap();
						m.put("image", image);
						m.put("tag", tag);
						sess.delete("gov.alaska.dggs.photodb.Image.removeTag", m);
					}

					// Generate a list of tags that need to be added
					// and add them
					List<Tag> add_tags = new LinkedList<Tag>(ntags);
					add_tags.removeAll(otags);
					for(Tag tag : add_tags){
						HashMap m = new HashMap();
						m.put("image", image);
						m.put("tag", tag);
						sess.insert("gov.alaska.dggs.photodb.Image.addTag", m);
					}

					// Set a list of new tags and push that into image
					image.setTags(ntags);

					// Then update image
					String summary = request.getParameter("summary");
					if(summary != null){
						summary = summary.trim();
						image.setSummary(summary.length() > 0 ? summary : null);
					}

					String description = request.getParameter("description");
					if(description != null){
						description = description.trim();
						image.setDescription(description.length() > 0 ? description : null);
					}

					String credit = request.getParameter("credit");
					if(credit != null){
						credit = credit.trim();
						image.setCredit(credit.length() > 0 ? credit : null);
					}

					String geojson = request.getParameter("geojson");
					if(geojson != null){
						geojson = geojson.trim();
						image.setGeoJSON(geojson.length() > 0 ? geojson : null);
					}

					String accuracy = request.getParameter("accuracy");
					if(accuracy != null){
						image.setAccuracy(Integer.valueOf(accuracy));
					}

					String taken = request.getParameter("taken");
					if(taken != null){
						DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
						try {
							image.setTaken(df.parse(taken));
						} catch(Exception ex){
							image.setTaken(null);
						}
					}

					String ispublic = request.getParameter("ispublic");
					if(ispublic != null){
						image.setIsPublic(Boolean.valueOf(ispublic));
					}

					int r = sess.update("gov.alaska.dggs.photodb.Image.update", image);
					if(r < 1) throw new Exception("Image update failed.");

					SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyy-MM-dd'T'HH:mm:ss'Z'"
					);
					Json json = Json.object("id", image.getID());
					if(image.getSummary() != null){
						json.set("title", image.getSummary());
					}
					if(image.getCredit() != null){
						json.set("credit", image.getCredit());
					}
					if(image.getDescription() != null){
						json.set("description", image.getDescription());
					}
					if(image.getEntered() != null){
						json.set("entered", sdf.format(image.getEntered()));
					}
					if(image.getTaken() != null){
						json.set("entered", sdf.format(image.getTaken()));
					}
					if(image.getFilename() != null){
						json.set("filename", image.getFilename());
					}
					if(image.getGeoJSON() != null){
						json.set("geog", image.getGeoJSON());
					}
					if(image.getIsPublic() != null){
						json.set("ispublic", image.getIsPublic());
					}
					if(!image.getTags().isEmpty()){
						Json arr = Json.array();
						for(Tag tag : image.getTags()){
							arr.add(tag.getName());
						}
						json.set("tags", arr);
					}
					solr.add(json);
				}
			}

			sess.commit();

			HashMap out = new HashMap();
			out.put("success", true);

			response.setContentType("application/json");
			response.getOutputStream().print(
				Json.object("success", true).toString()
			);
		} catch(Exception ex){
			sess.rollback();

			response.setStatus(500);
			response.setContentType("text/plain");
			response.getOutputStream().print(ex.getMessage());
		} finally {
			sess.close();	
		}
	}


	public synchronized Tag getOrAddTag(String name) throws Exception
	{
		try (SqlSession sess = PhotoDBFactory.openSession()){
			Tag tag = sess.selectOne(
				"gov.alaska.dggs.photodb.Tag.getByName", name
			);

			if(tag == null){
				tag = new Tag();
				tag.setName(name);
				sess.insert("gov.alaska.dggs.photodb.Tag.insert", tag);
				if(tag.getID() == null) throw new Exception("Tag insert failed.");
				sess.commit();
			}

			return tag;
		}
	}
}
