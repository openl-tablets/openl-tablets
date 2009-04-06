/*
 * Created on Jun 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.science;

import java.util.Iterator;

import org.openl.base.INamedThing;
import org.openl.util.OpenIterator;

/**
 * @author snshor
 *
 */
public abstract class ASimpleUnit extends AMultiplicativeExpression implements IUnit, IDimensionPower, INamedThing
{
	IDimension dimension;
	String name;
	
	/**
	 * Converts value measured in this unit into value "normal" in this dimension. Based on convention certainly, 
	 * so in metric system normal values are meter, second and kilogramm. Based on this unit of length  
	 * cm will have to have normalized function 0.01 * value. 
	 * 
	 * @param value
	 * @return
	 */

	protected ASimpleUnit(String name, IDimension dimension)
	{
		this.name = name;
		this.dimension = dimension;
	}
	 
	public abstract double normalize(double value);
	
	
  public IDimensionPower getDimensionPower(IDimension id)
  {
    return id == dimension ? this : null;
  }

  public Iterator getDimensionsPowers()
  {
    return OpenIterator.single(this);
  }

  public  double getScalar()
  {
    return normalize(1);
  }
  
  public IDimension getDimension()
  {
    return dimension;
  }

  public int getPower()
  {
    return 1;
  }

  /* (non-Javadoc)
   * @see org.openl.types.science2.IMultiplicativeExpression#getDimensionCount()
   */
  public int getDimensionCount()
  {
    return 1;
  }

  /**
   * @return
   */
  public String getName()
  {
    return name;
  }

	public String getDisplayName(int mode)
	{
		return name;
	}

}
