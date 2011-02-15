package com.exigen.le.servicedescr.evaluator;

/**
 * This interface should be implemented by user defined class
 * that is responsible for retrieving property @author zsulkins
 *
 */
public interface PropertyRetriever {
	/**
	 * @param source  Object from which property should be retrieved 
	 * @param propertyName  name of property to retrieve
	 * @param index If index >= 0 element of (property) collection should be retrieved, else - value of property 
	 * @return
	 */
	Object retrieveProperty(Object source, String propertyName, int index) throws Exception;
}
