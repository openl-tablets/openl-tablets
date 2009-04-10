/*
 * Created on Jun 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.science;

/**
 * @author snshor
 *
 */
public interface IDerivedConstants extends IBasicConstants
{
	public static final IMultiplicativeExpression mph = mi.divide(h);
	
	public static final IMultiplicativeExpression m2 = m.multiply(m);

	public static final IMultiplicativeExpression m3 = m.multiply(m.multiply(m));

	public static final IMultiplicativeExpression l = cm.multiply(cm.multiply(cm)).changeScalar(1000);


	public static final IMeasurementSystem METRIC = MeasurementSystem.METRIC; 
	
}

