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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.StaticOpenClass;

/**
 * @author snshor
 */
@Slf4j
public abstract class AOpenClass implements IOpenClass {

    private final AtomicReference<StaticOpenClass> staticOpenClass = new AtomicReference<>();


    protected static final Map<MethodKey, IOpenMethod> STUB = Map.of();
    private IOpenField indexField;

    protected IMetaInfo xlsMetaInfo;
    private final AtomicReference<LowerCaseFieldMaps> lowerCaseFieldMaps = new AtomicReference<>();

    /** The fields by lower-cased name: those whose name is unique apart from those sharing a name. */
    private record LowerCaseFieldMaps(Map<String, IOpenField> unique, Map<String, List<IOpenField>> nonUnique) {
    }

    private void addFieldToLowerCaseMaps(IOpenField f,
                                         Map<String, IOpenField> uniqueLCaseFieldMap,
                                         Map<String, List<IOpenField>> nonUniqueLCaseFieldMap) {
        var lname = f.getName().toLowerCase().replace(" ", "");
        if (uniqueLCaseFieldMap.containsKey(lname)) {
            var ff = new ArrayList<IOpenField>(2);
            ff.add(uniqueLCaseFieldMap.get(lname));
            ff.add(f);
            nonUniqueLCaseFieldMap.put(lname, ff);
            uniqueLCaseFieldMap.remove(lname);
        } else if (nonUniqueLCaseFieldMap.containsKey(lname)) {
            nonUniqueLCaseFieldMap.get(lname).add(f);
        } else {
            uniqueLCaseFieldMap.put(lname, f);
        }
    }

    protected void addFieldToLowerCaseMap(IOpenField f) {
        var maps = lowerCaseFieldMaps.get();
        if (maps == null) {
            return;
        }
        addFieldToLowerCaseMaps(f, maps.unique(), maps.nonUnique());
    }

    protected abstract Map<String, IOpenField> fieldMap();

    @Override
    public Collection<IOpenField> getFields() {
        var fields = new ArrayList<IOpenField>();
        Iterable<IOpenClass> superClasses = superClasses();
        for (IOpenClass superClass : superClasses) {
            fields.addAll(superClass.getFields());
        }
        fields.addAll(fieldMap().values());
        return fields;
    }

    @Override
    public Collection<IOpenField> getDeclaredFields() {
        return Collections.unmodifiableCollection(fieldMap().values());
    }

