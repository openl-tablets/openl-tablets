/*
 * Created on Mar 9, 2004
 *
 * Developed by OpenRules Inc. 2003-2004
 */
 
package org.openl.types.impl;

import java.lang.reflect.Array;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;


public class ArrayIndex implements IOpenIndex
{
	IOpenClass elementType;
	public ArrayIndex(IOpenClass elementType)
	{
		this.elementType = elementType;
	}

	/* (non-Javadoc)
	* @see org.openl.types.IOpenIndex#getElementType()
	*/
	public IOpenClass getElementType()
	{
		return elementType;
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenIndex#getValue(java.lang.Object, java.lang.Object)
	 */
	public Object getValue(Object container, Object index)
	{
		return Array.get(container, ((Integer) index).intValue());
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenIndex#isWritable()
	 */
	public boolean isWritable()
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenIndex#setValue(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	public void setValue(Object container, Object index, Object value)
	{
		Array.set(container, ((Integer) index).intValue(), value);
	}

	/* (non-Javadoc)
	 * @see org.openl.types.IOpenIndex#getIndexType()
	 */
	public IOpenClass getIndexType()
	{
		return JavaOpenClass.INT;
	}

}