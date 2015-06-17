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

import java.util.zip.GZIPOutputStream;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import java.sql.Array;

import flexjson.JSONSerializer;
import flexjson.transformer.DateTransformer;

import org.apache.ibatis.session.SqlSession;

import gov.alaska.dggs.photodb.PhotoDBFactory;
import gov.alaska.dggs.photodb.model.Image;
import gov.alaska.dggs.transformer.ExcludeTransformer;
import gov.alaska.dggs.transformer.IterableTransformer;


public class ImageDetailServlet extends HttpServlet
{
	private static JSONSerializer serializer;
	static {
		serializer = new JSONSerializer();
		serializer.include("image_ids");
		serializer.include("credit");
		serializer.include("taken");
		serializer.include("summary");
		serializer.include("description");
		serializer.include("geojson");
		serializer.include("tags");

		serializer.exclude("class");

		serializer.transform(new DateTransformer("M/d/yyyy"), Date.class);
		serializer.transform(new ExcludeTransformer(), void.class);
		serializer.transform(new IterableTransformer(), Iterable.class);
	}


	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPostGet(request,response); }
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPostGet(request,response); }


	@SuppressWarnings("unchecked")
	public void doPostGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		ServletContext context = getServletContext();

		// Aggressively disable cache
		response.setHeader("Cache-Control","no-cache");
		response.setHeader("Pragma","no-cache");
		response.setDateHeader("Expires", 0);

		SqlSession sess = PhotoDBFactory.openSession();
		try {
			List<Integer> ids = new ArrayList<Integer>();
			String[] strids = request.getParameterValues("image_id[]");
			for(String sid : strids){
				try { ids.add(Integer.valueOf(sid)); }
				catch(Exception ex){
					// Explicitly do nothing
				}
			}

			Map output = sess.selectOne(
				"gov.alaska.dggs.photodb.Image.getCommonByID", ids
			);

			// Fix java.sql.Arrrays - I probably should write
			// a transformer for this
			Array pid = (Array)output.get("image_ids");
			if(pid != null){
				output.put("image_ids", pid.getArray());
			}

			OutputStreamWriter out = null;
			GZIPOutputStream gos = null;
			try { 
				// If GZIP is supported by the requesting browser, use it.
				String encoding = request.getHeader("Accept-Encoding");
				if(encoding != null && encoding.contains("gzip")){
					response.setHeader("Content-Encoding", "gzip");
					gos = new GZIPOutputStream(response.getOutputStream(), 8196);
					out = new OutputStreamWriter(gos, "utf-8");
				} else {
					out = new OutputStreamWriter(response.getOutputStream(), "utf-8");
				}

				response.setContentType("application/json");
				serializer.serialize(output, out);
			} finally {
				if(out != null){ out.close(); }
				if(gos != null){ gos.close(); }
			}
		} catch(Exception ex){
			response.setStatus(500);
			response.setContentType("text/plain");
			response.getOutputStream().print(ex.getMessage());
		} finally {
			sess.close();	
		}
	}
}
