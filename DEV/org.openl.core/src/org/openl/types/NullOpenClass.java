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
import org.openl.syntax.impl.ISyntaxConstants;
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

    public IAggregateInfo getAggregateInfo() {
        return null;
    }

    public String getDisplayName(int mode) {
        return getName();
    }

    public IDomain<?> getDomain() {
        return null;
    }

    public IOpenField getField(String name) {
        return null;
    }

    public IOpenField getField(String fname, boolean strictMatch) {
        return null;
    }

    public IOpenField getIndexField() {
        return null;
    }

    public Class<?> getInstanceClass() {
        return null;
    }

    @Override
    public IOpenMethod getConstructor(IOpenClass[] params) {
        return null;
    }

    public IMetaInfo getMetaInfo() {
        return null;
    }

    public IOpenMethod getMethod(String name, IOpenClass[] classes) {
        return null;
    }

    public String getName() {
        return "null-Class";
    }

    public String getJavaName() {
        return getName();
    }

    @Override
    public String getPackageName() {
        return getName();
    }

    public String getNameSpace() {
        return ISyntaxConstants.THIS_NAMESPACE;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClassHolder#getOpenClass()
     */
    public IOpenClass getOpenClass() {
        return this;
    }

    public IOpenField getVar(String fname, boolean strictMatch) {
        return null;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    public IOpenClass getComponentClass() {
        return null;
    }

    public boolean isAssignableFrom(Class<?> c) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openl.types.IOpenClass#isAssignableFrom(org.openl.types.IOpenClass)
     */
    public boolean isAssignableFrom(IOpenClass ioc) {
        return ioc == this;
    }

    public boolean isAssignableFrom(IType type) {
        return false;
    }

    public boolean isInstance(Object instance) {
        return instance == null;
    }

    public boolean isSimple() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClass#newInstance()
     */
    public Object newInstance(IRuntimeEnv env) {
        return null;
    }

    public Object nullObject() {
        return null;
    }

    public void setMetaInfo(IMetaInfo info) {
    }

    public Iterable<IOpenClass> superClasses() {
        return Collections.emptyList();
    }

    public void addType(IOpenClass type) throws Exception {
    }

    public IOpenClass findType(String typeName) {
        // Default implementation
        return null;
    }

    public Collection<IOpenClass> getTypes() {
        // Default implementation
        return Collections.emptyList();
    }

    public Map<String, IOpenField> getFields() {
        // Default implementation
        return Collections.emptyMap();
    }

    public Map<String, IOpenField> getDeclaredFields() {
        // Default implementation
        return Collections.emptyMap();
    }

    public Collection<IOpenMethod> getMethods() {
        // Default implementation
        return Collections.emptyList();
    }

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
}
