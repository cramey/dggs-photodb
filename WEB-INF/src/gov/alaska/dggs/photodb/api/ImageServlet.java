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
import java.text.DateFormat;

import flexjson.JSONSerializer;

import org.apache.ibatis.session.SqlSession;

import gov.alaska.dggs.photodb.PhotoDBFactory;
import gov.alaska.dggs.photodb.model.Image;


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
			Image image = new Image();
			image.setID(Integer.parseInt(request.getParameter("ID")));

			if("delete".equals(request.getParameter("action"))){
				int r = sess.delete("gov.alaska.dggs.photodb.Image.delete", image);
				if(r < 1) throw new Exception("Delete failed.");
			} else {
				image.setDescription(request.getParameter("description"));
				image.setCredit(request.getParameter("credit"));
				image.setGeoJSON(request.getParameter("geojson"));

				if(request.getParameter("date") != null){
					DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
					image.setDate(df.parse(request.getParameter("date")));
				}

				int r = sess.update("gov.alaska.dggs.photodb.Image.update", image);
				if(r < 1) throw new Exception("Update failed.");
			}

			sess.commit();

			HashMap out = new HashMap();
			out.put("success", true);

			response.setContentType("application/json");
			serializer.serialize(out, response.getWriter());

		} catch(Exception ex){
			response.setStatus(500);
			response.setContentType("text/plain");
			response.getOutputStream().print(ex.getMessage());
			ex.printStackTrace();
		} finally {
			sess.close();	
		}
	}
}
