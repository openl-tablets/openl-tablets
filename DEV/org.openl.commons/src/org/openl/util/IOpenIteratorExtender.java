/*
 * Created on Jul 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author snshor
 *
 */
public interface IOpenIteratorExtender
{

	/**
	 * 
	 */
	public Iterator extend(Object obj);

	static final class CollectionExtender implements IOpenIteratorExtender
	{
		/**
			 *
			 */

		public Iterator extend(Object obj)
		{
			return ((Collection) obj).iterator();
		}

	}

	public static final CollectionExtender COLLECTION_EXTENDER = new CollectionExtender();

}
