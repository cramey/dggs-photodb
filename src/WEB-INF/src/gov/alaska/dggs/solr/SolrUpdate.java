package gov.alaska.dggs.solr;

import mjson.Json;


public class SolrUpdate
{
	private SolrConnection conn;


	public SolrUpdate(String url)
	{
		this(new SolrConnection(url + "/update?commit=true"));
	}
	public SolrUpdate(SolrConnection conn)
	{
		this.conn = conn;
		conn.setContentType("application/json");
	}


	public void delete(Integer id)
	{
		try { 
			String params = Json.object("delete",
				Json.object("id", id)
			).toString();
			conn.execute(params);
		} catch(Exception ex){
			// Ignore errors
		}
	}


	public void add(Json obj)
	{
		try {
			String params = Json.object(
				"add", Json.object("doc", obj)
			).toString();
			conn.execute(params);
		} catch(Exception ex){
			// Ignore errors
		}
	}
}
