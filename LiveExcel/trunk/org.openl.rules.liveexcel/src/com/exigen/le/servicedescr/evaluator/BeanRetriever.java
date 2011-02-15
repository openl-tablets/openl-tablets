package com.exigen.le.servicedescr.evaluator;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.LiveExcel;



/**
 *  To retrieve property from Bean
 * @author zsulkins
 *
 */
public class BeanRetriever implements PropertyRetriever {

	private static final Log LOG = LogFactory.getLog(BeanRetriever.class);

	/* (non-Javadoc)
	 * @see com.exigen.le.servicedescr.evaluator.PropertyRetriever#retrieveProperty(java.lang.Object, java.lang.String, int)
	 */
	public Object retrieveProperty(Object source, String propertyName, int index)  throws IllegalAccessException, NoSuchMethodException, InvocationTargetException{
		Object result = null;
		// Bean Property Name conversation is different than our ServiceModel (all name in Upper Case)
		// so we  need to find matching property name
		for(PropertyDescriptor prop:PropertyUtils.getPropertyDescriptors(source.getClass())){
			if(prop.getName().equalsIgnoreCase(propertyName)){
				if (index<0) {
					result = PropertyUtils.getProperty(source,prop.getName());
				} else {
					result = PropertyUtils.getIndexedProperty(source, prop.getName(), index);
			}
				return result;
				
			}
		}
		String msg =" Bean "+source+" has no macthing property "+propertyName;
		LOG.error(msg);
		throw new RuntimeException(msg);
		
	}
}
