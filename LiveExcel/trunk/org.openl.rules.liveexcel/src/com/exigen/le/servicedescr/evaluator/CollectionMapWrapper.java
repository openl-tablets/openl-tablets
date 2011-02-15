/**
 * 
 */
package com.exigen.le.servicedescr.evaluator;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.accessor.ValueHolder;

/**
 * Wrapper for collections
 * @author zsulkins
 *
 */
public class CollectionMapWrapper implements com.exigen.le.smodel.accessor.CollectionValueHolder, ValueHolder {

	private Object collection ; // we assume that collection  is either List<MapWrapper> or Array of primitives
	private static final Logger logger = Logger.getLogger(CollectionMapWrapper.class);
	private Type type; 

	
	CollectionMapWrapper(Type type){
		this.type = type;
	}
	
	public Object getValue(int index) {
		if (collection.getClass().isArray()){
			return Array.get(collection, index);
		}
		else{
			return ((List<MapWrapper>)collection).get(index);
			
		}
	}

	public int size() {
		// we assume that collection  is either Collection or Array
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
		return null;
	}

	public void set(String name, int index, Object value) {
		if(collection == null){
			collection = new ArrayList<MapWrapper>();
		}
		if (collection.getClass().isArray()){  // Primitive 
			Array.set(collection, index, value);
		}
		else{
			ValueHolder obj;
			if(value instanceof ValueHolder)
			{
				obj = (ValueHolder)value;
			}
			else{
				obj = new MapWrapper(type);
				obj.set(name,value);
			}
			try {
					((List<ValueHolder>)collection).set(index, obj);
			} catch (Exception e) {
				((List<ValueHolder>)collection).add(obj);
			}
		}
	}

	public Object getValue(String name) {
		return this;
	}

	public Object getValue(String name, int index) {
		return getValue(index);
	}

	public void set(String name, Object value) {
		set(name,size(),value);
		
	}

	public int size(String name) {
		return size();
	}
}
