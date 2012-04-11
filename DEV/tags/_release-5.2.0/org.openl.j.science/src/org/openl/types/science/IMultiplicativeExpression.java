/*
 * Created on Jun 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.science;

import java.util.Iterator;

/**
 * @author snshor
 *
 * Provides facility for expressing something like 15 m/s, or 3.5 APY 	
 */

public interface IMultiplicativeExpression
{
	/**
	 * Return scalar part of the expression
	 * @return
	 */
	public double getScalar();
	
	public IDimensionPower getDimensionPower(IDimension id);
	public Iterator getDimensionsPowers();
	
	/**
	 * Returns number of different dimensions it has
	 * @return
	 */
	public int getDimensionCount();
	
	public IMultiplicativeExpression changeScalar(double newScalar);
	
	public IMultiplicativeExpression divide(IMultiplicativeExpression im);
  public IMultiplicativeExpression multiply(IMultiplicativeExpression im);
	
  public IMultiplicativeExpression add(IMultiplicativeExpression im) throws RuntimeException;
  public IMultiplicativeExpression subtract(IMultiplicativeExpression im) throws RuntimeException;
	
	public IMultiplicativeExpression negate();
	
	public String printInSystem(IMeasurementSystem system, int doubleDigits);
	
	public String printAs(IMultiplicativeExpression asUnit, String image);

	public String printAs(IMultiplicativeExpression asUnit, String image, int doubleDidgits);

}
