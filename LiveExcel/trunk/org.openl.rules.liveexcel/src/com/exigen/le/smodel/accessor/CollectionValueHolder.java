/**
 * 
 */
package com.exigen.le.smodel.accessor;

/**
 * Provides access to wrapped collection 
 * @author zsulkins
 *
 */
public interface CollectionValueHolder extends ModelHolder{
	Object getValue(int index);
	int size();
}
