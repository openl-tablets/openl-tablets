/*
 * Created on Jun 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public abstract class AOpenClass implements IOpenClass {

    protected static final Map<MethodKey, IOpenMethod> STUB = Collections
        .unmodifiableMap(Collections.<MethodKey, IOpenMethod> emptyMap());
    private IOpenField indexField;

    protected IMetaInfo metaInfo;
    protected Map<String, IOpenField> uniqueLowerCaseFieldMap = null;

    protected Map<String, List<IOpenField>> nonUniqueLowerCaseFieldMap = null;

    protected synchronized void addFieldToLowerCaseMap(IOpenField f) {
        if (uniqueLowerCaseFieldMap == null) {
            return;
        }
        String lname = f.getName().toLowerCase().replace(" ", "");

        if (uniqueLowerCaseFieldMap.containsKey(lname)) {
            initNonUniqueMap();
            List<IOpenField> ff = new ArrayList<IOpenField>(2);
            ff.add(uniqueLowerCaseFieldMap.get(lname));
            ff.add(f);
            nonUniqueLowerCaseFieldMap.put(lname, ff);
            uniqueLowerCaseFieldMap.remove(lname);
        } else if (nonUniqueLowerCaseFieldMap != null && nonUniqueLowerCaseFieldMap.containsKey(lname)) {
            nonUniqueLowerCaseFieldMap.get(lname).add(f);
        } else {
            uniqueLowerCaseFieldMap.put(lname, f);
        }
    }

    protected abstract Map<String, IOpenField> fieldMap();

    public Map<String, IOpenField> getFields() {
        Map<String, IOpenField> fields = new HashMap<String, IOpenField>();
        Iterable<IOpenClass> superClasses = superClasses();
        for (IOpenClass superClass : superClasses) {
            fields.putAll(superClass.getFields());
        }
        fields.putAll(fieldMap());
        return fields;
    }

    public Map<String, IOpenField> getDeclaredFields() {
        return new HashMap<String, IOpenField>(fieldMap());
    }

    public static IOpenClass getArrayType(IOpenClass openClass, int dim) {
        if (dim > 0) {
            IOpenClass arrayType = JavaOpenClass
                .getOpenClass(Array.newInstance(openClass.getInstanceClass(), dim).getClass());
            if (openClass.getDomain() != null) {
                StringBuilder domainOpenClassName = new StringBuilder(openClass.getName());
                for (int j = 0; j < dim; j++) {
                    domainOpenClassName.append("[]");
                }
                DomainOpenClass domainArrayType = new DomainOpenClass(domainOpenClassName.toString(),
                    arrayType,
                    openClass.getDomain(),
                    null);
                return domainArrayType;
            } else {
                return arrayType;
            }
        }
        throw new IllegalArgumentException();
    }

    public IOpenClass getArrayType(int dim) {
        return getArrayType(this, dim);
    }

    public IDomain<?> getDomain() {
        return null;
    }

    public IOpenField getField(String fname) {
        return getField(fname, true);
    }

    public IOpenField getField(String fname, boolean strictMatch) {

        IOpenField f = null;
        if (strictMatch) {

            Map<String, IOpenField> m = fieldMap();

            f = m == null ? null : m.get(fname);

            if (f != null) {
                return f;
            } else {
                return searchFieldFromSuperClass(fname, strictMatch);
            }
        }
        
        String lfname = fname.toLowerCase();

        Map<String, IOpenField> uniqueLowerCaseFields = getUniqueLowerCaseFieldMap();

        if (uniqueLowerCaseFields != null) {
            f = uniqueLowerCaseFields.get(lfname);
            if (f != null) {
                return f;
            }
        }

        Map<String, List<IOpenField>> nonUniqueLowerCaseFields = getNonUniqueLowerCaseFieldMap();

        List<IOpenField> ff = nonUniqueLowerCaseFields.get(lfname);

        if (ff != null) {
            throw new AmbiguousVarException(fname, ff);
        }

        return searchFieldFromSuperClass(fname, strictMatch);
    }

    private IOpenField searchFieldFromSuperClass(String fname, boolean strictMatch) {
        IOpenField f;
        Iterable<IOpenClass> superClasses = superClasses();
        for (IOpenClass superClass : superClasses) {
            f = superClass.getField(fname, strictMatch);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    public IOpenField getIndexField() {
        return indexField;
    }

    public IOpenMethod getConstructor(IOpenClass[] params) {
        Map<MethodKey, IOpenMethod> m = constructorMap();
        MethodKey methodKey = new MethodKey(params);
        return m.get(methodKey);
    }

    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public IOpenMethod getMethod(String name, IOpenClass[] classes) {

        IOpenMethod method = getDeclaredMethod(name, classes);

        // If method is not found try to find it in parent classes.
        //
        if (method == null) {
            Iterator<IOpenClass> superClasses = superClasses().iterator();

            while (method == null && superClasses.hasNext()) {
                method = superClasses.next().getMethod(name, classes);
            }
        }

        return method;
    }

    private synchronized Map<String, List<IOpenField>> getNonUniqueLowerCaseFieldMap() {
        if (nonUniqueLowerCaseFieldMap == null) {
            makeLowerCaseMaps();
        }
        return nonUniqueLowerCaseFieldMap;
    }

    private synchronized Map<String, IOpenField> getUniqueLowerCaseFieldMap() {
        if (uniqueLowerCaseFieldMap == null) {
            makeLowerCaseMaps();
        }
        return uniqueLowerCaseFieldMap;
    }

    public IOpenField getVar(String name, boolean strictMatch) {
        return getField(name, strictMatch);
    }

    private void initNonUniqueMap() {
        if (nonUniqueLowerCaseFieldMap == null) {
            nonUniqueLowerCaseFieldMap = new HashMap<String, List<IOpenField>>();
        }
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isAssignableFrom(IType type) {
        if (type instanceof IOpenClass) {
            return isAssignableFrom((IOpenClass) type);
        }
        return false;
    }

    public boolean isSimple() {
        return false;
    }

    public boolean isArray() {
        if (getInstanceClass() != null) {
            return getInstanceClass().isArray();
        }
        return false;
    }

    public IOpenClass getComponentClass() {
        // Default implementation. Open classes that can be represented as
        // arrays, should override this method.
        //
        return null;
    }

    private void makeLowerCaseMaps() {
        uniqueLowerCaseFieldMap = new HashMap<String, IOpenField>();

        for (IOpenField field : getFields().values()) {
            addFieldToLowerCaseMap(field);
        }

        if (nonUniqueLowerCaseFieldMap == null) {
            nonUniqueLowerCaseFieldMap = Collections.emptyMap();
        }

    }

    private volatile Map<MethodKey, IOpenMethod> methodMap;
    private volatile Map<MethodKey, IOpenMethod> constructorMap;

    private Map<MethodKey, IOpenMethod> methodMap() {
        if (methodMap == null) {
            synchronized (this) {
                if (methodMap == null) {
                    methodMap = initMethodMap();
                }
            }
        }
        return methodMap;
    }

    private Map<MethodKey, IOpenMethod> constructorMap() {
        if (constructorMap == null) {
            synchronized (this) {
                if (constructorMap == null) {
                    constructorMap = initConstructorMap();
                }
            }
        }
        return constructorMap;
    }

    protected Map<MethodKey, IOpenMethod> initMethodMap() {
        return STUB;
    }

    protected Map<MethodKey, IOpenMethod> initConstructorMap() {
        return STUB;
    }

    private IOpenMethod putMethod(IOpenMethod method) {
        if (methodMap == null || methodMap == STUB) {
            synchronized (this) {
                if (methodMap == null) {
                    methodMap = initMethodMap();
                }
                if (methodMap == STUB) {
                    methodMap = new HashMap<MethodKey, IOpenMethod>(4);
                }
            }
        }
        MethodKey key = new MethodKey(method);
        final IOpenMethod existMethod = methodMap.put(key, method);
        return existMethod;
    }

    private IOpenMethod putConstructor(IOpenMethod method) {
        if (constructorMap == null || constructorMap == STUB) {
            synchronized (this) {
                if (constructorMap == null) {
                    constructorMap = initConstructorMap();
                }
                if (constructorMap == STUB) {
                    constructorMap = new HashMap<MethodKey, IOpenMethod>(4);
                }
            }
        }
        MethodKey key = new MethodKey(method);
        final IOpenMethod existConstructor = constructorMap.put(key, method);
        return existConstructor;
    }

    protected void addMethod(IOpenMethod method) {
        MethodKey key = new MethodKey(method);
        final IOpenMethod existMethod = putMethod(method);
        if (existMethod != null) {
            throw new DuplicatedMethodException(
                "Method '" + key + "' have bean already defined for class '" + getName() + "'",
                method);
        }
        invalidateInternalData();
    }

    public void addConstructor(IOpenMethod method) {
        MethodKey key = new MethodKey(method);
        final IOpenMethod existCostructor = putConstructor(method);
        if (existCostructor != null) {
            throw new DuplicatedMethodException(
                "Constructor '" + key + "' have bean already defined for class '" + getName() + "'",
                method);
        }
    }

    protected void overrideMethod(IOpenMethod method) {
        MethodKey key = new MethodKey(method);
        final IOpenMethod existMethod = putMethod(method);
        if (existMethod == null) {
            throw new IllegalStateException("Method '" + key + "' is absent to override in class '" + getName() + "'");
        }
        invalidateInternalData();
    }

    protected final void invalidateInternalData() {
        allMethodsCacheInvalidated = true;
        allMethodNamesMapInvalidated = true;
        allConstructorNamesMapInvalidated = true;
        constructorMap = null;
    }

    private Collection<IOpenMethod> allMethodsCache = null;
    private volatile boolean allMethodsCacheInvalidated = true;

    public final Collection<IOpenMethod> getMethods() {
        if (allMethodsCacheInvalidated) {
            synchronized (this) {
                if (allMethodNamesMapInvalidated) {
                    allMethodsCache = buildAllMethods();
                    allMethodsCacheInvalidated = false;
                }
            }
        }
        return allMethodsCache;
    }

    private Collection<IOpenMethod> buildAllMethods() {
        Map<MethodKey, IOpenMethod> methods = new HashMap<MethodKey, IOpenMethod>();
        Iterable<IOpenClass> superClasses = superClasses();
        for (IOpenClass superClass : superClasses) {
            for (IOpenMethod method : superClass.getMethods()) {
                methods.put(new MethodKey(method), method);
            }
        }
        final Map<MethodKey, IOpenMethod> m = methodMap();
        if (m != null) {
            methods.putAll(m);
        }
        if (methods.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(methods.values());
    }

    public IOpenMethod getDeclaredMethod(String name, IOpenClass[] classes) {
        Map<MethodKey, IOpenMethod> m = methodMap();
        MethodKey methodKey = new MethodKey(name, classes);
        return m.get(methodKey);
    }

    public Collection<IOpenMethod> getDeclaredMethods() {
        return methodMap().values();
    }

    public Object nullObject() {
        return null;
    }

    public void setIndexField(IOpenField field) {
        indexField = field;
    }

    public void setMetaInfo(IMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Default implementation.
     * 
     * @param type IOpenClass instance
     * @throws Exception if an error had occurred.
     */
    public void addType(IOpenClass type) throws Exception {
    }

    @Override
    public IOpenClass findType(String name) {
        return null;
    }

    /**
     * Default implementation. Always returns <code>null</code>.
     * 
     */
    public Collection<IOpenClass> getTypes() {
        // Default implementation.
        // To do nothing. Not everyone has internal types.
        return Collections.emptyList();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IOpenClass)) {
            return false;
        }
        return Objects.equals(getName(), ((IOpenClass) obj).getName());
    }

    private Map<String, List<IOpenMethod>> allMethodNamesMap = null;

    private volatile boolean allMethodNamesMapInvalidated = true;

    private Collection<IOpenMethod> allConstructors = null;

    private volatile boolean allConstructorNamesMapInvalidated = true;

    @Override
    public final Iterable<IOpenMethod> methods(String name) {
        if (allMethodNamesMapInvalidated) {
            synchronized (this) {
                if (allMethodNamesMapInvalidated) {
                    allMethodNamesMap = buildMethodNameMap(getMethods());
                    allMethodNamesMapInvalidated = false;
                }
            }
        }
        List<IOpenMethod> found = allMethodNamesMap.get(name);
        return found == null ? Collections.<IOpenMethod> emptyList() : Collections.unmodifiableList(found);
    }

    @Override
    public final Iterable<IOpenMethod> constructors() {
        if (allConstructorNamesMapInvalidated) {
            synchronized (this) {
                if (allConstructorNamesMapInvalidated) {
                    allConstructors = Collections.unmodifiableCollection(constructorMap().values());
                    allConstructorNamesMapInvalidated = false;
                }
            }
        }
        return allConstructors == null ? Collections.<IOpenMethod> emptyList() : allConstructors;
    }

    public static Map<String, List<IOpenMethod>> buildMethodNameMap(Iterable<IOpenMethod> methods) {
        Map<String, List<IOpenMethod>> res = new HashMap<String, List<IOpenMethod>>();

        for (IOpenMethod m : methods) {
            String name = m.getName();

            List<IOpenMethod> list = res.get(name);
            if (list == null) {
                list = new LinkedList<IOpenMethod>();
                res.put(name, list);
            }
            list.add(m);
        }

        return res;
    }
}
