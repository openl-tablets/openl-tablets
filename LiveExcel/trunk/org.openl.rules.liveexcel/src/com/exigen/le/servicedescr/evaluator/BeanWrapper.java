/**
 * 
 */
package com.exigen.le.servicedescr.evaluator;

import java.lang.reflect.Array;
import java.util.List;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;

import com.exigen.le.smodel.Property;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.MappedProperty;
import com.exigen.le.smodel.accessor.ValueHolder;


/**
 * Evaluates value based on model description
 * 
 * @author zsulkins
 * 
 */

public class BeanWrapper implements com.exigen.le.smodel.accessor.ValueHolder {
	private static final Logger logger = Logger.getLogger(BeanWrapper.class);
	protected Object currentHolder;
	protected Type currentHolderDescr; // describes current holder
	private static PropertyRetriever defaultRetriever = new BeanRetriever();
	private static PropertySetter defaultSetter = new BeanSetter();

	public BeanWrapper(Object valueHolder, 
				Type currentHolderDescr) {
		this.currentHolder = valueHolder;
		this.currentHolderDescr = currentHolderDescr;
	}

	public Object getValue(String name){
		return getValue(name, -1);
	}
	
	/*
	 * If index < 0, value is not indexed
	 * This method could return either BeanWrapper, CollectionBeanWrapper or Java type/array (Boolean, Double, String, Calendar)
	 */
	public Object getValue(String name, int index) {
		logger.debug("retrieving property: " + name + " index" + index + "  from " + currentHolderDescr.getName());
		
		Property property = null;
		for (Property p : currentHolderDescr.getChilds()) {
			if (p.getName().equalsIgnoreCase(name)) {
				property = p;
				break;
			}
		}
		if (property == null) {
			String err = "property: " + name + "  not found in: "
					+ currentHolderDescr.getName();
			logger.error(err);
			throw new RuntimeException(err);
		}
		Type ref = property.getType();
		
		
		Object result = null;
		try {
			PropertyRetriever instance;
				if(property instanceof MappedProperty){
			
				String retriever = ((MappedProperty) property).getPropertyRetriever();
				if (retriever!=null){
					// property retriever (class) is defined or empty (return the same)
					if (retriever.isEmpty()){
						return new BeanWrapper(currentHolder, ref); // this could be needed if we want to make structure deeper that it exists in original object
					}	
					Class<?> clazz = Class.forName(retriever);
					instance = (PropertyRetriever)clazz.newInstance();
				} else {
					instance = defaultRetriever;
				}	
				if(((MappedProperty) property).getMappingProperty()!= null){
					result = instance.retrieveProperty(currentHolder, ((MappedProperty) property).getMappingProperty(), index);
				}
				else {
					result = instance.retrieveProperty(currentHolder,  property.getName(), index);
				}	
			}
				else {
					instance = defaultRetriever;
					result = instance.retrieveProperty(currentHolder,  property.getName(), index);
					
				}	
			} catch (Exception e) {
			String err = "failed to retrieve property: " + name + " in: "
					+ currentHolderDescr.getName();
			logger.error(err, e);
			throw new RuntimeException(err, e);
		}
		
		if (property.isCollection() && index < 0 && ref.isComplex() ){
			// collection of complex type requested  
			if( result instanceof CollectionBeanWrapper)
			{
				return result;
			}
			else {
				
				return new CollectionBeanWrapper(this, ref, name, result);
			}
		}
	
		
		
		if (ref.isComplex()) { 
			if(result instanceof ValueHolder){
				return result;
			}
			else{
				return new BeanWrapper(result, ref);
			}
		}
		// primitive
		Class<?> clazz = Type.Primary.getPrimary(ref).getClass();
		if (property.isCollection()){
			clazz = (Array.newInstance(clazz, 1)).getClass();
		}
		return ConvertUtils.convert(result, clazz);

	}
	
	public Type getModel(){
		return currentHolderDescr;
	}

	public Object getHolder(){
		return currentHolder;
	}
	@SuppressWarnings("unchecked")
	public int size(String name) {
		Object value =  getValue(name);
		if(value != null){ // Dyna object may have no value yet
			Class type = value.getClass();
			  if (type.isArray()) {
			       return  Array.getLength(value);
			    }
			  else if(value instanceof List<?>){
				  return ((List)value).size();
			  }
			  else if(value instanceof CollectionBeanWrapper ){
					return ((CollectionBeanWrapper)value).size(); 
			  }
		}
	return 0;
	}
	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.accessor.SetableValueHolder#set(java.lang.String, java.lang.Object)
	 */
	public void set(String name, Object value) {
		set(name,-1,value);
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.accessor.SetableValueHolder#set(java.lang.String, int, java.lang.Object)
	 */
	public void set(String name, int index, Object value) {
		logger.debug("Set property: " + name + " index" + index + "  from " + currentHolderDescr.getName());
		
		Property property = null;
		for (Property p : currentHolderDescr.getChilds()) {
			if (p.getName().equals(name)) {
				property = p;
				break;
			}
		}
		if (property == null) {
			String err = "property: " + name + "  not found in: "
					+ currentHolderDescr.getName();
			logger.error(err);
			throw new RuntimeException(err);
		}
		Type ref = property.getType();
		String propName = name;
		try {
			PropertySetter instance;
				if(property instanceof MappedProperty){
					String msg =	"Setting of mapped properties does not implemented yet";	
					logger.error(msg);
					instance = defaultSetter;
					throw new RuntimeException(msg);
//			  // TODO review code below to support mapping
//				String setter = ((MappedProperty) property).getPropertySetter();
//				if (setter!=null){
//					// property retriever (class) is defined or empty (return the same)
//					if (setter.isEmpty()){
//						instance = defaultSetter;
//					}	
//					Class<?> clazz = Class.forName(setter);
//					instance = (PropertySetter)clazz.newInstance();
//				} else {
//					instance = defaultSetter;
//				}	
//				propName = ((MappedProperty) property).getMappingProperty();
					
			}
				else {
					instance = defaultSetter;
				}	
				instance.setProperty(currentHolder, propName, index, value);
			} catch (Exception e) {
			String err = "failed to set property: " + name + " to: "
					+ currentHolderDescr.getName();
			logger.error(err, e);
			throw new RuntimeException(err, e);
		}
	}


}
