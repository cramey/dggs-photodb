package gov.alaska.dggs.photodb.page;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ibatis.session.SqlSession;

import gov.alaska.dggs.photodb.PhotoDBFactory;
import gov.alaska.dggs.photodb.model.Image;

public class ImageDetailServlet extends HttpServlet
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
			if(path == null){ throw new Exception("No ID provided."); }

			Integer id = Integer.valueOf(path.substring(1));

			Image image = sess.selectOne(
				"gov.alaska.dggs.photodb.Image.getByID", id
			);
			if(image == null) throw new Exception("ID not found.");
			if(!image.getIsPublic()) throw new Exception("Access denied.");

			request.setAttribute("image", image);
			request.getRequestDispatcher(
				"/WEB-INF/tmpl/detail.jsp"
			).forward(request, response);
		} catch(Exception ex) {
			throw new ServletException(ex);
		}
	}
}
