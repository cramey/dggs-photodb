package gov.alaska.dggs.photodb.model;

import java.io.Serializable;
import java.util.List;
import java.util.Date;


public class Image implements Serializable
{
	private static final long serialVersionUID = 1L;


	private int id;
	public int getID(){ return id; }


	private String filename;
	public String getFilename(){ return filename; }
	public void setFilename(String filename)
	{
		this.filename = filename;
	}


	private String credit;
	public String getCredit(){ return credit; }
	public void setCredit(String credit)
	{
		this.credit = credit;
	}


	private String description;
	public String getDescription(){ return description; }
	public void setDescription(String description)
	{
		this.description = description;
	}


	private String metadata;
	public String getMetadata(){ return metadata; }


	private String wkt;
	public String getWKT(){ return wkt; }


	private Date date;
	public Date getDate(){ return date; }


	private List<Tag> tags;
	public List<Tag> getTags(){ return tags; }
}