    public static IOpenClass getArrayType(IOpenClass openClass, int dim) {
        if (dim > 0) {
            IOpenClass arrayType = ComponentTypeArrayOpenClass.createComponentTypeArrayOpenClass(openClass, dim);
            if (openClass.getDomain() != null) {
                var domainOpenClassName = new StringBuilder(openClass.getName());
                for (var j = 0; j < dim; j++) {
                    domainOpenClassName.append("[]");
                }
                return new DomainOpenClass(domainOpenClassName.toString(),
                        arrayType,
                        openClass.getDomain(),
                        openClass instanceof BelongsToModuleOpenClass btmoc ? btmoc.getModule()
                                : null,
                        null);
            } else {
                return arrayType;
            }
        }
        throw new IllegalArgumentException("Expected positive number for array dimension");
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
        } catch (AmbiguousFieldException e) {
            log.debug("Ignored error: ", e);
            return null;
        }
    }

    @Override
    public IOpenField getField(String fname, boolean strictMatch) throws AmbiguousFieldException {

        IOpenField f;
        if (strictMatch) {

            Map<String, IOpenField> m = fieldMap();

            f = m == null ? null : m.get(fname);
            if (f != null) {
                return f;
            } else {
                return searchFieldFromSuperClass(fname, strictMatch);
            }
        }

        var lfname = fname.toLowerCase();

        f = getUniqueLowerCaseFieldMap().get(lfname);
        if (f != null) {
            return f;
        }

        List<IOpenField> ff = getNonUniqueLowerCaseFieldMap().get(lfname);

        if (ff != null) {
            throw new AmbiguousFieldException(fname, ff);
        }

        return searchFieldFromSuperClass(fname, strictMatch);
    }

    protected IOpenField searchFieldFromSuperClass(String fname, boolean strictMatch) throws AmbiguousFieldException {
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
        var methodKey = new MethodKey(params);
        return m.get(methodKey);
    }

    @Override
    public IMetaInfo getMetaInfo() {
        return xlsMetaInfo;
    }

    @Override
    public IOpenMethod getMethod(String name, IOpenClass[] classes) {

        var method = getDeclaredMethod(name, classes);

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

    private Map<String, List<IOpenField>> getNonUniqueLowerCaseFieldMap() {
        return lowerCaseFieldMaps().nonUnique();
    }

    private Map<String, IOpenField> getUniqueLowerCaseFieldMap() {
        return lowerCaseFieldMaps().unique();
    }

    @Override
    public IOpenField getVar(String name, boolean strictMatch) throws AmbiguousFieldException {
        return getField(name, strictMatch);
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isAssignableFrom(IType type) {
        if (type instanceof IOpenClass class1) {
            return isAssignableFrom(class1);
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

    private LowerCaseFieldMaps lowerCaseFieldMaps() {
        var maps = lowerCaseFieldMaps.get();
        if (maps == null) {
            synchronized (this) {
                maps = lowerCaseFieldMaps.get();
                if (maps == null) {
                    var uniqueLCaseFieldMap = new HashMap<String, IOpenField>();
                    var nonUniqueLCaseFieldMap = new HashMap<String, List<IOpenField>>();
                    for (IOpenField field : getFields()) {
                        addFieldToLowerCaseMaps(field, uniqueLCaseFieldMap, nonUniqueLCaseFieldMap);
                    }
                    maps = new LowerCaseFieldMaps(uniqueLCaseFieldMap, nonUniqueLCaseFieldMap);
                    lowerCaseFieldMaps.set(maps);
                }
            }
        }
        return maps;
    }

    private final AtomicReference<Map<MethodKey, IOpenMethod>> methodMap = new AtomicReference<>();
    private final AtomicReference<Map<MethodKey, IOpenMethod>> constructorMap = new AtomicReference<>();

    private Map<MethodKey, IOpenMethod> methodMap() {
        var methods = methodMap.get();
        if (methods == null) {
            synchronized (this) {
                methods = methodMap.get();
                if (methods == null) {
                    methods = initMethodMap();
                    methodMap.set(methods);
                }
            }
        }
        return methods;
    }

    private Map<MethodKey, IOpenMethod> constructorMap() {
        var constructors = constructorMap.get();
        if (constructors == null) {
            synchronized (this) {
                constructors = constructorMap.get();
                if (constructors == null) {
                    constructors = initConstructorMap();
                    constructorMap.set(constructors);
                }
            }
        }
        return constructors;
    }

    protected Map<MethodKey, IOpenMethod> initMethodMap() {
        return STUB;
    }

    protected Map<MethodKey, IOpenMethod> initConstructorMap() {
        return STUB;
    }

    private IOpenMethod putMethod(IOpenMethod method) {
        var methods = methodMap.get();
        if (methods == null || methods == STUB) {
            synchronized (this) {
                methods = methodMap.get();
                if (methods == null) {
                    methods = initMethodMap();
                }
                if (methods == STUB) {
                    methods = HashMap.newHashMap(4);
                }
                methodMap.set(methods);
            }
        }
        var key = new MethodKey(method);
        return methods.put(key, method);
    }

    protected void removeMethod(IOpenMethod method) {
        var methods = methodMap.get();
        if (methods != null) {
            var key = new MethodKey(method);
            methods.remove(key);
            invalidateInternalData();
        }
    }

    public void addMethod(IOpenMethod method) throws DuplicatedMethodException {
        final var existMethod = putMethod(method);
        if (existMethod != null) {
            throw new DuplicatedMethodException("Method '%s' is already defined in class '%s'"
                    .formatted(method, getName()), existMethod, method);
        }
        invalidateInternalData();
    }

    protected void invalidateInternalData() {
        allMethodsCacheInvalidated = true;
        allMethodNamesMapInvalidated = true;
        allConstructorNamesMapInvalidated = true;
        constructorMap.set(null);
    }

    private Collection<IOpenMethod> allMethodsCache;
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
        var methods = new HashMap<MethodKey, IOpenMethod>();
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
            return List.of();
        }
        return Collections.unmodifiableCollection(methods.values());
    }

    public IOpenMethod getDeclaredMethod(String name, IOpenClass[] classes) {
        Map<MethodKey, IOpenMethod> m = methodMap();
        var methodKey = new MethodKey(name, classes);
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
        this.indexField = field;
    }

    @Override
    public void setMetaInfo(IMetaInfo metaInfo) {
        this.xlsMetaInfo = metaInfo;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Default implementation.
     *
     * @param type IOpenClass instance
     */
    @Override
    public void addType(IOpenClass type) {
    }

    @Override
    public IOpenClass findType(String name) {
        return null;
    }

    /**
     * Default implementation. Always returns <code>null</code>.
     */
    @Override
    public Collection<IOpenClass> getTypes() {
        // Default implementation.
        // To do nothing. Not everyone has internal types.
        return List.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var that = (AOpenClass) o;
        if (getInstanceClass() != null || that.getInstanceClass() != null) {
            return Objects.equals(getInstanceClass(), that.getInstanceClass());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = getInstanceClass() != null ? getInstanceClass().hashCode() : 0;
        result = 31 * result + getName().hashCode();
        return result;
    }

    private Map<String, List<IOpenMethod>> allMethodNamesMap;

    private volatile boolean allMethodNamesMapInvalidated = true;

    private Collection<IOpenMethod> allConstructors;

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
        return found == null ? List.of() : Collections.unmodifiableList(found);
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
        return allConstructors == null ? List.of() : allConstructors;
    }

    public static Map<String, List<IOpenMethod>> buildMethodNameMap(Iterable<IOpenMethod> methods) {
        var res = new HashMap<String, List<IOpenMethod>>();

        for (IOpenMethod m : methods) {
            var name = m.getName();
            var list = res.computeIfAbsent(name, e -> new LinkedList<>());
            list.add(m);
        }

        return res;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public IOpenClass toStaticClass() {
        var staticClass = staticOpenClass.get();
        if (staticClass == null) {
            synchronized (this) {
                staticClass = staticOpenClass.get();
                if (staticClass == null) {
                    staticClass = new StaticOpenClass(this);
                    staticOpenClass.set(staticClass);
                }
            }
        }
        return staticClass;
    }

    @Override
    public IOpenField getStaticField(String fname) {
        return null;
    }

    @Override
    public Collection<IOpenField> getStaticFields() {
        return null;
    }

    @Override
    public IOpenField getStaticField(String name, boolean strictMatch) {
        return null;
    }

    @Override
    public boolean isStatic() {
        return false;
    }
}
