package org.openl.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.vm.IRuntimeEnv;

public class UnknownOpenClass implements IOpenClass {

    public static final UnknownOpenClass the = new UnknownOpenClass();

    private UnknownOpenClass() {
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return null;
    }

    @Override
    public String getDisplayName(int mode) {
        return getName();
    }

    @Override
    public IDomain<?> getDomain() {
        return null;
    }

    @Override
    public IOpenField getField(String name) {
        return null;
    }

    @Override
    public IOpenField getField(String fname, boolean strictMatch) {
        return null;
    }

    @Override
    public IOpenField getIndexField() {
        return null;
    }

    @Override
    public Class<?> getInstanceClass() {
        return null;
    }

    @Override
    public IOpenMethod getConstructor(IOpenClass[] params) {
        return null;
    }

    @Override
    public IMetaInfo getMetaInfo() {
        return null;
    }

    @Override
    public IOpenMethod getMethod(String name, IOpenClass[] classes) {
        return null;
    }

    @Override
    public String getName() {
        return "unknown-Class";
    }

    @Override
    public String getJavaName() {
        return getName();
    }

    @Override
    public String getPackageName() {
        return getName();
    }

    @Override
    public IOpenField getVar(String fname, boolean strictMatch) {
        return null;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public IOpenClass getComponentClass() {
        return null;
    }

    @Override
    public boolean isAssignableFrom(Class<?> c) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClass#isAssignableFrom(org.openl.types.IOpenClass)
     */
    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        return ioc == this;
    }

    @Override
    public boolean isAssignableFrom(IType type) {
        return false;
    }

    @Override
    public boolean isInstance(Object instance) {
        return true;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClass#newInstance()
     */
    @Override
    public Object newInstance(IRuntimeEnv env) {
        return new UnsupportedOperationException();
    }

    @Override
    public Object nullObject() {
        return new UnsupportedOperationException();
    }

    @Override
    public void setMetaInfo(IMetaInfo info) {
    }

    @Override
    public Iterable<IOpenClass> superClasses() {
        return Collections.emptyList();
    }

    @Override
    public void addType(IOpenClass type) throws Exception {
    }

    @Override
    public IOpenClass findType(String typeName) {
        return null;
    }

    @Override
    public Collection<IOpenClass> getTypes() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, IOpenField> getFields() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, IOpenField> getDeclaredFields() {
        return Collections.emptyMap();
    }

    @Override
    public Collection<IOpenMethod> getMethods() {
        return Collections.emptyList();
    }

    @Override
    public Collection<IOpenMethod> getDeclaredMethods() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<IOpenMethod> methods(String name) {
        return Collections.emptyList();
    }

    @Override
    public Iterable<IOpenMethod> constructors() {
        return Collections.emptyList();
    }

    @Override
    public IOpenClass getArrayType(int dim) {
        return this;
    }

    @Override
    public boolean isInterface() {
        return false;
    }
}
