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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;

import flexjson.JSONSerializer;
import flexjson.transformer.DateTransformer;

import org.apache.ibatis.session.SqlSession;

import gov.alaska.dggs.photodb.PhotoDBFactory;
import gov.alaska.dggs.photodb.model.Image;
import gov.alaska.dggs.transformer.ExcludeTransformer;
import gov.alaska.dggs.transformer.IterableTransformer;
import gov.alaska.dggs.transformer.RawTransformer;


public class ImageListServlet extends HttpServlet
{
	private static JSONSerializer serializer;
	static {
		serializer = new JSONSerializer();
		serializer.include("tags");

		serializer.exclude("modified");
		serializer.exclude("entered");
		serializer.exclude("description");
		serializer.exclude("metadata");
		serializer.exclude("tags.class");
		serializer.exclude("class");

		serializer.transform(new DateTransformer("M/d/yyyy"), Date.class);
		serializer.transform(new ExcludeTransformer(), void.class);
		serializer.transform(new IterableTransformer(), Iterable.class);
		serializer.transform(new RawTransformer(), "geoJSON");
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
			HashMap map = new HashMap();

			String search = request.getParameter("search");
			if(search != null && search.length() > 0){
				map.put("search", request.getParameter("search"));
			}

			String aoi = request.getParameter("aoi");
			if(aoi != null && aoi.length() > 0){
				map.put("aoi", request.getParameter("aoi"));
			}

			String emptydesc = request.getParameter("emptydesc");
			if(emptydesc != null && emptydesc.length() > 0){
				map.put("emptydesc",
					Boolean.valueOf(request.getParameter("emptydesc"))
				);
			}

			String emptylocation = request.getParameter("emptylocation");
			if(emptylocation != null && emptylocation.length() > 0){
				map.put("emptylocation",
					Boolean.valueOf(request.getParameter("emptylocation"))
				);
			}

			Integer limit = 6;
			try { limit = Integer.valueOf(request.getParameter("show")); }
			catch(Exception ex){ }

			Integer page = 0;
			try { page = Integer.valueOf(request.getParameter("page")); }
			catch(Exception ex){ }

			map.put("limit", limit);
			map.put("offset", (limit * page));

			List output = sess.selectList(
				"gov.alaska.dggs.photodb.Image.getFromID", map
			);

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
