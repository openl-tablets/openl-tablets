/*
 * Created on Apr 29, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.util.Iterator;

import org.openl.util.OpenIterator;

/**
 * @author snshor
 */
public interface ISetDomain
{

  ISetDomain and(ISetDomain sd);
  ISetDomain or(ISetDomain sd);

	
	ISetDomain sub(ISetDomain sd);
	
	boolean contains(Object obj);
	
	Iterator iterator();
	int size();


	static final public ISetDomain EMPTY_DOMAIN = new EmptyDomain();



	static class EmptyDomain implements ISetDomain
	{
		
		public ISetDomain and(ISetDomain sd)
		{
			return this;
		}

		/**
		 *
		 */

		public boolean contains(Object obj)
		{
			return false;
		}

		/**
		 *
		 */

		public Iterator iterator()
		{
			return OpenIterator.EMPTY;
		}


		/**
		 *
		 */

		public ISetDomain or(ISetDomain sd)
		{
			return sd;
		}

		/**
		 *
		 */

		public int size()
		{
			return 0;
		}

		/**
		 *
		 */

		public ISetDomain sub(ISetDomain sd)
		{
			return this;
		}

}
	
}
