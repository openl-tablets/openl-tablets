package org.openl.types.impl;

import java.util.Iterator;

import org.openl.binding.AmbiguousMethodException;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.vm.IRuntimeEnv;

public class OpenClassDelegator implements IOpenClass
{
    IOpenClass baseClass;
    String name;
    IMetaInfo metaInfo;
    String nameSpace;
    
    public IOpenClass getBaseClass()
    {
        return baseClass;
    }

    public void setBaseClass(IOpenClass baseClass)
    {
        this.baseClass = baseClass;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public IMetaInfo getMetaInfo()
    {
        return metaInfo;
    }

    public void setMetaInfo(IMetaInfo metaInfo)
    {
        this.metaInfo = metaInfo;
    }

    public OpenClassDelegator( String name, IOpenClass baseClass,
	    IMetaInfo metaInfo)
    {
	this.baseClass = baseClass;
	this.name = name;
	this.metaInfo = metaInfo;
    }

    public Iterator fields()
    {
	return baseClass.fields();
    }

    public IAggregateInfo getAggregateInfo()
    {
	return baseClass.getAggregateInfo();
    }

    public String getDisplayName(int mode)
    {
	return baseClass.getDisplayName(mode);
    }

    public IDomain getDomain()
    {
	return baseClass.getDomain();
    }

    public IOpenField getField(String name)
    {
	return baseClass.getField(name);
    }

    public IOpenField getIndexField()
    {
	return baseClass.getIndexField();
    }

    public Class getInstanceClass()
    {
	return baseClass.getInstanceClass();
    }

    public IOpenMethod getMatchingMethod(String name, IOpenClass[] params)
	    throws AmbiguousMethodException
    {
	return baseClass.getMatchingMethod(name, params);
    }

    public IOpenMethod getMethod(String name, IOpenClass[] classes)
    {
	return baseClass.getMethod(name, classes);
    }

    public IOpenSchema getSchema()
    {
	return baseClass.getSchema();
    }

    public IOpenField getVar(String name)
    {
	return baseClass.getVar(name);
    }

    public boolean isAbstract()
    {
	return baseClass.isAbstract();
    }

    public boolean isAssignableFrom(Class c)
    {
	return baseClass.isAssignableFrom(c);
    }

    public boolean isAssignableFrom(IOpenClass ioc)
    {
	return baseClass.isAssignableFrom(ioc);
    }

    public boolean isAssignableFrom(IType type)
    {
	return baseClass.isAssignableFrom(type);
    }

    public boolean isInstance(Object instance)
    {
	return baseClass.isInstance(instance);
    }

    public Iterator methods()
    {
	return baseClass.methods();
    }

    public Object newInstance(IRuntimeEnv env)
    {
	return baseClass.newInstance(env);
    }

    public Object nullObject()
    {
	return baseClass.nullObject();
    }

    public Iterator superClasses()
    {
	return baseClass.superClasses();
    }

    public String getNameSpace()
    {
	return nameSpace;
    }

    public IOpenClass getOpenClass()
    {
	return this;
    }

    public void setNameSpace(String nameSpace)
    {
        this.nameSpace = nameSpace;
    }
    
    
    
}
