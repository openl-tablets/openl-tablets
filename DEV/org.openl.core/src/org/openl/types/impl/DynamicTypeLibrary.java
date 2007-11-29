/*
 * Created on Aug 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.binding.AmbiguousTypeException;
import org.openl.types.IOpenClass;
import org.openl.types.ITypeLibrary;

/**
 * @author snshor
 * 
 */
public class DynamicTypeLibrary implements ITypeLibrary
{

    Map<String, IOpenClass> types = new HashMap<String, IOpenClass>();

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.ITypeLibrary#getType(java.lang.String)
     */
    public IOpenClass getType(String typename) throws AmbiguousTypeException
    {
	return types.get(typename);
    }

    public void addType(String name, IOpenClass type)
    {
	types.put(name, type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.ITypeLibrary#typeNames()
     */
    public Iterator<String> typeNames()
    {
	return types.keySet().iterator();
    }

}
