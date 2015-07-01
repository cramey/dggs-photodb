package gov.alaska.dggs.photodb.model;

import java.io.Serializable;


public class Tag implements Serializable
{
	private static final long serialVersionUID = 1L;

	private Integer id;
	public Integer getID(){ return id; }

	private String name;
	public String getName(){ return name; }
	public void setName(String name){ this.name = name; }
}
