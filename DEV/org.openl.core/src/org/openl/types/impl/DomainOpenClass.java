package org.openl.types.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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
        assert name != null;
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

    @Override
    public IOpenMethod getConstructor(IOpenClass[] params) throws AmbiguousMethodException {
        return baseClass.getConstructor(params);
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

    @Override
    public String getJavaName() {
        return baseClass.getJavaName();
    }

    @Override
    public String getPackageName() {
        return baseClass.getPackageName();
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
        return getName();
    }

    public void addType(IOpenClass type) throws Exception {
    }

    public IOpenClass findType(String typeName) {
        // Default implementation.
        return null;
    }

    public Collection<IOpenClass> getTypes() {
        // Default implementation
        return Collections.emptyList();
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

    public Iterable<IOpenMethod> methods(String name) {
        return baseClass.methods(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DomainOpenClass that = (DomainOpenClass) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public Iterable<IOpenMethod> constructors() {
        return baseClass.constructors();
    }
    
    public IOpenClass getArrayType(int dim) {
        return AOpenClass.getArrayType(this, dim);
    }

}
