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
public interface ISetDomain<T>
{

    ISetDomain<T> and(ISetDomain<T> sd);

    ISetDomain<T> or(ISetDomain<T> sd);

    ISetDomain<T> sub(ISetDomain<T> sd);

    boolean contains(T obj);

    Iterator<T> iterator();

    int size();

    static final public ISetDomain<Object> EMPTY_DOMAIN = new EmptyDomain<Object>();


    static class EmptyDomain<T> implements ISetDomain<T>
    {

	public ISetDomain<T> and(ISetDomain<T> sd)
	{
	    return this;
	}

	/**
	 * 
	 */

	public boolean contains(T obj)
	{
	    return false;
	}

	/**
	 * 
	 */

	public Iterator<T> iterator()
	{
	    return OpenIterator.empty();
	}

	/**
	 * 
	 */

	public ISetDomain<T> or(ISetDomain<T> sd)
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

	public ISetDomain<T> sub(ISetDomain<T> sd)
	{
	    return this;
	}

    }

}
