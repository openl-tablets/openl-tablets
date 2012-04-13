/**
 * 
 */
package com.exigen.le.evaluator.function;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;

import com.exigen.le.LE_Function;
import com.exigen.le.LE_FunctionFactory;
import com.exigen.le.evaluator.function.addins.IfError;
import com.exigen.le.evaluator.function.addins.NormDist;
import com.exigen.le.evaluator.function.addins.NormsDist;
import com.exigen.le.evaluator.function.addins.OfFirst;

/**
 * UDF Register
 * @author vabramovs
 *
 */
public class UDFRegister {
	private static final Log LOG = LogFactory.getLog(UDFRegister.class);
	static private Map<String,FreeRefFunction> javaUDF = new HashMap<String,FreeRefFunction>();
	static private UDFRegister INSTANCE = new UDFRegister();
	
	static private LE_FunctionFactory functionFactory; 
	static private UDFExecutorFactory udfFactory;
	
	private UDFRegister(){
	    init(new Properties());
	}

	public static UDFRegister getInstance(){
		return INSTANCE;
	}

	/**
	 * @return the javaUDF
	 */
	public  Map<String, FreeRefFunction> getJavaUDF() {
		return javaUDF;
	}
	
	/**
	 * Init component
	 * @param prop
	 */
	public void init(Properties prop) {
		registerOurBultin();
		FunctionUDFFactoryImpl factory = new FunctionUDFFactoryImpl();
		functionFactory = factory;
		udfFactory = factory;
		 for(String propName:prop.stringPropertyNames()){
			 // LE_function interface by specified factory
			 if(propName.startsWith("functionFactory.className")){
				 String className = prop.getProperty(propName);
				 try {
					Class<?> clazz= this.getClass().getClassLoader().loadClass(className);
					 LE_FunctionFactory instance =(LE_FunctionFactory)clazz.newInstance();
					 String propNameList = new String(propName).replace("className","functionList");
					 String functionList = prop.getProperty(propNameList);
					 if(functionList != null){
						 String[] functions = functionList.split(",");
						 for(String function:functions){
							 function = function.trim();
							 LE_Function executor = instance.createFunctionExecutor(function,prop.getProperty("UDF.function."+function));
							registerJavaFunction(function, executor);
						 }
					 }
				} catch (ClassNotFoundException e) {
					String msg = "Factory class "+className +" was not found";
					LOG.warn(msg);
				} catch (InstantiationException e) {
					String msg = "Factory class "+className +" could not instantiated";
					LOG.warn(msg,e);
				} catch (IllegalAccessException e) {
					String msg = "Factory class "+className +" could not accessed";
					LOG.warn(msg,e);
				}
			 }
			
			 // UDF_function interface by specified factory
			 else  if(propName.startsWith("UDFFactory.className")){
				 String className = prop.getProperty(propName);
				 try {
					Class<?> clazz= this.getClass().getClassLoader().loadClass(className);
					 UDFExecutorFactory instance =(UDFExecutorFactory)clazz.newInstance();
					 String propNameList = new String(propName).replace("className","functionList");
					 String functionList = prop.getProperty(propNameList);
					 if(functionList != null){
						 String[] functions = functionList.split(",");
						 for(String function:functions){
							 function = function.trim();
							 FreeRefFunction executor = instance.createUDFExecutor(function,prop.getProperty("UDF.function."+function)); 
							 registerJavaUDF(function, executor);
						 }
					 }
				} catch (ClassNotFoundException e) {
					String msg = "Factory class "+className +" was not found";
					LOG.warn(msg);
				} catch (InstantiationException e) {
					String msg = "Factory class "+className +" could not instantiated";
					LOG.warn(msg,e);
				} catch (IllegalAccessException e) {
					String msg = "Factory class "+className +" could not accessed";
					LOG.warn(msg,e);
				}
			 }
		 }
		 // Not specified factory (try default)
		 for(String propName:prop.stringPropertyNames()){
			 if(propName.startsWith("UDF.function")){
				 String functionName = propName.substring(propName.lastIndexOf(".")+1).toUpperCase();
				 functionName = functionName.trim();
				 if(!javaUDF.containsKey(functionName)){
					 FreeRefFunction executor = udfFactory.createUDFExecutor(functionName,prop.getProperty(propName));
					 if(executor != null)
						 registerJavaUDF(functionName, executor);
				 }
			 }
			 else if (propName.startsWith("LE_Function.function")){
				 String functionName = propName.substring(propName.lastIndexOf(".")+1).toUpperCase();
				 functionName = functionName.trim();
				 if(!javaUDF.containsKey(functionName)){
					 LE_Function executor = functionFactory.createFunctionExecutor(functionName,prop.getProperty(propName)); 
					 if(executor != null)
						 registerJavaFunction(functionName, executor);
				 }
			 }
		 }
}

	private void registerOurBultin() {
		registerJavaUDF("IFERROR",new IfError());
		registerJavaUDF("NORMDIST",new NormDist());
		registerJavaUDF("NORMSDIST",new NormsDist());
		registerJavaUDF("OFFIRST",new OfFirst());
	}

	/**
	 * Register new java Class to calculate UDF
	 * New registered UDF will effect to new lifted workbooks, not to cached 
	 * @param functionName
	 * @param executor
	 */
public void registerJavaFunction(String functionName, LE_Function executor){
	javaUDF.put(functionName.toUpperCase(),new LE_FunctionWrapper(executor,functionName));
		
	}
/**
 * Register new java Class to calculate UDF
 * New registered UDF will effect to new lifted workbooks, not to cached 
 * @param functionName
 * @param executor
 */
public void registerJavaUDF(String functionName, FreeRefFunction executor){
	javaUDF.put(functionName.toUpperCase(),executor);
}

}
