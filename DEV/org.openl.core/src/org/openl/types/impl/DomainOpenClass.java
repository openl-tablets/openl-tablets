package org.openl.types.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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

    @Override
    public IOpenField getField(String fname) {
        return baseClass.getField(fname);
    }

    @Override
    public IOpenField getField(String fname, boolean strictMatch) {
        return baseClass.getField(fname, strictMatch);
    }

    @Override
    public IOpenField getIndexField() {
        return baseClass.getIndexField();
    }

    @Override
    public Class<?> getInstanceClass() {
        return baseClass.getInstanceClass();
    }

    @Override
    public IOpenMethod getConstructor(IOpenClass[] params) {
        return baseClass.getConstructor(params);
    }

    @Override
    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    @Override
    public IOpenMethod getMethod(String mname, IOpenClass[] classes) {
        return baseClass.getMethod(mname, classes);
    }

    @Override
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

    @Override
    public IOpenField getVar(String vname, boolean strictMatch) {
        return baseClass.getVar(vname, strictMatch);
    }

    @Override
    public boolean isAbstract() {
        return baseClass.isAbstract();
    }

    @Override
    public boolean isAssignableFrom(Class<?> c) {
        return baseClass.isAssignableFrom(c);
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        return baseClass.isAssignableFrom(ioc);
    }

    @Override
    public boolean isAssignableFrom(IType type) {
        return baseClass.isAssignableFrom(type);
    }

    @Override
    public boolean isInstance(Object instance) {
        return baseClass.isInstance(instance);
    }

    @Override
    public boolean isSimple() {
        return baseClass.isSimple();
    }

    @Override
    public boolean isArray() {
        return getName().contains("[]");
    }

    @Override
    public IOpenClass getComponentClass() {
        return getAggregateInfo().getComponentType(this);
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        return baseClass.newInstance(env);
    }

    @Override
    public Object nullObject() {
        return baseClass.nullObject();
    }

    @Override
    public void setMetaInfo(IMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    @Override
    public Iterable<IOpenClass> superClasses() {
        return baseClass.superClasses();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void addType(IOpenClass type) throws Exception {
    }

    @Override
    public IOpenClass findType(String typeName) {
        // Default implementation.
        return null;
    }

    @Override
    public Collection<IOpenClass> getTypes() {
        // Default implementation
        return Collections.emptyList();
    }

    @Override
    public Map<String, IOpenField> getFields() {
        return baseClass.getFields();
    }

    @Override
    public Map<String, IOpenField> getDeclaredFields() {
        return baseClass.getDeclaredFields();
    }

    @Override
    public Collection<IOpenMethod> getMethods() {
        return baseClass.getMethods();
    }

    @Override
    public Collection<IOpenMethod> getDeclaredMethods() {
        return baseClass.getMethods();
    }

    @Override
    public Iterable<IOpenMethod> methods(String name) {
        return baseClass.methods(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        DomainOpenClass that = (DomainOpenClass) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public Iterable<IOpenMethod> constructors() {
        return baseClass.constructors();
    }

    @Override
    public IOpenClass getArrayType(int dim) {
        return AOpenClass.getArrayType(this, dim);
    }

    @Override
    public boolean isInterface() {
        return false;
    }
}
