/**
 * Created Apr 6, 2007
 */
package org.openl.base;

/**
 * @author snshor
 *
 */
public class NameSpacedThing extends NamedThing implements INameSpacedThing
{

	String nameSpace;

	public String getNameSpace()
	{
		return this.nameSpace;
	}

	public void setNameSpace(String nameSpace)
	{
		this.nameSpace = nameSpace;
	}
	
	public NameSpacedThing(String name, String namespace)
	{
		super(name);
		this.nameSpace = namespace;
	}

	public NameSpacedThing()
	{
	}

	
	
	
}
