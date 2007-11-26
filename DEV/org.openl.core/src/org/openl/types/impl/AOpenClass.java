/*
 * Created on Jun 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.binding.AmbiguousMethodException;
import org.openl.binding.MethodNotFoundException;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.OpenIterator;

/**
 * @author snshor
 * 
 */
public abstract class AOpenClass implements IOpenClass
{
    public boolean isSimple()
    {
	return false;
    }

    public IDomain getDomain()
    {
	return null;
    }

    protected IOpenSchema schema;

    IOpenField indexField;
    protected IMetaInfo metaInfo;

    public IMetaInfo getMetaInfo()
    {
	return metaInfo;
    }

    public void setMetaInfo(IMetaInfo metaInfo)
    {
	this.metaInfo = metaInfo;
    }

    protected AOpenClass(IOpenSchema schema)
    {
	this.schema = schema;
    }

    /**
     * @return
     */
    public IOpenSchema getSchema()
    {
	return schema;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenClass#isAbstract()
     */
    public boolean isAbstract()
    {
	return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenClass#nullObject()
     */
    public Object nullObject()
    {
	return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IVarFactory#getVar(java.lang.String)
     */
    public IOpenField getVar(String name)
    {
	return getField(name);
    }

    public Iterator<IOpenField> fields()
    {
	Map<String, IOpenField> fieldMap = fieldMap();
	return fieldMap == null ? null : fieldMap.values().iterator();
    }

    protected abstract Map<String, IOpenField> fieldMap();

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IMethodFactory#methods()
     */
    public Iterator<IOpenMethod> methods()
    {
	Map<MethodKey, IOpenMethod> methodMap = methodMap();
	return methodMap == null ? null : methodMap.values().iterator();
    }

    protected abstract Map<MethodKey, IOpenMethod> methodMap();

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IMethodFactory#getMatchingMethod(java.lang.String,
     *      org.openl.types.IOpenClass[])
     */
    public IOpenMethod getMatchingMethod(String name, IOpenClass[] params)
	    throws AmbiguousMethodException
    {
	return getMethod(name, params);
    }

    @SuppressWarnings({ "cast", "unchecked" })
    public IOpenMethod getMethod(String name, IOpenClass[] classes)
    {
	Map<MethodKey, IOpenMethod> m = methodMap();

	if (classes == null)
	{
	    ISelector<IOpenMethod> nameSel = (ISelector<IOpenMethod>)new ASelector.StringValueSelector(
		    name, IOpenMethod.NAME_CONVERTOR);

	    List<IOpenMethod> list = OpenIterator.select(methods(), nameSel)
		    .asList();
	    if (list.size() > 1)
	    {
		throw new AmbiguousMethodException(name, IOpenClass.EMPTY, list);
	    } else if (list.size() == 1)
	    {
		return list.get(0);
	    } else
		return null;

	}

	return m == null ? null : (IOpenMethod) m.get(new MethodKey(name,
		classes));
    }

    public IOpenField getField(String name)
    {
	Map<String, IOpenField> m = fieldMap();
	return m == null ? null : (IOpenField) m.get(name);
    }

    static public final class MethodKey
    {
	String name;
	IOpenClass[] pars;

	public MethodKey(IOpenMethod om)
	{
	    name = om.getName();
	    pars = om.getSignature().getParameterTypes();
	}

	public MethodKey(String name, IOpenClass[] pars)
	{
	    this.name = name;
	    this.pars = pars;
	}

	public boolean equals(Object obj)
	{
	    if (!(obj instanceof MethodKey))
		return false;
	    MethodKey mk = (MethodKey) obj;

	    return new EqualsBuilder().append(name, mk.name).append(pars,
		    mk.pars).isEquals();
	}

	public int hashCode()
	{
	    return new HashCodeBuilder().append(name).append(pars).toHashCode();
	}

    }// eof MethodKey

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.IOpenClass#getArrayType(int)
     */
    public IOpenClass getArrayType(int dim)
    {
	return JavaOpenClass.getOpenClass(getInstanceClass()).getArrayType(dim);
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

    /**
     * @return
     */
    public IOpenField getIndexField()
    {
	return indexField;
    }

    /**
     * @param field
     */
    public void setIndexField(IOpenField field)
    {
	indexField = field;
    }

    public String toString()
    {
	return getName();
    }

    static public IOpenMethod[] getMethods(String name,
	    Iterator<IOpenMethod> methods)
    {
	ArrayList<IOpenMethod> list = new ArrayList<IOpenMethod>();
	for (; methods.hasNext();)
	{
	    IOpenMethod m = methods.next();
	    if (m.getName().equals(name))
		list.add(m);
	}

	return list.toArray(new IOpenMethod[0]);
    }

    static public IOpenMethod getSingleMethod(String name,
	    Iterator<IOpenMethod> methods)
    {
	ArrayList<IOpenMethod> list = new ArrayList<IOpenMethod>();
	for (; methods.hasNext();)
	{
	    IOpenMethod m = methods.next();
	    if (m.getName().equals(name))
		list.add(m);
	}

	if (list.size() == 0)
	    throw new MethodNotFoundException(null, name, IOpenClass.EMPTY);

	if (list.size() > 1)
	    throw new AmbiguousMethodException(name, IOpenClass.EMPTY, list);

	return list.get(0);
    }

    public String getNameSpace()
    {
	return ISyntaxConstants.THIS_NAMESPACE;
    }

    public boolean isAssignableFrom(IType type)
    {
	if (type instanceof IOpenClass)
	    return isAssignableFrom((IOpenClass) type);
	return false;
    }

}
