/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.openl.rules.lang.xls.XlsBinder;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.impl.MethodKey;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.RuntimeExceptionWrapper;
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

    /**
     * User has a possibility to set the package (by table properties mechanism) where he wants to generate datatype
     * beans classes.
     */
     public DatatypeOpenClass(String name, String packageName) {
        // NOTE! The instance class during the construction is null.
        // It will be set after the generating the appropriate byte code for the datatype.
        // See {@link org.openl.rules.datatype.binding.DatatypeTableBoundNode.addFields()}
        //
        // @author Denis Levchuk
        //
        super(name, null);
        if (StringUtils.isBlank(packageName)) {
            javaName = name;
        } else {
            javaName = packageName + '.' + name;
        }
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

    /**
     * Used {@link LinkedHashMap} to store fields in order as them defined in DataType table
     */
    @Override
    protected LinkedHashMap<String, IOpenField> fieldMap() {
        if (fieldMap == null) {
            fieldMap = new LinkedHashMap<String, IOpenField>();
        }
        return (LinkedHashMap<String, IOpenField>) fieldMap;
    }

    private Map<String, IOpenField> fields = null;
    
    @Override
    public Map<String, IOpenField> getFields() {
        if (fields == null){
            synchronized (this) {
                if (fields == null){
                    fields = initializeFields();
                }
            }
        }
        return Collections.unmodifiableMap(fields);
    }

    private Map<String, IOpenField> initializeFields() {
        Map<String, IOpenField> fields = new LinkedHashMap<String, IOpenField>();
        Iterable<IOpenClass> superClasses = superClasses();
        for(IOpenClass superClass : superClasses) {
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

    public Object newInstance(IRuntimeEnv env) {
        Object instance = null;
        try {
            instance = getInstanceClass().newInstance();
        } catch (InstantiationException e) {
            log.error("{}", this, e);
        } catch (IllegalAccessException e) {
            log.error("{}", this, e);
        } catch (Throwable e) {
            // catch e.g. NoClassDefFoundError
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
     * Override super class implementation to provide possibility to compare datatypes with info about their fields
     * Is used in {@link XlsBinder} (method filterDependencyTypes)
     *
     * @author DLiauchuk
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        DatatypeOpenClass other = (DatatypeOpenClass) obj;

        return Objects.equals(superClass, other.getSuperClass()) &&
                Objects.equals(getMetaInfo(), other.getMetaInfo()) &&
                Objects.equals(javaName, other.getJavaName());
    }

    @Override
    public String toString() {
        return javaName;
    }

    /**
     * Constructor with all parameters initialization.
     *
     * @author PUdalau
     */
    public static class OpenFieldsConstructor implements IOpenMethod {

        private IOpenClass openClass;

        public OpenFieldsConstructor(IOpenClass openClass) {
            this.openClass = openClass;
        }

        public IOpenClass getDeclaringClass() {
            return openClass;
        }

        public String getDisplayName(int mode) {
            return openClass.getDisplayName(mode);
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public String getName() {
            return openClass.getName();
        }

        public IMethodSignature getSignature() {
            Map<String, IOpenField> fields = openClass.getFields();
            IParameterDeclaration[] params = new IParameterDeclaration[fields.size()];
            int i = 0;
            for (Entry<String, IOpenField> field : fields.entrySet()) {
                params[i] = new ParameterDeclaration(field.getValue().getType(), field.getKey());
                i++;
            }
            return new MethodSignature(params);
        }

        public IOpenClass getType() {
            return openClass;
        }

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            Object result = openClass.newInstance(env);
            int i = 0;
            for (IOpenField field : openClass.getFields().values()) {
                field.set(result, params[i], env);
                i++;
            }
            return result;
        }

        public boolean isStatic() {
            return true;
        }
        
        @Override
        public boolean isConstructor() {
            return true;
        }

        @Override
        public String toString() {
            return openClass.getName();
        }

    }


    @Override
    protected Map<MethodKey, IOpenMethod> initMethodMap() {
        Map<MethodKey, IOpenMethod> methods = super.initMethodMap();
        if (methods == STUB){
            methods = new HashMap<MethodKey, IOpenMethod>(5);
        }
        methods.put(toStringKey, toString);
        methods.put(equalsKey, equals);
        methods.put(hashCodeKey, hashCode);

        return methods;
    }

    private static final IOpenMethod toString;
    private static final IOpenMethod equals;
    private static final IOpenMethod hashCode;
    private static final MethodKey toStringKey;
    private static final MethodKey equalsKey;
    private static final MethodKey hashCodeKey;
    static {
        try {
            toString = new JavaOpenMethod(Object.class.getMethod("toString"));
            equals = new JavaOpenMethod(Object.class.getMethod("equals", Object.class));
            hashCode = new JavaOpenMethod(Object.class.getMethod("hashCode"));
            toStringKey = new MethodKey(toString);
            equalsKey = new MethodKey(equals);
            hashCodeKey = new MethodKey(hashCode);
        } catch (NoSuchMethodException nsme) {
            throw RuntimeExceptionWrapper.wrap(nsme);
        }
    }
}
