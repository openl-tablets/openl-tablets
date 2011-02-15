/**
 * 
 */
package com.exigen.le.servicedescr.evaluator;


import java.lang.reflect.Array;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.accessor.CollectionValueHolder;
import com.exigen.le.smodel.accessor.ValueHolder;

/**
 * ValueHolder based on Nap
 * @author vabramovs
 *
 */
public class MapWrapper extends java.util.HashMap<String, Object> implements ValueHolder {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory.getLog(MapWrapper.class);
	private Type type; 

	
	public MapWrapper(Type type){
		this.type = type;
	}
	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.accessor.ValueHolder#getValue(java.lang.String)
	 */
	public Object getValue(String name) {
		return super.get(name);
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.accessor.ValueHolder#getValue(java.lang.String, int)
	 */
	public Object getValue(String name, int index) {
		Object container = getValue(name);
		if(index == (-1))
		{
			return container;	
		}
		if(container instanceof CollectionValueHolder ){
			return ((CollectionValueHolder)container).getValue(index);
		}
		if(container.getClass().isArray()){
			return Array.get(container, index);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.accessor.ValueHolder#set(java.lang.String, java.lang.Object)
	 */
	public void set(String name, Object value) {
		super.put(name, value);

	}

	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.accessor.ValueHolder#set(java.lang.String, int, java.lang.Object)
	 */
	public void set(String name, int index, Object value) {
		if(index==(-1)){
			set(name, value);
			return ;
		}
		Object container = get(name);
		if(container == null){ // We have no container yet
			container = new CollectionMapWrapper(ServiceModel.getTypeByPath(type, name));
			set(name,container);
		}
		if(container instanceof CollectionValueHolder ){
//			Object itemHolder = ((CollectionValueHolder)container).getValue(index);
//			if(itemHolder instanceof ValueHolder){
//				((ValueHolder)itemHolder).set(name, value);
//			}
//			else {
//				String msg = "Object  "+itemHolder+ " does not support set operation";
//				LOG.error(msg);
//				throw new RuntimeException(msg);
//			}
			((CollectionMapWrapper)container).set(name, index, value);
			return;
		}
		else {
			String msg = "Object  "+container+ " does not support set operation with index "+index;
			LOG.error(msg);
			throw new RuntimeException(msg);
			
		}
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.accessor.ValueHolder#size(java.lang.String)
	 */
	public int size(String name) {
		Object obj = get(name);
		if(obj == null){
			return 0;
		}
		if(obj instanceof CollectionValueHolder){
			return ((CollectionValueHolder)obj).size();
		}
		if(obj.getClass().isArray()){
			return Array.getLength(obj);
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.smodel.accessor.ModelHolder#getModel()
	 */
	public Type getModel() {
		// This 
		return type;
	}

}
