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
import java.text.DateFormat;

import flexjson.JSONSerializer;

import org.apache.ibatis.session.SqlSession;

import gov.alaska.dggs.photodb.PhotoDBFactory;
import gov.alaska.dggs.photodb.model.Image;
import gov.alaska.dggs.photodb.model.Tag;


public class ImageServlet extends HttpServlet
{
	private static JSONSerializer serializer;
	static { serializer = new JSONSerializer(); }


	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		ServletContext context = getServletContext();

		// Aggressively disable cache
		response.setHeader("Cache-Control","no-cache");
		response.setHeader("Pragma","no-cache");
		response.setDateHeader("Expires", 0);

		SqlSession sess = PhotoDBFactory.openSession();
		try {
			String ids[] = request.getParameterValues("image_id");
			for(String id : ids){
				Image image = sess.selectOne(
					"gov.alaska.dggs.photodb.Image.getByID", 
					Integer.valueOf(id)
				);
				if(image == null) throw new Exception("Image not found.");

				if("delete".equals(request.getParameter("action"))){
					sess.delete("gov.alaska.dggs.photodb.Image.deleteAllTags", image);
					int r = sess.delete("gov.alaska.dggs.photodb.Image.delete", image);
					if(r < 1) throw new Exception("Delete failed.");
				} else {
					// update tags first
					List<Tag> c_tags = sess.selectList(
						"gov.alaska.dggs.photodb.Tag.getByImageID", image.getID()
					);

					// Find all the tags provided by the user
					// Add them if they don't already exist.
					String str_tags = request.getParameter("tags");
					if(str_tags != null){
						String tags[] = str_tags.split(",");
						for(String t : tags){
							String tx = t.trim().toLowerCase();
							if(tx.length() > 0){
								boolean contains = false;

								for(Tag tag : c_tags){
									if(tag.getName().equals(tx)){
										contains = true;
										break;
									}
								}

								if(!contains){
									Tag tag = sess.selectOne(
										"gov.alaska.dggs.photodb.Tag.getByName", tx
									);

									if(tag == null){
										tag = new Tag();
										tag.setName(tx);
										sess.insert("gov.alaska.dggs.photodb.Tag.insert", tag);
										if(tag.getID() == null){
											throw new Exception("Tag insert failed.");
										}
									}

									HashMap m = new HashMap();
									m.put("image", image);
									m.put("tag", tag);
									sess.insert("gov.alaska.dggs.photodb.Image.addTag", m);
								}
							}
						}


						for(Tag tag : c_tags){
							boolean contains = false;
							for(String t : tags){
								String tx = t.trim().toLowerCase();
								if(tag.getName().equals(tx)){
									contains = true;
								}
							}

							if(!contains){
								HashMap m = new HashMap();
								m.put("image", image);
								m.put("tag", tag);
								sess.delete("gov.alaska.dggs.photodb.Image.removeTag", m);
							}
						}
					}

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

					String taken = request.getParameter("taken");
					if(taken != null){
						DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
						try {
							image.setTaken(df.parse(taken));
						} catch(Exception ex){
							image.setTaken(null);
						}
					}

					int r = sess.update("gov.alaska.dggs.photodb.Image.update", image);
					if(r < 1) throw new Exception("Image update failed.");
				}
			}

			sess.commit();

			HashMap out = new HashMap();
			out.put("success", true);

			response.setContentType("application/json");
			serializer.serialize(out, response.getWriter());

		} catch(Exception ex){
			sess.rollback();

			response.setStatus(500);
			response.setContentType("text/plain");
			response.getOutputStream().print(ex.getMessage());
			ex.printStackTrace();
		} finally {
			sess.close();	
		}
	}
}
