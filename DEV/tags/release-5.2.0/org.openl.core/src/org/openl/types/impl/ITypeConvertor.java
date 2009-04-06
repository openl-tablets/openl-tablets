/*
 * Created on Jul 1, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.impl;

/**
 * @author snshor
 *
 */
public interface ITypeConvertor
{
	Object srcToDest(Object src);
	
	Object destToSrc(Object dest); 
	

}
