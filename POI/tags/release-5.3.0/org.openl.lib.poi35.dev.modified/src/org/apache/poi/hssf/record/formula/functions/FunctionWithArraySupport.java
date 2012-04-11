/**
 * Later this interface should be merged with Function interface
 */
package org.apache.poi.hssf.record.formula.functions;

/**
 * @author zsulkins
 *
 */
public interface FunctionWithArraySupport extends Function {

	/*
	 * true if parameter accept array, false otherwise
	 */
	public boolean supportArray(int paramIndex); 
}
