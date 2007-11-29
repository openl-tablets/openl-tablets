/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import java.util.Iterator;

import org.openl.binding.AmbiguousMethodException;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public class NullOpenClass implements IOpenClass
{

    private NullOpenClass()
    {
    }

    public static final NullOpenClass the = new NullOpenClass();

    public IOpenSchema getSchema()
    {
	return null;
    }

    public IOpenFactory getFactory()
    {
	return null;
    }

    public Iterator<IOpenClass> superClasses()
    {
	return null;
    }

    public Iterator<IOpenMethod> methods()
    {
	return null;
    }

    public Iterator<IOpenField> fields()
    {
	return null;
    }

    public boolean isAbstract()
    {
	return false;
    }

    public boolean isInstance(Object instance)
    {
	return instance == null;
    }

    public Class<?> getInstanceClass()
    {
	return null;
    }

    public IOpenMethod getMethod(String name, IOpenClass[] classes)
    {
	return null;
    }

    public IOpenField getField(String fname, boolean strictMatch)
    {
	return null;
    }

    public String getName()
    {
	return "null-Class";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenClass#isAssignableFrom(org.openl.types.IOpenClass)
     */
    public boolean isAssignableFrom(IOpenClass ioc)
    {
	return ioc == this;
    }

    public Object nullObject()
    {
	return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IMethodFactory#getMatchingMethod(java.lang.String,
     *      org.openl.types.IOpenClass[])
     */
    public IOpenMethod getMatchingMethod(String name, IOpenClass[] params)
	    throws AmbiguousMethodException
    {
	return null;
    }

    public IOpenField getVar(String fname, boolean strictMatch)
    {
	return null;
    }

    public boolean isArray()
    {
	return false;
    }

    public IOpenClass getArrayType(@SuppressWarnings("unused")
    int dim)
    {
	return null;
    }

    public IAggregateInfo getAggregateInfo()
    {
	return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenClassHolder#getOpenClass()
     */
    public IOpenClass getOpenClass()
    {
	return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenClass#newInstance()
     */
    public Object newInstance(IRuntimeEnv env)
    {
	return null;
    }

    public IOpenField getIndexField()
    {
	return null;
    }

    public IMetaInfo getMetaInfo()
    {
	return null;
    }

    public void setMetaInfo(IMetaInfo info)
    {
    }

    public String getDisplayName(int mode)
    {
	return getName();
    }

    public boolean isAssignableFrom(IType type)
    {
	return false;
    }

    public String getNameSpace()
    {
	return ISyntaxConstants.THIS_NAMESPACE;
    }

    @SuppressWarnings("unchecked")
    public IDomain getDomain()
    {
	return null;
    }

    public boolean isAssignableFrom(Class<?> c)
    {
	return true;
    }

    public boolean isSimple()
    {
	return true;
    }

    public IOpenField getField(String name)
    {
	return null;
    }

}
