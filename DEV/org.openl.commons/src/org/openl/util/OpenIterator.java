/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.util;

import java.util.Enumeration;
import java.util.Iterator;



/**
 * @author snshor
 *
 */
public class OpenIterator extends AOpenIterator.IteratorWrapper
{

	public static IOpenIterator fromEnumeration(Enumeration enumeration)
	{
		if (enumeration == null)
			return EMPTY;
		
		return new EnumerationIterator(enumeration);  
	}


	
	public static IOpenIterator fromArray(Object[] ary)
	{
		if (ary == null || ary.length == 0)
			return EMPTY;
		
		return new AIndexedIterator.ArrayIterator(ary);  
	}
	
	public static IOpenIterator fromArray(Object ary)
	{
		if (ary == null)
			return EMPTY;
		
		return new AIndexedIterator.AnyArrayIterator(ary);  
	}
	



	public OpenIterator(Iterator it)
	{
		super(it);
	}


	static class EnumerationIterator extends AOpenIterator
	{
		Enumeration enumeration;
		EnumerationIterator(Enumeration enumeration)
		{
			this.enumeration = enumeration;
		}
		
	    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext()
    {
      return enumeration.hasMoreElements();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next()
    {
      return enumeration.nextElement();
    }

}


}
