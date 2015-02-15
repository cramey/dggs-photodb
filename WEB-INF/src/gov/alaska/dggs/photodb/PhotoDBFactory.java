package gov.alaska.dggs.photodb;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextEvent;

import java.io.FileInputStream;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSession;


public class PhotoDBFactory implements ServletContextListener
{
	private static SqlSessionFactory factory = null;


	public static SqlSession openSession()
	{
		return factory.openSession(false);
	}


	public void contextInitialized(ServletContextEvent event)
	{
		ServletContext context = event.getServletContext();

		SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();

		FileInputStream fin = null;
		try {
			fin = new FileInputStream(context.getRealPath("/WEB-INF/mybatis.xml"));
			factory = builder.build(fin);
			context.setAttribute("factory", factory);
		} catch(Exception ex){
			context.log("Cannot create factory: " + ex.getMessage());
		} finally {
			try { fin.close(); }
			catch(Exception ex){ }
		}
	}


	public void contextDestroyed(ServletContextEvent event){ }
}
