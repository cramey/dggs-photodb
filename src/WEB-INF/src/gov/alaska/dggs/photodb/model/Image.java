package gov.alaska.dggs.photodb.model;

import java.io.Serializable;
import java.util.List;
import java.util.Date;


public class Image implements Serializable
{
	private static final long serialVersionUID = 1L;


	private Integer id;
	public Integer getID(){ return id; }
	public void setID(Integer id){ this.id = id; }


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


	private String summary;
	public String getSummary(){ return summary; }
	public void setSummary(String summary)
	{
		this.summary = summary;
	}


	private String description;
	public String getDescription(){ return description; }
	public void setDescription(String description)
	{
		this.description = description;
	}


	private String metadata;
	public String getMetadata(){ return metadata; }


	private String geojson;
	public String getGeoJSON(){ return geojson; }
	public void setGeoJSON(String geojson)
	{
		this.geojson = geojson;
	}


	private Integer accuracy;
	public Integer getAccuracy(){ return accuracy; }
	public void setAccuracy(Integer accuracy)
	{
		this.accuracy = accuracy;
	}


	private Date taken;
	public Date getTaken(){ return taken; }
	public void setTaken(Date taken)
	{
		this.taken = taken;
	}


	private Date modified;
	public Date getModified(){ return modified; }
	public void setModified(Date modified)
	{
		this.modified = modified;
	}


	private Date entered;
	public Date getEntered(){ return entered; }
	public void setEntered(Date entered)
	{
		this.entered = entered;
	}


	private Boolean ispublic;
	public Boolean getIsPublic(){ return ispublic; }
	public void setIsPublic(Boolean ispublic)
	{
		this.ispublic = ispublic;
	}


	private List<Tag> tags;
	public List<Tag> getTags(){ return tags; }
	public void setTags(List<Tag> tags){ this.tags = tags; }
}
