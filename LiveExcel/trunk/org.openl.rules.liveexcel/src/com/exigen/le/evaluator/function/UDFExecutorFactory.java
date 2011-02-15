/**
 * 
 */
package com.exigen.le.evaluator.function;

import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;


/**
 * @author vabramovs
 *
 */
public interface UDFExecutorFactory {
	FreeRefFunction createUDFExecutor(String functionName,String className); 

}
