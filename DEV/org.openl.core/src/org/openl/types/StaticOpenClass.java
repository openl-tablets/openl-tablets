package org.openl.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class StaticOpenClass implements IOpenClass {

    private final IOpenClass delegate;

    public StaticOpenClass(IOpenClass delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public String getDisplayName(int mode) {
        return "Reference<" + delegate.getDisplayName(mode) + ">";
    }

    @Override
    public String getName() {
        return "Reference<" + delegate.getName() + ">";
    }

    @Override
    public IDomain<?> getDomain() {
        return delegate.getDomain();
    }

    public IOpenClass getDelegate() {
        return delegate;
    }

    @Override
    public boolean isAssignableFrom(IType type) {
        return JavaOpenClass.CLASS.isAssignableFrom(type);
    }

    @Override
    public IOpenMethod getConstructor(IOpenClass[] params) throws AmbiguousMethodException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<IOpenMethod> methods(String name) {
        return StreamSupport.stream(delegate.methods(name).spliterator(), false)
                .filter(IOpenMember::isStatic)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<IOpenMethod> constructors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOpenField getVar(String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField staticVar = delegate.getVar(name, strictMatch);
        return staticVar != null && staticVar.isStatic() ? staticVar : null;
    }

    @Override
    public IMetaInfo getMetaInfo() {
        return delegate.getMetaInfo();
    }

    @Override
    public void setMetaInfo(IMetaInfo info) {
        delegate.setMetaInfo(info);
    }

    @Override
    public Collection<IOpenField> getFields() {
        return delegate.getStaticFields();
    }

    @Override
    public Collection<IOpenField> getDeclaredFields() {
        return delegate.getDeclaredFields().stream().filter(IOpenField::isStatic).collect(Collectors.toList());
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return delegate.getAggregateInfo();
    }

    @Override
    public IOpenField getField(String name) {
        return delegate.getStaticField(name);
    }

    @Override
    public IOpenField getStaticField(String name) {
        return delegate.getStaticField(name);
    }

    @Override
    public IOpenField getStaticField(String name, boolean strictMatch) {
        return delegate.getStaticField(name, strictMatch);
    }

    @Override
    public Collection<IOpenField> getStaticFields() {
        return delegate.getStaticFields();
    }

    @Override
    public IOpenField getField(String name, boolean strictMatch) throws AmbiguousFieldException {
        return getStaticField(name, strictMatch);
    }

    @Override
    public IOpenField getIndexField() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getInstanceClass() {
        return JavaOpenClass.CLASS.getInstanceClass();
    }

    @Override
    public String getJavaName() {
        return JavaOpenClass.CLASS.getJavaName();
    }

    @Override
    public String getPackageName() {
        return delegate.getPackageName();
    }

    @Override
    public IOpenMethod getMethod(String name, IOpenClass[] classes) {
        IOpenMethod method = delegate.getMethod(name, classes);
        return method != null && method.isStatic() ? method : null;
    }

    @Override
    public boolean isAbstract() {
        return JavaOpenClass.CLASS.isAbstract();
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        return JavaOpenClass.CLASS.isAssignableFrom(ioc);
    }

    @Override
    public boolean isInstance(Object instance) {
        return JavaOpenClass.CLASS.isInstance(instance);
    }

    @Override
    public boolean isSimple() {
        return JavaOpenClass.CLASS.isSimple();
    }

    @Override
    public boolean isArray() {
        return JavaOpenClass.CLASS.isArray();
    }

    @Override
    public IOpenClass getComponentClass() {
        return delegate.getComponentClass() != null ? new StaticOpenClass(delegate.getComponentClass()) : null;
    }

    @Override
    public Collection<IOpenMethod> getMethods() {
        return delegate.getMethods().stream().filter(IOpenMethod::isStatic).collect(Collectors.toList());
    }

    @Override
    public Collection<IOpenMethod> getDeclaredMethods() {
        return delegate.getDeclaredMethods().stream().filter(IOpenMethod::isStatic).collect(Collectors.toList());
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object nullObject() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<IOpenClass> superClasses() {
        return Collections.singleton(JavaOpenClass.CLASS);
    }

    @Override
    public void addType(IOpenClass type) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOpenClass findType(String name) {
        return new StaticOpenClass(delegate.findType(name));
    }

    @Override
    public Collection<IOpenClass> getTypes() {
        return delegate.getTypes().stream().map(StaticOpenClass::new).collect(Collectors.toList());
    }

    @Override
    public IOpenClass getArrayType(int dim) {
        return new StaticOpenClass(delegate.getArrayType(dim));
    }

    @Override
    public boolean isInterface() {
        return JavaOpenClass.CLASS.isInterface();
    }

    @Override
    public IOpenClass toStaticClass() {
        return this;
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public String toString() {
        return getName();
    }
}
