/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class NullOpenClass implements IOpenClass {

    public static final NullOpenClass the = new NullOpenClass();

    public static boolean isAnyNull(IOpenClass... args) {
        for (IOpenClass arg : args) {
            if (arg == the)
                return true;
        }

        return false;
    }

    private NullOpenClass() {
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
        return "null-Class";
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
     * @see
     * org.openl.types.IOpenClass#isAssignableFrom(org.openl.types.IOpenClass)
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
        return instance == null;
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
        return null;
    }

    @Override
    public Object nullObject() {
        return null;
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
        // Default implementation
        return null;
    }

    @Override
    public Collection<IOpenClass> getTypes() {
        // Default implementation
        return Collections.emptyList();
    }

    @Override
    public Map<String, IOpenField> getFields() {
        // Default implementation
        return Collections.emptyMap();
    }

    @Override
    public Map<String, IOpenField> getDeclaredFields() {
        // Default implementation
        return Collections.emptyMap();
    }

    @Override
    public Collection<IOpenMethod> getMethods() {
        // Default implementation
        return Collections.emptyList();
    }

    @Override
    public Collection<IOpenMethod> getDeclaredMethods() {
        // Default implementation
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
