/**
 * 
 */
package com.exigen.le.servicedescr.evaluator;

import java.lang.reflect.Array;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.exigen.le.smodel.Type;

/**
 * Wrapper for collections
 * @author zsulkins
 *
 */
public class CollectionBeanWrapper implements com.exigen.le.smodel.accessor.CollectionValueHolder {

	private BeanWrapper parentBeanWrapper;
	private String propertyName;
	private static final Logger logger = Logger.getLogger(CollectionBeanWrapper.class);
	private Type currentHolderDescr; // describes current holder
	private Object collection;
	
	CollectionBeanWrapper(BeanWrapper parentBeanWrapper, Type currentHolderDescr, String propertyName, Object collection){
		this.parentBeanWrapper = parentBeanWrapper;
		this.currentHolderDescr = currentHolderDescr;
		this.propertyName = propertyName;
		this.collection = collection;
	}
	
	
	
	public Object getValue(int index) {
		return parentBeanWrapper.getValue(propertyName, index);
	}

	public int size() {
		// we assume that collection  is either Collection or Array
		logger.debug("defines size of collection" + propertyName + " in " + parentBeanWrapper.getModel().getName());
		if(collection == null) { // Dyna object with no defined collection yet
			return 0;
		}
		if (collection.getClass().isArray()){
			return Array.getLength(collection);
		}
		// Collection
		int size = 0;
		size = ((Collection<?>)collection).size();
		return size;
	}



	public Type getModel() {
		return currentHolderDescr;
	}

}
