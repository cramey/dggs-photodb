package gov.alaska.dggs;

import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import mjson.Json;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.MalformedURLException;


public class SolrConnection
{
	private Map<String,List<String>> filters;
	private URL url;
	private String query, fields, sort;
	private Integer limit, page;
	private Integer connect_timeout, read_timeout;


	public SolrConnection(String url) throws MalformedURLException
	{
		this(new URL(url));
	}

	public SolrConnection(URL url)
	{
		this.url = url;
		filters = new HashMap<String,List<String>>();

		// Set some sane defaults
		page = 0;
		limit = 10;
		connect_timeout = 1000;
		read_timeout = 500;
		query = "*:*";
		fields = null;
		sort = null;
	}


	public String getQuery(){ return query; }
	public void setQuery(String query){ this.query = query; }

	public String getSort(){ return sort; }
	public void setSort(String sort){ this.sort = sort; }

	public String getFields(){ return fields; }
	public void setFields(String fields){ this.fields = fields; }


	public Integer getConnectTimeout(){ return connect_timeout; }
	public void setConnectTimeout(Integer connect_timeout)
	{
		this.connect_timeout = connect_timeout;
	}


	public Integer getReadTimeout(){ return read_timeout; }
	public void setReadTimeout(Integer read_timeout)
	{
		this.read_timeout = read_timeout;
	}


	public Integer getLimit(){ return limit; }
	public void setLimit(Integer limit)
	{
		this.limit = limit;
	}
	public void setLimit(String limit)
	{
		try { setLimit(Integer.valueOf(limit)); }
		catch(Exception ex){ }
	}


	public Integer getPage(){ return page; }
	public void setPage(Integer page)
	{
		this.page = page;
	}
	public void setPage(String page)
	{
		try { setPage(Integer.valueOf(page)); }
		catch(Exception ex){ }
	}


	public void setFilter(String key, String value)
	{
		List<String> values = filters.get(key);
		if(values == null){
			values = new LinkedList<String>();
			filters.put(key, values);
		}
		values.clear();
		values.add(value);
	}
	public String getFilter(String key){
		List<String> values = filters.get(key);
		if(values == null || values.size() == 0) return null;
		return values.get(0);
	}
	public List<String> getFilters(String key){
		List<String> values = filters.get(key);
		if(values == null || values.size() == 0) return null;
		return values;
	}
	public void addFilter(String key, String value)
	{
		List<String> values = filters.get(key);
		if(values == null){
			values = new LinkedList<String>();
			filters.put(key, values);
		}
		values.add(value);
	}
	public void removeFilter(String key, String value)
	{
		List<String> values = filters.get(key);
		if(values == null) return;
		values.remove(value);
	}
	public void clearFilter(String key)
	{
		filters.remove(key);
	}


	public Json execute()
	{
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)url.openConnection();
			conn.setReadTimeout(read_timeout);
			conn.setConnectTimeout(connect_timeout);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);

			StringBuilder params = new StringBuilder();

			if(query != null){
				params.append("q=");
				params.append(URLEncoder.encode(query, "UTF-8"));
			}

			for(Map.Entry<String, List<String>> e : filters.entrySet()){
				String key = e.getKey();
				switch(e.getValue().size()){
					case 0: continue;
					case 1:
						if(params.length() > 0) params.append("&");
						params.append("fq=");
						params.append(URLEncoder.encode(key, "UTF-8"));
						if(key.charAt(key.length() - 1) != '}'){
							params.append(":");
						}
						params.append(
							URLEncoder.encode(
								e.getValue().get(0), "UTF-8"
							)
						);
					break;
					default:
						if(params.length() > 0) params.append("&");
						params.append("fq=");
						params.append(URLEncoder.encode(key, "UTF-8"));
						params.append(":(");
						int count = 0;
						for(String value : e.getValue()){
							if(count++ > 0) params.append(" OR ");
							params.append(URLEncoder.encode(value, "UTF-8"));
						}
						params.append(")");
				}
			}
			
			// add fields (if they exist)
			if(fields != null){
				if(params.length() > 0) params.append("&");
				params.append("fl=");
				params.append(URLEncoder.encode(fields, "UTF-8"));
			}

			// add sort (if it exists)
			if(sort != null){
				if(params.length() > 0) params.append("&");
				params.append("sort=");
				params.append(URLEncoder.encode(sort, "UTF-8"));
			}

			// Add limit and page
			if(params.length() > 0) params.append("&");
			params.append("start=");
			params.append((limit * page));
			params.append("&rows=");
			params.append(limit);

			// Return json format
			params.append("&wt=json");

			try (OutputStream os = conn.getOutputStream()){
				os.write(params.toString().getBytes("UTF-8"));
				os.flush();
			}

			StringBuilder result = new StringBuilder();
			try (InputStream is = (
				conn.getResponseCode() == HttpURLConnection.HTTP_OK ?
				conn.getInputStream() : conn.getErrorStream()
			)){
				byte[] buf = new byte[4096];
				for(int c; (c = is.read(buf, 0, buf.length)) != -1; result.append(new String(buf, 0, c)));
			}

			Json json = Json.read(result.toString());

			if(json.at("response") != null){
				json = json.at("response");
			} else if(json.at("error") != null){
				json = json.at("error");
				json.atDel("metadata");
				json = Json.object("error", json);
			}

			return json;
		} catch(Exception ex){
			return Json.object("error", Json.object(
				"msg", ex.getMessage(),
				"code", 400
			));
		} finally {
			if(conn != null) conn.disconnect();
		}
	}
}
