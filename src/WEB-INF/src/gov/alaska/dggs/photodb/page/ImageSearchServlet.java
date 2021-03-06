package gov.alaska.dggs.photodb.page;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

public class ImageSearchServlet extends HttpServlet
{
	private boolean pub;

	public void init() throws ServletException
	{
		pub = Boolean.parseBoolean(
			getServletConfig().getInitParameter("public")
		);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		ServletContext context = getServletContext();

		request.getRequestDispatcher(
			"/WEB-INF/tmpl/" + (pub ? "pub" : "priv") + ".search.jsp"
		).forward(request, response);
	}
}
