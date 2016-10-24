/*
 * Created on Jun 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openl.binding.ICastFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public abstract class AOpenClass implements IOpenClass {

    private static final Map<MethodKey, IOpenMethod> STUB = Collections
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

    public IOpenClass getArrayType(int dim) {
        return JavaOpenClass.getOpenClass(getInstanceClass()).getArrayType(dim);
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

    public IOpenMethod getMatchingMethod(String name, IOpenClass[] params) throws AmbiguousMethodException {
        return getMethod(name, params);
    }

    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    private ICastFactory castFactory;

    private ICastFactory getCastFactory() {
        if (castFactory == null) {
            castFactory = new CastFactory();
        }
        return castFactory;
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

        if (method != null && hasAliasTypeParams(method)) {

            IOpenClass[] methodParams = method.getSignature().getParameterTypes();
            IOpenCast[] typeCasts = new IOpenCast[methodParams.length];

            ICastFactory castFactory = getCastFactory();

            for (int i = 0; i < methodParams.length; i++) {
                IOpenClass methodParam = methodParams[i];
                IOpenClass param = classes[i];

                IOpenCast castObject = castFactory.getCast(param, methodParam);
                typeCasts[i] = castObject;
            }

            IMethodCaller methodCaller = new CastingMethodCaller(method, typeCasts);
            method = new MethodDelegator(methodCaller);
        }

        return method;
    }

    private boolean hasAliasTypeParams(IOpenMethod method) {
        IOpenClass[] params = method.getSignature().getParameterTypes();

        for (IOpenClass param : params) {
            if (param instanceof DomainOpenClass) {
                return true;
            }
        }

        return false;
    }

    public String getNameSpace() {
        return ISyntaxConstants.THIS_NAMESPACE;
    }

    private synchronized Map<String, List<IOpenField>> getNonUniqueLowerCaseFieldMap() {
        if (nonUniqueLowerCaseFieldMap == null) {
            makeLowerCaseMaps();
        }
        return nonUniqueLowerCaseFieldMap;
    }

    public IOpenClass getOpenClass() {
        return this;
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

    private Map<MethodKey, IOpenMethod> methodMap;

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

    protected Map<MethodKey, IOpenMethod> initMethodMap() {
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

    protected void addMethod(IOpenMethod method) {
        MethodKey key = new MethodKey(method);
        final IOpenMethod existMethod = putMethod(method);
        if (existMethod != null) {
            throw new DuplicatedMethodException(
                "Method '" + key + "' have bean already defined for class '" + getName() + "'", method);
        }
        invalidateMethodCaches();
    }

    protected void overrideMethod(IOpenMethod method) {
        MethodKey key = new MethodKey(method);
        final IOpenMethod existMethod = putMethod(method);
        if (existMethod == null) {
            throw new IllegalStateException("Method '" + key + "' is absent to override in class '" + getName() + "'");
        }
        invalidateMethodCaches();
    }

    protected void invalidateMethodCaches() {
        allMethodsCacheInvalidated = true;
        allMethodNamesMapInvalidated = true;
    }

    private Collection<IOpenMethod> allMethodsCache = null;
    private volatile boolean allMethodsCacheInvalidated = true;

    public synchronized Collection<IOpenMethod> getMethods() {
        if (allMethodsCacheInvalidated) {
            allMethodsCache = buildAllMethods();
            allMethodsCacheInvalidated = false;
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
        if (m.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(methods.values());
    }

    protected IOpenMethod getDeclaredMethod(String name, IOpenClass[] classes) {
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
    public IOpenClass addType(String namespace, IOpenClass type) throws Exception {
        return type;
    }
    
    @Override
    public IOpenClass findType(String namespace, String name) {
        return null;
    }

    /**
     * Default implementation. Always returns <code>null</code>.
     * 
     */
    public Map<String, IOpenClass> getTypes() {
        // Default implementation.
        // To do nothing. Not everyone has internal types.
        return Collections.EMPTY_MAP;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getName()).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IOpenClass)) {
            return false;
        }
        return new EqualsBuilder().append(getName(), ((IOpenClass) obj).getName()).isEquals();
    }

    private Map<String, List<IOpenMethod>> allMethodNamesMap = null;
    private volatile boolean allMethodNamesMapInvalidated = true;

    @Override
    public synchronized Iterable<IOpenMethod> methods(String name) {
        if (allMethodNamesMapInvalidated) {
            synchronized (this) {
                allMethodNamesMap = buildMethodNameMap(getMethods());
                allMethodNamesMapInvalidated = false;
            }
        }
        List<IOpenMethod> found = allMethodNamesMap.get(name);
        return found == null ? Collections.<IOpenMethod> emptyList() : Collections.unmodifiableList(found);
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
