/**
 * 
 */
package com.exigen.le.evaluator.selector;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.project.ProjectManager;
import com.exigen.le.project.cache.SimpleCache;
import com.exigen.le.repository.Repository;

/**
 * @author vabramovs
 *
 */
public class SelectorFactory {
	private static final Log LOG = LogFactory.getLog(SelectorFactory.class);
	static private SelectorFactory INSTANCE = new SelectorFactory();
	static FunctionSelector functionSelector ;
	private SelectorFactory(){};
	/**
	 * @return
	 */
	public static SelectorFactory getInstance(){
		return INSTANCE; 
	}
	/**
	 * Init component
	 * @param prop
	 */
	public void init(Properties prop){
		String factoryClass = prop.getProperty("functionSelector.className");
		if(factoryClass == null){
			LOG.warn("Property 'functionSelector.className' not found in configuration properties. Will use default dummy selector");
			functionSelector = new DummyFunctionSelector();
		}
		else{
			try {
				functionSelector = (FunctionSelector)Class.forName(factoryClass).newInstance();
			} catch (InstantiationException e) {
				LOG.error("Class "+factoryClass+" did not instantiated.",e);
			} catch (IllegalAccessException e) {
				LOG.error("Class "+factoryClass+" did not instantiated.",e);
			} catch (ClassNotFoundException e) {
				LOG.error("Class "+factoryClass+" not found.");
			}
		}

	}
	public FunctionSelector getFunctionSelector(){
		return functionSelector;
	}
}
