/**
 * 
 */
package com.exigen.le.smodel.accessor;


/**
 * 
 * Provide access to it's values 
 * @author zsulkins
 *
 */
public interface ValueHolder extends ModelHolder{
	public Object getValue(String name);
	public Object getValue(String name, int index);
	public int size(String name);
	public void set(String name, Object value);
	public void set(String name, int index, Object value);

}
