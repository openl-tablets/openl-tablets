/*
 * Created on May 21, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.java;

import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public abstract class JavaCast implements IOpenCast
{

	Class from; 
	Class to; 
	int distance;
	boolean implicit;
	
	public JavaCast(Class from, Class to, int distance, boolean implicit)
	{
		this.from = from;
		this.to = to;
		this.distance = distance;
		this.implicit = implicit;
	}

  /* (non-Javadoc)
   * @see org.openl.types.IOpenCast#getFrom()
   */
  public IOpenClass getFrom()
  {
    return JavaOpenClass.getOpenClass(from);
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenCast#getTo()
   */
  public IOpenClass getTo()
  {
		return JavaOpenClass.getOpenClass(to);
  }

  /* (non-Javadoc)
   * @see org.openl.types.IOpenCast#isImplicit()
   */
  public boolean isImplicit()
  {
    return false;
  }

}
