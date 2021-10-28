package org.openl.types;

import java.util.Collection;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.vm.IRuntimeEnv;

public class OpenClassDelegator implements IOpenClass {
    private final IOpenClass delegate;

    public OpenClassDelegator(IOpenClass delegate) {
        this.delegate = delegate;
    }

    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    public String getName() {
        return delegate.getName();
    }

    public IDomain<?> getDomain() {
        return delegate.getDomain();
    }

    public boolean isAssignableFrom(IType type) {
        return delegate.isAssignableFrom(type);
    }

    public IOpenMethod getConstructor(IOpenClass[] params) throws AmbiguousMethodException {
        return delegate.getConstructor(params);
    }

    public Iterable<IOpenMethod> methods(String name) {
        return delegate.methods(name);
    }

    public Iterable<IOpenMethod> constructors() {
        return delegate.constructors();
    }

    public IOpenField getVar(String name, boolean strictMatch) throws AmbiguousFieldException {
        return delegate.getVar(name, strictMatch);
    }

    public IMetaInfo getMetaInfo() {
        return delegate.getMetaInfo();
    }

    public void setMetaInfo(IMetaInfo info) {
        delegate.setMetaInfo(info);
    }

    public Collection<IOpenField> getFields() {
        return delegate.getFields();
    }

    public Collection<IOpenField> getDeclaredFields() {
        return delegate.getDeclaredFields();
    }

    public IAggregateInfo getAggregateInfo() {
        return delegate.getAggregateInfo();
    }

    public IOpenField getField(String name) {
        return delegate.getField(name);
    }

    public IOpenField getField(String name, boolean strictMatch) throws AmbiguousFieldException {
        return delegate.getField(name, strictMatch);
    }

    public IOpenField getIndexField() {
        return delegate.getIndexField();
    }

    public Class<?> getInstanceClass() {
        return delegate.getInstanceClass();
    }

    public String getJavaName() {
        return delegate.getJavaName();
    }

    public String getPackageName() {
        return delegate.getPackageName();
    }

    public IOpenMethod getMethod(String name, IOpenClass[] classes) {
        return delegate.getMethod(name, classes);
    }

    public boolean isAbstract() {
        return delegate.isAbstract();
    }

    public boolean isAssignableFrom(IOpenClass ioc) {
        return delegate.isAssignableFrom(ioc);
    }

    public boolean isInstance(Object instance) {
        return delegate.isInstance(instance);
    }

    public boolean isSimple() {
        return delegate.isSimple();
    }

    public boolean isArray() {
        return delegate.isArray();
    }

    public IOpenClass getComponentClass() {
        return delegate.getComponentClass();
    }

    public Collection<IOpenMethod> getMethods() {
        return delegate.getMethods();
    }

    public Collection<IOpenMethod> getDeclaredMethods() {
        return delegate.getDeclaredMethods();
    }

    public Object newInstance(IRuntimeEnv env) {
        return delegate.newInstance(env);
    }

    public Object nullObject() {
        return delegate.nullObject();
    }

    public Collection<IOpenClass> superClasses() {
        return delegate.superClasses();
    }

    public void addType(IOpenClass type) throws Exception {
        delegate.addType(type);
    }

    public IOpenClass findType(String name) {
        return delegate.findType(name);
    }

    public Collection<IOpenClass> getTypes() {
        return delegate.getTypes();
    }

    public IOpenClass getArrayType(int dim) {
        return delegate.getArrayType(dim);
    }

    public boolean isInterface() {
        return delegate.isInterface();
    }

    public IOpenField getStaticField(String name) {
        return delegate.getStaticField(name);
    }

    public IOpenField getStaticField(String name, boolean strictMatch) {
        return delegate.getStaticField(name, strictMatch);
    }

    public Collection<IOpenField> getStaticFields() {
        return delegate.getStaticFields();
    }

    public IOpenClass toStaticClass() {
        return delegate.toStaticClass();
    }

    public boolean isStatic() {
        return delegate.isStatic();
    }

    public IOpenClass getDelegate() {
        return delegate;
    }
}
