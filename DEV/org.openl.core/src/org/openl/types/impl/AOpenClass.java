/*
 * Created on Jun 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.lang.reflect.Array;
import java.util.*;

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
            List<IOpenField> ff = new ArrayList<>(2);
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

    @Override
    public Map<String, IOpenField> getFields() {
        Map<String, IOpenField> fields = new HashMap<>();
        Iterable<IOpenClass> superClasses = superClasses();
        for (IOpenClass superClass : superClasses) {
            fields.putAll(superClass.getFields());
        }
        fields.putAll(fieldMap());
        return fields;
    }

    @Override
    public Map<String, IOpenField> getDeclaredFields() {
        return new HashMap<>(fieldMap());
    }

    public static IOpenClass getArrayType(IOpenClass openClass, int dim) {
        if (dim > 0) {
            IOpenClass arrayType = JavaOpenClass
                .getOpenClass(Array.newInstance(openClass.getInstanceClass(), new int[dim]).getClass());
            if (openClass.getDomain() != null) {
                StringBuilder domainOpenClassName = new StringBuilder(openClass.getName());
                for (int j = 0; j < dim; j++) {
                    domainOpenClassName.append("[]");
                }
                return new DomainOpenClass(domainOpenClassName.toString(), arrayType, openClass.getDomain(), null);
            } else {
                return arrayType;
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public IOpenClass getArrayType(int dim) {
        return getArrayType(this, dim);
    }

    @Override
    public IDomain<?> getDomain() {
        return null;
    }

    @Override
    public IOpenField getField(String fname) {
        try {
            return getField(fname, true);
        } catch (AmbiguousVarException e) {
            return null;
        }
    }

    @Override
    public IOpenField getField(String fname, boolean strictMatch) throws AmbiguousVarException {

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

    private IOpenField searchFieldFromSuperClass(String fname, boolean strictMatch) throws AmbiguousVarException {
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

    @Override
    public IOpenField getIndexField() {
        return indexField;
    }

    @Override
    public IOpenMethod getConstructor(IOpenClass[] params) {
        Map<MethodKey, IOpenMethod> m = constructorMap();
        MethodKey methodKey = new MethodKey(params);
        return m.get(methodKey);
    }

    @Override
    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    @Override
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

    @Override
    public IOpenField getVar(String name, boolean strictMatch) throws AmbiguousVarException {
        return getField(name, strictMatch);
    }

    private void initNonUniqueMap() {
        if (nonUniqueLowerCaseFieldMap == null) {
            nonUniqueLowerCaseFieldMap = new HashMap<>();
        }
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isAssignableFrom(IType type) {
        if (type instanceof IOpenClass) {
            return isAssignableFrom((IOpenClass) type);
        }
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public boolean isArray() {
        if (getInstanceClass() != null) {
            return getInstanceClass().isArray();
        }
        return false;
    }

    @Override
    public IOpenClass getComponentClass() {
        // Default implementation. Open classes that can be represented as
        // arrays, should override this method.
        //
        return null;
    }

    private void makeLowerCaseMaps() {
        uniqueLowerCaseFieldMap = new HashMap<>();

        for (IOpenField field : getFields().values()) {
            addFieldToLowerCaseMap(field);
        }

        if (nonUniqueLowerCaseFieldMap == null) {
            nonUniqueLowerCaseFieldMap = new HashMap<>();
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
                    methodMap = new HashMap<>(4);
                }
            }
        }
        MethodKey key = new MethodKey(method);
        return methodMap.put(key, method);
    }

    private IOpenMethod putConstructor(IOpenMethod method) {
        if (constructorMap == null || constructorMap == STUB) {
            synchronized (this) {
                if (constructorMap == null) {
                    constructorMap = initConstructorMap();
                }
                if (constructorMap == STUB) {
                    constructorMap = new HashMap<>(4);
                }
            }
        }
        MethodKey key = new MethodKey(method);
        return constructorMap.put(key, method);
    }

    public void addMethod(IOpenMethod method) throws DuplicatedMethodException {
        MethodKey key = new MethodKey(method);
        final IOpenMethod existMethod = putMethod(method);
        if (existMethod != null) {
            throw new DuplicatedMethodException(String
                .format("Method '%s' is already defined in class '%s'", key, getName()), existMethod, method);
        }
        invalidateInternalData();
    }

    public void addConstructor(IOpenMethod method) throws DuplicatedMethodException {
        MethodKey key = new MethodKey(method);
        final IOpenMethod existCostructor = putConstructor(method);
        if (existCostructor != null) {
            throw new DuplicatedMethodException(String
                .format("Constructor '%s' is already defined in class '%s'", key, getName()), existCostructor, method);
        }
    }

    protected void overrideMethod(IOpenMethod method) {
        MethodKey key = new MethodKey(method);
        final IOpenMethod existMethod = putMethod(method);
        if (existMethod == null) {
            throw new IllegalStateException(
                String.format("Method '%s' is absent to override in class '%s'", key, getName()));
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

    @Override
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
        Map<MethodKey, IOpenMethod> methods = new HashMap<>();
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

    @Override
    public Collection<IOpenMethod> getDeclaredMethods() {
        return methodMap().values();
    }

    @Override
    public Object nullObject() {
        return null;
    }

    public void setIndexField(IOpenField field) {
        indexField = field;
    }

    @Override
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
    @Override
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
    @Override
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
        return found == null ? Collections.emptyList() : Collections.unmodifiableList(found);
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
        return allConstructors == null ? Collections.emptyList() : allConstructors;
    }

    public static Map<String, List<IOpenMethod>> buildMethodNameMap(Iterable<IOpenMethod> methods) {
        Map<String, List<IOpenMethod>> res = new HashMap<>();

        for (IOpenMethod m : methods) {
            String name = m.getName();
            List<IOpenMethod> list = res.computeIfAbsent(name, e -> new LinkedList<>());
            list.add(m);
        }

        return res;
    }

    @Override
    public boolean isInterface() {
        return false;
    }
}
