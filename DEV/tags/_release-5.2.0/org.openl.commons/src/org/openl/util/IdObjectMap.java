/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.util;

import java.util.HashMap;

/**
 * @author snshor
 * 
 */
public class IdObjectMap<T>
{
    protected HashMap<Integer, T> idObjMap = null;
    protected HashMap<T, Integer> objIdMap = null;

    protected int id = 0;

    final int newID()
    {
	return ++id;
    }

    public IdObjectMap()
    {
	reset();
    }

    public void reset()
    {
	idObjMap = new HashMap<Integer, T>();
	objIdMap = new HashMap<T, Integer>();
    }

    public synchronized int addObject(T o)
    {
	Integer v = objIdMap.get(o);

	if (v == null)
	{
	    v = newID();
	    objIdMap.put(o, v);
	    idObjMap.put(v, o);
	}

	return v.intValue();
    }

    public synchronized int getID(T o)
    {
	return addObject(o);
    }

    /**
     * Always produces newID, use with caution
     * 
     * @param o
     * @return
     */
    public synchronized int getNewID(T o)
    {
	Integer v = new Integer(newID());
	objIdMap.put(o, v);
	idObjMap.put(v, o);

	return v.intValue();
    }

    public T getObject(int idx)
    {

	return idObjMap.get(idx);
    }

    public synchronized int removeObject(T o)
    {
	Integer v = objIdMap.get(o);

	if (v != null)
	{
	    objIdMap.remove(o);
	    idObjMap.remove(v);
	    return v;
	}

	return -1;
    }

    public synchronized int remove(int idx)
    {

	Object obj = idObjMap.get(idx);

	if (obj != null)
	{
	    objIdMap.remove(obj);
	    idObjMap.remove(idx);
	    return idx;
	}

	return -1;
    }

}
