package org.openl.types.impl;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.types.DomainOpenClassAggregateInfo;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

import java.util.Collection;
import java.util.Map;

/**
 * {@link IOpenClass} implementation, that adds restriction for instances of this class by {@link IDomain}
 *
 */
public class DomainOpenClass implements IOpenClass {
    
    private IDomain<?> domain;

    private IAggregateInfo aggregateInfo;
    private IOpenClass baseClass;
    private String name;
    private IMetaInfo metaInfo;

    public DomainOpenClass(String name, IOpenClass baseClass, IDomain<?> domain, IMetaInfo metaInfo) {
        this.baseClass = baseClass;
        this.name = name;
        this.metaInfo = metaInfo;
        this.domain = domain;
    }

    @Override    
    public IDomain<?> getDomain() {
        return domain;
    }
    
    public void setDomain(IDomain<?> domain) {
        this.domain = domain;
    }
    
    /**
     * Overriden to add the possibility to return special aggregate info for DomainOpenClass
     * 
     * @author DLiauchuk
     */
    @Override
    public IAggregateInfo getAggregateInfo() { 
    	if (aggregateInfo == null) {
    		aggregateInfo = DomainOpenClassAggregateInfo.DOMAIN_AGGREGATE;
    	}
    	return aggregateInfo;
    }

    @Override
    public String getDisplayName(int mode) {
        return getName();
    }

    public IOpenClass getBaseClass() {
        return baseClass;
    }

    public IOpenField getField(String fname) {
        return baseClass.getField(fname);
    }

    public IOpenField getField(String fname, boolean strictMatch) {
        return baseClass.getField(fname, strictMatch);
    }

    public IOpenField getIndexField() {
        return baseClass.getIndexField();
    }

    public Class<?> getInstanceClass() {
        return baseClass.getInstanceClass();
    }

    public IOpenMethod getMatchingMethod(String mname, IOpenClass[] params) throws AmbiguousMethodException {
        return baseClass.getMatchingMethod(mname, params);
    }

    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public IOpenMethod getMethod(String mname, IOpenClass[] classes) {
        return baseClass.getMethod(mname, classes);
    }

    public String getName() {
        return name;
    }

    public String getNameSpace() {
        return null;
    }

    public IOpenClass getOpenClass() {
        return this;
    }

    public IOpenField getVar(String vname, boolean strictMatch) {
        return baseClass.getVar(vname, strictMatch);
    }

    public boolean isAbstract() {
        return baseClass.isAbstract();
    }

    public boolean isAssignableFrom(Class<?> c) {
        return baseClass.isAssignableFrom(c);
    }

    public boolean isAssignableFrom(IOpenClass ioc) {
        return baseClass.isAssignableFrom(ioc);
    }

    public boolean isAssignableFrom(IType type) {
        return baseClass.isAssignableFrom(type);
    }

    public boolean isInstance(Object instance) {
        return baseClass.isInstance(instance);
    }

    public boolean isSimple() {
        return baseClass.isSimple();
    }

    public boolean isArray() {
        return baseClass.isArray();
    }

    public IOpenClass getComponentClass() {
        return baseClass.getComponentClass();
    }

    public Object newInstance(IRuntimeEnv env) {
        return baseClass.newInstance(env);
    }

    public Object nullObject() {
        return baseClass.nullObject();
    }

    public void setMetaInfo(IMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public Iterable<IOpenClass> superClasses() {
        return baseClass.superClasses();
    }

    @Override
    public String toString() {
        return (getNameSpace() == null ? "" : getNameSpace() + ":") + getName();
    }

    public IOpenClass addType(String namespace, IOpenClass type) throws Exception {
        return type;
    }

    public IOpenClass findType(String namespace, String typeName) {
        // Default implementation.
        return null;
    }

    public Map<String, IOpenClass> getTypes() {
        // Default implementation
        return null;
    }

    public Map<String, IOpenField> getFields() {
        return baseClass.getFields();
    }

    public Map<String, IOpenField> getDeclaredFields() {
        return baseClass.getDeclaredFields();
    }

    public Collection<IOpenMethod> getMethods() {
        return baseClass.getMethods();
    }

    public Collection<IOpenMethod> getDeclaredMethods() {
        return baseClass.getMethods();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getName()).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IOpenClass)) {
            return false;
        }
        return new EqualsBuilder().append(getName(), ((IOpenClass) obj).getName()).isEquals();
    }

    public Iterable<IOpenMethod> methods(String name) {
        return baseClass.methods(name);
    }
}
