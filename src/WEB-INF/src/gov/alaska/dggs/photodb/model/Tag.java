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

	@Override
	public boolean equals(Object o)
	{
		if(o == null || !(o instanceof Tag)) return false;

		Tag t = (Tag)o;

		if(name == null){
			return name == t.getName();
		} else {
			return name.equalsIgnoreCase(t.getName());
		}
	}
}
