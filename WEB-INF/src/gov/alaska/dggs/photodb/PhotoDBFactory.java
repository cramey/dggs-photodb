package gov.alaska.dggs.photodb;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextEvent;

import java.io.FileInputStream;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSession;

import gov.alaska.dggs.PostgreSQL;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;


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
			Connection conn = sess.getConnection();
			if(!PostgreSQL.isExtension(conn, "postgis")){
				context.log("PostGIS extension not available");
				return;
			}

			if(!PostgreSQL.isExtension(conn, "plpgsql")){
				context.log("PL/pgSQL extension not available");
				return;
			}

			try (Statement st = conn.createStatement(
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY
			)){
				st.execute(
					"CREATE TABLE IF NOT EXISTS tag (" +
						"tag_id SERIAL PRIMARY KEY," +
						"name VARCHAR(50)" + 
					")"
				);

				st.execute(
					"CREATE TABLE IF NOT EXISTS image (" +
						"image_id SERIAL PRIMARY KEY," +
						"image BYTEA NOT NULL," + 
						"image_md5 VARCHAR(32) UNIQUE NOT NULL," +
						"thumbnail BYTEA NULL," + 
						"filename TEXT NOT NULL," + 
						"taken DATE NULL," +
						"entered TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()," +
						"modified TIMESTAMP WITHOUT TIME ZONE NOT NULL," +
						"credit TEXT NULL," + 
						"summary VARCHAR(100) NULL," + 
						"description TEXT NULL," +
						"metadata JSONB NULL," + 
						"search TSVECTOR NOT NULL," + 
						"geog GEOGRAPHY(Point) NULL," + 
						"geog_accuracy SMALLINT NOT NULL DEFAULT 1," + 
						"ispublic BOOLEAN NOT NULL DEFAULT false" +
					") WITH (FILLFACTOR=70)"
				);

				st.execute(
					"CREATE TABLE IF NOT EXISTS image_tag (" + 
						"tag_id INT REFERENCES tag(tag_id) NOT NULL," + 
						"image_id INT REFERENCES image(image_id) NOT NULL," + 
						"PRIMARY KEY(tag_id, image_id)" + 
					")"
				);

				if(!PostgreSQL.isFunction(conn, "image_md5_fn")){
					st.execute(
						"CREATE OR REPLACE FUNCTION image_md5_fn() " + 
						"RETURNS TRIGGER AS $$ " + 
						"BEGIN " +
							"IF NEW.image IS NOT NULL AND NEW.image_md5 IS NULL THEN " +
								"NEW.image_md5 = md5(NEW.image); " + 
							"END IF; " +
							"RETURN NEW; " +
						"END; $$ LANGUAGE 'plpgsql'"
					);
				}

				if(!PostgreSQL.isTrigger(conn, "image_md5_tr")){
					st.execute(
						"CREATE TRIGGER image_md5_tr " +
						"BEFORE INSERT OR UPDATE ON image " +
						"FOR EACH ROW EXECUTE PROCEDURE image_md5_fn()"
					);
				}

				if(!PostgreSQL.isFunction(conn, "image_modified_fn")){
					st.execute(
						"CREATE FUNCTION image_modified_fn() " +
						"RETURNS TRIGGER AS $$ " + 
						"BEGIN " + 
							"NEW.modified = NOW(); " + 
							"NEW.search = " + 
								"TO_TSVECTOR('simple', COALESCE(NEW.credit, '')) || " +
								"TO_TSVECTOR('simple', COALESCE(NEW.filename, '')) || " + 
								"TO_TSVECTOR('simple', COALESCE(NEW.summary, '')) || " + 
								"TO_TSVECTOR('simple', COALESCE(NEW.description, '')) || ( " + 
									"SELECT TO_TSVECTOR(" + 
										"'simple', COALESCE(STRING_AGG(t.name, ' '), '') " + 
									") " + 
									"FROM image_tag AS it " +
									"JOIN tag AS t ON t.tag_id = it.tag_id " + 
									"WHERE it.image_id = NEW.image_id " + 
								"); " +
							"RETURN NEW; " +
						"END; $$ LANGUAGE 'plpgsql'"
					);
				}

				if(!PostgreSQL.isTrigger(conn, "image_modified_tr")){
					st.execute(
						"CREATE TRIGGER image_modified_tr " +
						"BEFORE INSERT OR UPDATE ON image " +
						"FOR EACH ROW EXECUTE PROCEDURE image_modified_fn()"
					);
				}

				if(!PostgreSQL.isIndex(conn, "image_image_md5_idx")){
					st.execute(
						"CREATE INDEX image_image_md5_idx ON image(image_md5)"
					);
				}

				if(!PostgreSQL.isIndex(conn, "image_taken_idx")){
					st.execute(
						"CREATE INDEX image_taken_idx ON image(taken)"
					);
				}

				if(!PostgreSQL.isIndex(conn, "image_geog_idx")){
					st.execute(
						"CREATE INDEX image_geog_idx ON image USING GIST(geog)"
					);
				}

				if(!PostgreSQL.isIndex(conn, "image_search_idx")){
					st.execute(
						"CREATE INDEX image_search_idx ON image USING GIN(search)"
					);
				}

				if(!PostgreSQL.isFunction(conn, "images_common")){
					// Finds and returns the commonality between various images 
					// That is, if two images share the same credit, this will
					// return that credit otherwise it returns null for the credit.
					st.execute(
						"CREATE FUNCTION public.images_common(" +
							"ids INT[]" +
						") RETURNS TABLE(" +
							"credit TEXT[], " +
							"taken DATE[], " +
							"summary VARCHAR[], " +
							"description TEXT[], " +
							"geojson TEXT[], " +
							"accuracy SMALLINT[], " +
							"tags TEXT[], " +
							"ispublic  BOOLEAN[]" +
						") AS $$ " +
							"DECLARE " +
								"arr_credit TEXT[]; " +
								"arr_taken DATE[]; " +
								"arr_summary VARCHAR[]; " +
								"arr_description TEXT[]; " +
								"arr_geojson VARCHAR[]; " +
								"arr_tags VARCHAR[]; " +
							"BEGIN " +
								"SELECT ARRAY_AGG(q.credit) INTO arr_credit " +
								"FROM (" +
									"SELECT DISTINCT i.credit " +
									"FROM image AS i " +
									"WHERE i.image_id = ANY(ids) " +
									"LIMIT 2" +
								") AS q; " +

								"SELECT ARRAY_AGG(q.taken) INTO arr_taken " +
								"FROM (" +
									"SELECT DISTINCT i.taken " +
									"FROM image AS i " +
									"WHERE i.image_id = ANY(ids) " +
									"LIMIT 2" +
								") AS q; " +

								"SELECT ARRAY_AGG(q.summary) INTO arr_summary " +
								"FROM (" +
									"SELECT DISTINCT i.summary " +
									"FROM image AS i " +
									"WHERE i.image_id = ANY(ids) " +
									"LIMIT 2" +
								") AS q; " +

								"SELECT ARRAY_AGG(q.description) INTO arr_description " +
								"FROM (" +
									"SELECT DISTINCT i.description " +
									"FROM image AS i " +
									"WHERE i.image_id = ANY(ids) " +
									"LIMIT 2" +
								") AS q; " +

								"SELECT ARRAY_AGG(q.geojson) INTO arr_geojson " +
								"FROM (" +
									"SELECT DISTINCT " +
										"ST_AsGeoJSON(i.geog::GEOMETRY) AS geojson " +
									"FROM image AS i " +
									"WHERE i.image_id = ANY(ids) " +
									"LIMIT 2" +
								") AS q; " +

								"SELECT ARRAY_AGG(q.names) INTO arr_tags " +
								"FROM (" +
									"SELECT DISTINCT names " +
									"FROM (" +
										"SELECT i.image_id, " +
											"STRING_AGG(t.name, ', ') AS names " +
										"FROM image AS i " +
										"LEFT OUTER JOIN image_tag AS it " +
											"ON it.image_id = i.image_id " +
										"LEFT OUTER JOIN tag AS t " +
											"ON t.tag_id = it.tag_id " +
										"WHERE i.image_id = ANY(ids) " +
										"GROUP BY i.image_id" +
									") AS q " +
									"LIMIT 2" +
								") AS q; " +

								"RETURN QUERY SELECT " +
									"CASE WHEN ARRAY_LENGTH(arr_credit, 1) = 1 " +
										"THEN ARRAY[arr_credit[1]] " +
										"ELSE ARRAY[]::TEXT[] " +
									"END AS credit, " +

									"CASE WHEN ARRAY_LENGTH(arr_taken, 1) = 1 " +
										"THEN ARRAY[arr_taken[1]] " +
										"ELSE ARRAY[]::DATE[] " +
									"END AS taken, " +

									"CASE WHEN ARRAY_LENGTH(arr_summary, 1) = 1 " +
										"THEN ARRAY[arr_summary[1]] " +
										"ELSE ARRAY[]::VARCHAR[] " +
									"END AS summary, " +

									"CASE WHEN ARRAY_LENGTH(arr_description, 1) = 1 " +
										"THEN ARRAY[arr_description[1]] " +
										"ELSE ARRAY[]::TEXT[] " +
									"END AS description, " +

									"CASE WHEN ARRAY_LENGTH(arr_geojson, 1) = 1 " +
										"THEN ARRAY[arr_geojson[1]] " +
										"ELSE ARRAY[]::TEXT[] " +
									"END AS geojson, " +

									"CASE WHEN ARRAY_LENGTH(arr_tags, 1) = 1 " +
										"THEN ARRAY[arr_tags[1]] " +
										"ELSE ARRAY[]::TEXT[] " +
									"END AS tags " +
								"; " +
							"END; " + 
						"$$ LANGUAGE plpgsql;"
					);
				}
			}
		} catch(Exception ex){
			context.log("Cannot validate database: " + ex.getMessage());
		}
	}


	public void contextDestroyed(ServletContextEvent event){ }
}
