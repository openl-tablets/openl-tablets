/*
 * Created on Jun 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.science;

/**
 * @author snshor
 */

public class Dimension implements IDimension
{
	String name;
	
	private Dimension(String name)
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
  
  
  static public final Dimension DISTANCE = new Dimension("distance");
	static public final Dimension TIME = new Dimension("time");
	static public final Dimension MASS = new Dimension("mass");

}
