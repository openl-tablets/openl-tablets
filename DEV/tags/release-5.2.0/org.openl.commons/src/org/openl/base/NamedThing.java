/**
 * Created Jan 19, 2007
 */
package org.openl.base;

/**
 * @author snshor
 *
 */
public class NamedThing implements INamedThing
{

	String name;
	
	public NamedThing(String name)
	{
		this.name = name;
	}

	public NamedThing()
	{
	}
	
	public void setName(String name)
	{
		this.name = name;
	}


	public String getName()
	{
		return name;
	}

	public String getDisplayName(int mode)
	{
		return name;
	}

}
