/*
 * Created on May 6, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util.meta;


/**
 * @author snshor
 */
public interface ITypeMetaInfo
{
	
	String getName();
	
	String getNamespace();
	
	/**
	 * If type is an aggregate type, returns it's meta-info, else null
	 * @return
	 */
	ICollectionMetaInfo getCollectionMetaInfo();
}
