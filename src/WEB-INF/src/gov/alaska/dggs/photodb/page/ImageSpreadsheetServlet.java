package gov.alaska.dggs.photodb.page;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import gov.alaska.dggs.photodb.PhotoDBFactory;
import gov.alaska.dggs.photodb.model.Image;


public class ImageSpreadsheetServlet extends HttpServlet
{
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		ServletContext context = getServletContext();
		try (SqlSession sess = PhotoDBFactory.openSession()) {
			String path = request.getPathInfo();
			if(path == null || path.length() < 2){
				throw new Exception("No IDs provided.");
			}
			
			List<Integer> ids = new ArrayList<Integer>();
			for(String sid : path.substring(1).split(",")){
				ids.add(Integer.valueOf(sid));
			}
			if(ids.isEmpty()) throw new Exception("No IDs provided.");

      List<Image> images = sess.selectList(
				"gov.alaska.dggs.photodb.Image.getByIDs", ids
			);
			request.setAttribute("images", images);
			request.getRequestDispatcher(
				"/WEB-INF/tmpl/spreadsheet.jsp"
			).forward(request, response);
		} catch(Exception ex) {
			throw new ServletException(ex);
		}
	}
}
