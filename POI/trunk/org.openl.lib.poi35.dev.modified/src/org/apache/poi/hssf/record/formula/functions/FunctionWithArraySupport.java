/**
 * Later this interface should be merged with Function interface
 */
package org.apache.poi.hssf.record.formula.functions;

/**
 * @author Zahars Sulkins(ZS)(Zahars.Sulkins at exigenservices.com)
 *
 */
public interface FunctionWithArraySupport  {

	/*
	 * true if parameter accept array, false otherwise
	 */
	public boolean supportArray(int paramIndex); 
}
