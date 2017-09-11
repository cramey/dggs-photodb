package gov.alaska.dggs.photodb;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.ibatis.session.SqlSession;

import gov.alaska.dggs.photodb.PhotoDBFactory;


public class ImageDownloadServlet extends HttpServlet
{
	private boolean thumbnail;


	public void init() throws ServletException
	{
		thumbnail = Boolean.parseBoolean(
			getServletConfig().getInitParameter("thumbnail")
		);
	}


	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPostGet(request,response); }
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { doPostGet(request,response); }

	@SuppressWarnings("unchecked")
	public void doPostGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		ServletContext context = getServletContext();

		int id = 0;
		try { id = Integer.parseInt(request.getPathInfo().substring(1)); }
		catch(Exception ex){ }

		SqlSession sess = PhotoDBFactory.openSession();
		Connection conn = sess.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = thumbnail ? 
				"SELECT filename, taken, " +
					"LENGTH(COALESCE(thumbnail, image)) AS image_size, " +
					"COALESCE(thumbnail, image) AS image " +
				"FROM image WHERE image_id = ?" :

				"SELECT filename, taken, image, " +
					"LENGTH(image) AS image_size " +
				"FROM image WHERE image_id = ?";

			ps = conn.prepareStatement(sql,
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY,
				ResultSet.CLOSE_CURSORS_AT_COMMIT
			);
			ps.setInt(1, id);

			rs = ps.executeQuery();

			if(rs.next()){
				String filename = rs.getString("filename");
				String mime = context.getMimeType(filename.toLowerCase());
				if(mime == null) mime = "application/octet-stream";

				Date taken = rs.getDate("taken");
				if(taken != null){
					response.addDateHeader("Last-Modified", taken.getTime());
				}

				response.setContentLength(rs.getInt("image_size"));
				response.setContentType(mime);
				response.setHeader(
					"Content-Disposition", "inline; filename=" + filename
				);

				byte buf[] = new byte[4096];
				InputStream is = rs.getBinaryStream("image");
				ServletOutputStream out = response.getOutputStream();
				for(int i = 0; (i = is.read(buf)) > 0; out.write(buf, 0, i));
				is.close();
				out.flush();
				out.close();
			} else {
				response.sendError(response.SC_NOT_FOUND, "File not found.");
			}
		} catch(Exception ex){
			throw new ServletException(ex);
		} finally {
			try { rs.close(); }
			catch(Exception exe){ }

			try { ps.close(); }
			catch(Exception exe){ }

			try { conn.close(); }
			catch(Exception exe){ }

			sess.close();	
		}
	}
}
