/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.types;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.openl.rules.lang.xls.XlsBinder;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DatatypeOpenConstructor;
import org.openl.types.impl.DatatypeOpenField;
import org.openl.types.impl.DatatypeOpenMethod;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.impl.MethodKey;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Open class for types represented as datatype table components in openl.
 *
 * @author snshor
 */
public class DatatypeOpenClass extends ADynamicClass {

    private final Logger log = LoggerFactory.getLogger(DatatypeOpenClass.class);

    private IOpenClass superClass;

    private final String javaName;

    private final String packageName;
    private byte[] bytecode;

    /**
     * User has a possibility to set the package (by table properties mechanism) where he wants to generate datatype
     * beans classes.
     */
    public DatatypeOpenClass(String name, String packageName) {
        // NOTE! The instance class during the construction is null.
        // It will be set after the generating the appropriate byte code for the
        // datatype.
        // See {@link
        // org.openl.rules.datatype.binding.DatatypeTableBoundNode.addFields()}
        //
        // @author Denis Levchuk
        //
        super(name, null);
        if (StringUtils.isBlank(packageName)) {
            javaName = name;
        } else {
            javaName = packageName + '.' + name;
        }
        this.packageName = packageName;
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    public IOpenClass getSuperClass() {
        return superClass;
    }

    public void setSuperClass(IOpenClass superClass) {
        this.superClass = superClass;
    }

    @Override
    public Iterable<IOpenClass> superClasses() {
        if (superClass != null) {
            return Collections.singletonList(superClass);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getJavaName() {
        return javaName;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    /**
     * Used {@link LinkedHashMap} to store fields in order as them defined in DataType table
     */
    @Override
    protected LinkedHashMap<String, IOpenField> fieldMap() {
        if (fieldMap == null) {
            fieldMap = new LinkedHashMap<>();
        }
        return (LinkedHashMap<String, IOpenField>) fieldMap;
    }

    private volatile Map<String, IOpenField> fields = null;

    @Override
    public Map<String, IOpenField> getFields() {
        Map<String, IOpenField> localFields = this.fields;
        if (localFields == null) {
            synchronized (this) {
                localFields = this.fields;
                if (localFields == null) {
                    fields = localFields = initializeFields();
                }
            }
        }
        return Collections.unmodifiableMap(localFields);
    }

    private Map<String, IOpenField> initializeFields() {
        Map<String, IOpenField> fields = new LinkedHashMap<>();
        Iterable<IOpenClass> superClasses = superClasses();
        for (IOpenClass superClass : superClasses) {
            fields.putAll(superClass.getFields());
        }
        fields.putAll(fieldMap());
        return fields;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, IOpenField> getDeclaredFields() {
        return (Map<String, IOpenField>) fieldMap().clone();
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        Object instance = null;
        try {
            instance = getInstanceClass().newInstance();
        } catch (Exception e) {
            log.error("{}", this, e);
        }
        return instance;
    }

    @Override
    public IOpenClass getComponentClass() {
        if (isArray()) {
            return JavaOpenClass.getOpenClass(getInstanceClass().getComponentType());
        }
        return null;
    }

    /**
     * Override super class implementation to provide possibility to compare datatypes with info about their fields
     *
     * @author DLiauchuk
     */
    @Override
    public int hashCode() {
        return Objects.hash(superClass, javaName);
    }

    /**
     * Override super class implementation to provide possibility to compare datatypes with info about their fields Is
     * used in {@link XlsBinder} (method filterDependencyTypes)
     *
     * @author DLiauchuk
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DatatypeOpenClass other = (DatatypeOpenClass) obj;

        return Objects.equals(superClass, other.getSuperClass()) && Objects.equals(getMetaInfo(),
            other.getMetaInfo()) && Objects.equals(javaName, other.getJavaName());
    }

    @Override
    public String toString() {
        return javaName;
    }

    private IOpenMethod wrapDatatypeOpenMethod(IOpenMethod method) {
        if (method instanceof JavaOpenMethod) {
            JavaOpenMethod javaOpenMethod = (JavaOpenMethod) method;
            Method javaMethod = javaOpenMethod.getJavaMethod();
            for (IOpenField field : fieldMap().values()) {
                if (field instanceof DatatypeOpenField) {
                    DatatypeOpenField datatypeOpenField = (DatatypeOpenField) field;
                    if (datatypeOpenField.getGetter().equals(javaMethod)) {
                        return new DatatypeOpenMethod(javaOpenMethod,
                            this,
                            javaOpenMethod.getParameterTypes(),
                            field.getType());
                    }
                    if (datatypeOpenField.getSetter().equals(javaMethod)) {
                        IOpenClass[] parameterTypes = new IOpenClass[] { field.getType() };
                        return new DatatypeOpenMethod(javaOpenMethod, this, parameterTypes, javaOpenMethod.getType());
                    }
                }
            }
        }
        return method;
    }

    @Override
    protected Map<MethodKey, IOpenMethod> initMethodMap() {
        Map<MethodKey, IOpenMethod> methods = super.initMethodMap();
        Map<MethodKey, IOpenMethod> methodMap = new HashMap<>(OBJECT_CLASS_METHODS);

        for (Entry<MethodKey, IOpenMethod> m : methods.entrySet()) {
            IOpenMethod m1 = wrapDatatypeOpenMethod(m.getValue());
            if (m1 != m.getValue()) {
                methodMap.put(new MethodKey(m1), m1);
            } else {
                methodMap.put(m.getKey(), m.getValue());
            }
        }

        return methodMap;
    }

    @Override
    protected Map<MethodKey, IOpenMethod> initConstructorMap() {
        Map<MethodKey, IOpenMethod> constructors = super.initConstructorMap();
        Map<MethodKey, IOpenMethod> constructorMap = new HashMap<>(1);
        for (Entry<MethodKey, IOpenMethod> constructor : constructors.entrySet()) {
            IOpenMethod wrapped = wrapDatatypeOpenConstructor(constructor.getKey(), constructor.getValue());
            if (wrapped == constructor.getValue()) {
                constructorMap.put(constructor.getKey(), constructor.getValue());
            } else {
                constructorMap.put(new MethodKey(wrapped), wrapped);
            }
        }
        return constructorMap;
    }

    private IOpenMethod wrapDatatypeOpenConstructor(MethodKey mk, IOpenMethod method) {
        if (method instanceof JavaOpenConstructor && javaName.equals(method.getDeclaringClass().getJavaName())) {
            JavaOpenConstructor javaOpenConstructor = (JavaOpenConstructor) method;
            if (javaOpenConstructor.getNumberOfParameters() == 0) {
                return new DatatypeOpenConstructor(javaOpenConstructor, this);
            } else {
                MethodKey candidate = new MethodKey(getFields().values().stream()
                    .map(IOpenMember::getType)
                    .toArray(IOpenClass[]::new));
                if (mk.equals(candidate)) {
                    ParameterDeclaration[] parameters = getFields().values().stream()
                        .map(f -> new ParameterDeclaration(f.getType(), f.getName()))
                        .toArray(ParameterDeclaration[]::new);
                    return new DatatypeOpenConstructor(javaOpenConstructor, this, parameters);
                }
            }
        }
        return method;
    }

    public byte[] getBytecode() {
        return bytecode;
    }

    public void setBytecode(byte[] bytecode) {
        this.bytecode = bytecode;
    }

    private static final Map<MethodKey, IOpenMethod> OBJECT_CLASS_METHODS;

    static {
        Map<MethodKey, IOpenMethod> objectClassMethods = new HashMap<>();
        for (IOpenMethod m : JavaOpenClass.OBJECT.getMethods()) {
            objectClassMethods.put(new MethodKey(m), m);
        }
        OBJECT_CLASS_METHODS = Collections.unmodifiableMap(objectClassMethods);
    }

}
