/**
 * 
 */
package com.exigen.le.servicedescr.evaluator;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * To set property in Bean
 * @author vabramovs
 *
 */
public class BeanSetter implements PropertySetter {

	/* (non-Javadoc)
	 * @see com.exigen.le.servicedescr.evaluator.PropertySetter#setProperty(java.lang.Object, java.lang.String, int, java.lang.Object)
	 */
	public void setProperty(Object source, String propertyName, int index,
			Object value) throws Exception {
		if (index<0) {
			PropertyUtils.setProperty(source,propertyName,value);
		} else {
			PropertyUtils.setIndexedProperty(source, propertyName, index,value);
	}

	}

}
