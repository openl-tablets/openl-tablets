/**
 * 
 */
package com.exigen.le.evaluator.function;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;

import com.exigen.le.LE_Function;
import com.exigen.le.LE_FunctionFactory;
import com.exigen.le.LiveExcel;

/**
 * @author vabramovs
 *
 */
public class FunctionUDFFactoryImpl implements LE_FunctionFactory, UDFExecutorFactory {
	private static final Log LOG = LogFactory.getLog(LiveExcel.class);
	
	public FunctionUDFFactoryImpl(){
		
	}
	
	/* (non-Javadoc)
	 * @see com.exigen.le.LE_FunctionFactory#createFunctionExecutor(java.lang.String, java.lang.String)
	 */
	public LE_Function createFunctionExecutor(String functionName,
			String className) {
		 try {
			 Class<?> clazz= this.getClass().getClassLoader().loadClass(className);
			 LE_Function instance =(LE_Function)clazz.newInstance();
			 return instance;
			} catch (ClassNotFoundException e) {
				String msg = "Class "+className+" not found";
				LOG.warn(msg);
			} catch (InstantiationException e) {
				String msg = "Class "+className+" could not be instanced";
				LOG.warn(msg);
			} catch (IllegalAccessException e) {
				String msg = "Class "+className+" could not be accessed";
				LOG.warn(msg);
			} catch (ClassCastException e) {
				String msg = "Class "+className+" does not support FreeRefFunction";
				LOG.warn(msg);
			}
		return null;
	}
	/* (non-Javadoc)
	 * @see com.exigen.le.evaluator.function.UDFExecutorFactory#createUDFExecutor(java.lang.String, java.lang.String)
	 */
	public FreeRefFunction createUDFExecutor(String functionName,
			String className) {
		 try {
			 Class<?> clazz= this.getClass().getClassLoader().loadClass(className);
			 FreeRefFunction instance =(FreeRefFunction)clazz.newInstance();
			 return instance;
			} catch (ClassNotFoundException e) {
				String msg = "Class "+className+" not found";
				LOG.warn(msg);
			} catch (InstantiationException e) {
				String msg = "Class "+className+" could not be instanced";
				LOG.warn(msg);
			} catch (IllegalAccessException e) {
				String msg = "Class "+className+" could not be accessed";
				LOG.warn(msg);
			} catch (ClassCastException e) {
				String msg = "Class "+className+" does not support FreeRefFunction";
				LOG.warn(msg);
			}
		return null;
	}

}
