package org.openl.binding.impl.method;

import java.util.Collection;
import java.util.Map;

import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class NoVarArgOpenClass implements IOpenClass {

    private final IOpenClass delegate;

    public NoVarArgOpenClass(IOpenClass delegate) {
        this.delegate = delegate;
    }

    @Override
    public Map<String, IOpenField> getFields() {
        return delegate.getFields();
    }

    @Override
    public Map<String, IOpenField> getDeclaredFields() {
        return delegate.getDeclaredFields();
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return delegate.getAggregateInfo();
    }

    @Override
    public IOpenField getField(String name) {
        return delegate.getField(name);
    }

    @Override
    public IOpenField getField(String name, boolean strictMatch) throws AmbiguousVarException {
        return delegate.getField(name, strictMatch);
    }

    @Override
    public IOpenField getIndexField() {
        return delegate.getIndexField();
    }

    @Override
    public Class<?> getInstanceClass() {
        return delegate.getInstanceClass();
    }

    @Override
    public String getJavaName() {
        return delegate.getJavaName();
    }

    @Override
    public String getPackageName() {
        return delegate.getPackageName();
    }

    @Override
    public IOpenMethod getMethod(String name, IOpenClass[] classes) {
        return delegate.getMethod(name, classes);
    }

    @Override
    public boolean isAbstract() {
        return delegate.isAbstract();
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        return delegate.isAssignableFrom(ioc);
    }

    @Override
    public boolean isInstance(Object instance) {
        return delegate.isInstance(instance);
    }

    @Override
    public boolean isSimple() {
        return delegate.isSimple();
    }

    @Override
    public boolean isArray() {
        return delegate.isArray();
    }

    @Override
    public IOpenClass getComponentClass() {
        return delegate.getComponentClass();
    }

    @Override
    public Collection<IOpenMethod> getMethods() {
        return delegate.getMethods();
    }

    @Override
    public Collection<IOpenMethod> getDeclaredMethods() {
        return delegate.getDeclaredMethods();
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        return delegate.newInstance(env);
    }

    @Override
    public Object nullObject() {
        return delegate.nullObject();
    }

    @Override
    public Iterable<IOpenClass> superClasses() {
        return delegate.superClasses();
    }

    @Override
    public void addType(IOpenClass type) throws Exception {
        delegate.addType(type);
    }

    @Override
    public IOpenClass findType(String name) {
        return delegate.findType(name);
    }

    @Override
    public Collection<IOpenClass> getTypes() {
        return delegate.getTypes();
    }

    @Override
    public IOpenClass getArrayType(int dim) {
        return delegate.getArrayType(dim);
    }

    @Override
    public boolean isInterface() {
        return delegate.isInterface();
    }

    @Override
    public IDomain<?> getDomain() {
        return delegate.getDomain();
    }

    @Override
    public boolean isAssignableFrom(IType type) {
        return delegate.isAssignableFrom(type);
    }

    @Override
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public IOpenMethod getConstructor(IOpenClass[] params) throws AmbiguousMethodException {
        return delegate.getConstructor(params);
    }

    @Override
    public Iterable<IOpenMethod> methods(String name) {
        return delegate.methods(name);
    }

    @Override
    public Iterable<IOpenMethod> constructors() {
        return delegate.constructors();
    }

    @Override
    public IOpenField getVar(String name, boolean strictMatch) throws AmbiguousVarException {
        return delegate.getVar(name, strictMatch);
    }

    @Override
    public IMetaInfo getMetaInfo() {
        return delegate.getMetaInfo();
    }

    @Override
    public void setMetaInfo(IMetaInfo info) {
        delegate.setMetaInfo(info);
    }
}
