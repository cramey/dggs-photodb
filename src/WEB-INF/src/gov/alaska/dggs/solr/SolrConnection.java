package gov.alaska.dggs.solr;

import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.MalformedURLException;


public class SolrConnection
{
	private String url;
	private Integer connect_timeout, read_timeout;
	private String content_type;


	public SolrConnection(String url)
	{
		this.url = url;
		connect_timeout = 1000;
		read_timeout = 500;
		content_type = null;
	}


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


	public String getContentType(){ return content_type; }
	public void setContentType(String content_type)
	{
		this.content_type = content_type;
	}


	public String execute(String input) throws Exception
	{
		HttpURLConnection conn = null;
		try {
			URL url = new URL(this.url);
			conn = (HttpURLConnection)url.openConnection();
			conn.setReadTimeout(read_timeout);
			conn.setConnectTimeout(connect_timeout);
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);

			if(content_type != null){
				conn.setRequestProperty("Content-Type", content_type);
			}

			try (OutputStream os = conn.getOutputStream()){
				os.write(input.getBytes("UTF-8"));
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

			return result.toString();
		} finally {
			if(conn != null) conn.disconnect();
		}
	}
}
