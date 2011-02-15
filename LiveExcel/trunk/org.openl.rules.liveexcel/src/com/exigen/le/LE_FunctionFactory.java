/**
 * 
 */
package com.exigen.le;

import java.util.Properties;

/**
 * @author vabramovs
 *
 */
public interface LE_FunctionFactory {
	LE_Function createFunctionExecutor(String functionName,String className); 
}
