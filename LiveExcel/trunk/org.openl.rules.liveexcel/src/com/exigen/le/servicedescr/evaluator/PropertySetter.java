package com.exigen.le.servicedescr.evaluator;

/**
 * This interface should be implemented by user defined class
 * that is responsible for retrieving property
 *  @author vabramovs
 *
 */
public interface PropertySetter {
	/**
	 * @param source  Object which property should be set 
	 * @param propertyName  name of property to set
	 * @param index If index >= 0 element of (property) collection should be set, else - value of property 
	 * @return
	 */
	void setProperty(Object source, String propertyName, int index, Object value) throws Exception;
}
