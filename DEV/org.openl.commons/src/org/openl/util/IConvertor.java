/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.util;

/**
 * @author snshor
 *
 */
public interface IConvertor
{
	public Object convert(Object obj);
	
	
	static final public IConvertor SAME_AS = new SameAs();
	
	static class SameAs implements IConvertor
	{
		public Object convert(Object obj)
		{
			return obj;
		}
	}
	
}
