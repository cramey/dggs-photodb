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
import java.io.InputStream;
import java.io.OutputStream;

import java.util.zip.GZIPOutputStream;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;

import mjson.Json;

import java.text.SimpleDateFormat;
import java.text.DateFormat;

import gov.alaska.dggs.SolrConnection;


public class ImageSearchServlet extends HttpServlet
{
  private boolean hideprivate;


	public void init() throws ServletException
	{
		hideprivate = Boolean.parseBoolean(
			getServletConfig().getInitParameter("hideprivate")
		);
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

		Json json = null;
		try {
			SolrConnection conn = new SolrConnection(
				context.getInitParameter("solr_url") + "/select"
			);
			// Set default limit to 6
			conn.setLimit(6);

			conn.setFields(
				"id, description, credit, title, " +
				"taken, filename, " +
				"geojson:[geo f=geog w=GeoJSON]"
			);

			String search = request.getParameter("search");
			if(search != null && search.length() > 0){
				conn.setQuery(search);
			}

			String emptydesc = request.getParameter("description");
			if(emptydesc != null && emptydesc.length() > 0){
				if(Boolean.valueOf(emptydesc)){
					conn.setFilter("-description", "[\"\" TO *]");
				} else {
					conn.setFilter("description", "[\"\" TO *]");
				}
			}

			String emptylocation = request.getParameter("location");
			if(emptylocation != null && emptylocation.length() > 0){
				if(Boolean.valueOf(emptylocation)){
					conn.setFilter("-geog", "[\"\" TO *]");
				} else {
					conn.setFilter("geog", "[\"\" TO *]");
				}
			}

			String aoi = request.getParameter("aoi");
			if(aoi != null && aoi.length() > 0){
				conn.setFilter(
					"{!field f=geog format=GeoJSON}",
					"Intersects(" + aoi + ")"
				);
			}

			String sort = request.getParameter("sort");
			if(sort != null && sort.length() > 0){
				conn.setSort(sort);
			}

			if(hideprivate) conn.setFilter("ispublic", "true");

			conn.setLimit(request.getParameter("show"));
			conn.setPage(request.getParameter("page"));

			json = conn.execute();

			DateFormat din = new SimpleDateFormat("yyyy-MM-dd'T'");
			DateFormat dout = DateFormat.getDateInstance(DateFormat.SHORT);

			if(json.at("docs") != null){
				for(Json doc : json.at("docs").asJsonList()){
					// Fix the broken-ass dates so they're less broken ass
					try { 
						if(doc.at("taken") != null){
							String st = doc.at("taken").asString();
							Date dt = din.parse(st);
							doc.set("taken", dout.format(dt));
						}
					} catch(Exception ex){
						// Ignore errors from trying to fix the date taken
					}
				}
			}
		} catch(Exception ex){
			json = Json.object(
				"error", Json.object(
					"msg", ex.getMessage(), "code", 400
				)
			);
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
			out.write(json.toString());
		} finally {
			if(out != null){ out.close(); }
			if(gos != null){ gos.close(); }
		}
	}
}
