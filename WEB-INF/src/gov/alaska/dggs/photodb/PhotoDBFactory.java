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

	private static boolean isTable(SqlSession sess, String name)
	{
		Boolean b = sess.selectOne("gov.alaska.dggs.Util.isTable", name);
		if(Boolean.TRUE.equals(b)) return true;
		return false;
	}

	private static boolean isFunction(SqlSession sess, String name)
	{
		Boolean b = sess.selectOne("gov.alaska.dggs.Util.isFunction", name);
		if(Boolean.TRUE.equals(b)) return true;
		return false;
	}

	private static boolean isTrigger(SqlSession sess, String name)
	{
		Boolean b = sess.selectOne("gov.alaska.dggs.Util.isTrigger", name);
		if(Boolean.TRUE.equals(b)) return true;
		return false;
	}

	private static boolean isIndex(SqlSession sess, String name)
	{
		Boolean b = sess.selectOne("gov.alaska.dggs.Util.isIndex", name);
		if(Boolean.TRUE.equals(b)) return true;
		return false;
	}

	public void contextInitialized(ServletContextEvent event)
	{
		ServletContext context = event.getServletContext();
		SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();

		try (FileInputStream fin = new FileInputStream(
			context.getRealPath("/WEB-INF/mybatis.xml")
		)) {
			factory = builder.build(fin);
			context.setAttribute("factory", factory);
		} catch(Exception ex){
			context.log("Cannot create factory: " + ex.getMessage());
		}

		if(factory == null) return;

		try (SqlSession sess = PhotoDBFactory.openSession()) {
			if(!isTable(sess, "tag")){
				sess.update("gov.alaska.dggs.photodb.Schema.createTableTag");
			}

			if(!isTable(sess, "image")){
				sess.update("gov.alaska.dggs.photodb.Schema.createTableImage");
			}

			if(!isTable(sess, "image_tag")){
				sess.update("gov.alaska.dggs.photodb.Schema.createTableImageTag");
			}

			if(!isFunction(sess, "image_md5_fn")){
				sess.update("gov.alaska.dggs.photodb.Schema.createFunctionImageMD5");
			}

			if(!isTrigger(sess, "image_md5_tr")){
				sess.update("gov.alaska.dggs.photodb.Schema.createTriggerImageMD5");
			}

			if(!isFunction(sess, "image_modified_fn")){
				sess.update("gov.alaska.dggs.photodb.Schema.createFunctionImageMod");
			}

			if(!isTrigger(sess, "image_modified_tr")){
				sess.update("gov.alaska.dggs.photodb.Schema.createTriggerImageMod");
			}

			if(!isIndex(sess, "image_image_md5_idx")){
				sess.update("gov.alaska.dggs.photodb.Schema.createIndexImageMD5");
			}

			if(!isIndex(sess, "image_taken_idx")){
				sess.update("gov.alaska.dggs.photodb.Schema.createIndexImageTaken");
			}

			if(!isIndex(sess, "image_geog_idx")){
				sess.update("gov.alaska.dggs.photodb.Schema.createIndexImageGeog");
			}

			if(!isFunction(sess, "images_common")){
				sess.update("gov.alaska.dggs.photodb.Schema.createFunctionImagesCommon");
			}

			sess.commit();
		} catch(Exception ex){
			context.log("Cannot validate database: " + ex.getMessage());
			System.out.println(ex.getMessage());
		}
	}


	public void contextDestroyed(ServletContextEvent event){ }
}
