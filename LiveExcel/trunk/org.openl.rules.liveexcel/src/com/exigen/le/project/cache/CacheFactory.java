/**
 * 
 */
package com.exigen.le.project.cache;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.project.Project;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.ProjectManager;
import com.exigen.le.project.ProjectVersion;

/**
 * @author vabramovs
 *
 */
public class CacheFactory {
	private static final Log LOG = LogFactory.getLog(ProjectManager.class);

	static Class cacheClass = new SimpleCache<Object,Object>().getClass();
	

	/**
	 * Init component
	 * @param prop
	 */
	static public void init(Properties prop){
		String cacheClassName = prop.getProperty("projectCache.className");
		if(cacheClassName == null){
			LOG.warn("Property 'projectCache.className' not found in configuration properties. Will use default Simple Cache");
		}
		else{
			try {
				cacheClass = Class.forName(cacheClassName);
			} catch (ClassNotFoundException e) {
				LOG.error("Class "+cacheClass+" not found.");
			}
		}

	}
	@SuppressWarnings("unchecked")
	static public Cache<String,Project> createProjectCache(){
		try {
			return (Cache<String,Project>)cacheClass.newInstance();
		} catch (InstantiationException e) {
			LOG.error("Class "+cacheClass+" did not instantiated.",e);
		} catch (IllegalAccessException e) {
			LOG.error("Class "+cacheClass+" did not instantiated.",e);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	static public Cache<String,ProjectVersion> createVersionCache(){
		try {
			return (Cache<String,ProjectVersion>)cacheClass.newInstance();
		} catch (InstantiationException e) {
			LOG.error("Class "+cacheClass+" did not instantiated.",e);
		} catch (IllegalAccessException e) {
			LOG.error("Class "+cacheClass+" did not instantiated.",e);
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	static public Cache<String,ProjectElement> createElementCache(){
		try {
			return (Cache<String,ProjectElement>)cacheClass.newInstance();
		} catch (InstantiationException e) {
			LOG.error("Class "+cacheClass+" did not instantiated.",e);
		} catch (IllegalAccessException e) {
			LOG.error("Class "+cacheClass+" did not instantiated.",e);
		}
		return null;
	}
}
