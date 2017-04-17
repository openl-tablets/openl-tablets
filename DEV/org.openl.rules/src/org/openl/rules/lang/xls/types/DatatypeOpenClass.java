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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.impl.MethodKey;
import org.openl.types.impl.MethodSignature;
import org.openl.types.java.JavaOpenClass;
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

    /**
     * see {@link #getPackageName()}
     */
    private String packageName;

    public DatatypeOpenClass(String name, String packageName) {
        // NOTE! The instance class during the construction is null.
        // It will be set after the generating the appropriate byte code for the datatype.
        // See {@link org.openl.rules.datatype.binding.DatatypeTableBoundNode.addFields()}
        //
        // @author Denis Levchuk
        //
        super(name, null);
        this.packageName = packageName;
    }
    
    private void addMethodToMap(Map<MethodKey, IOpenMethod> methodMap, IOpenMethod method){
        MethodKey key = new MethodKey(method);
        methodMap.put(key, method);
    }
    
    @Override
    protected Map<MethodKey, IOpenMethod> initMethodMap() {
        Map<MethodKey, IOpenMethod> methodMap = super.initMethodMap();
        if (methodMap == STUB){
            methodMap = new HashMap<MethodKey, IOpenMethod>(4);
        }
        addMethodToMap(methodMap, new EqualsMethod(this));
        addMethodToMap(methodMap, new HashCodeMethod(this));
        addMethodToMap(methodMap, new ToStringMethod(this));
        
        return methodMap;
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

    /**
     * User has a possibility to set the package (by table properties mechanism) where he wants to generate datatype
     * beans classes. It is stored in this field.
     *
     * @return package name for current datatype.
     */
    public String getPackageName() {
        return packageName;
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
                    fields = new LinkedHashMap<String, IOpenField>();
                    Iterable<IOpenClass> superClasses = superClasses();
                    for(IOpenClass superClass : superClasses) {
                        fields.putAll(superClass.getFields());
                    }
                    fields.putAll(fieldMap());
                }
            }
        }
        return Collections.unmodifiableMap(fields);
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
        return new HashCodeBuilder().append(superClass).append(getMetaInfo()).append(packageName).toHashCode();
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

        return new EqualsBuilder().append(superClass, other.getSuperClass()).append(getMetaInfo(), other.getMetaInfo()).append(packageName, other.getPackageName()).isEquals();
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
            IOpenClass[] params = new IOpenClass[fields.size()];
            String[] names = new String[fields.size()];
            int i = 0;
            for (Entry<String, IOpenField> field : fields.entrySet()) {
                params[i] = field.getValue().getType();
                names[i] = field.getKey();
                i++;
            }
            return new MethodSignature(params, names);
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

    /**
     * <code>toString()</code> method.
     *
     * @author PUdalau
     */
    public static class ToStringMethod implements IOpenMethod {
        private IOpenClass openClass;

        public ToStringMethod(IOpenClass forClass) {
            this.openClass = forClass;
        }

        public IMethodSignature getSignature() {
            return IMethodSignature.VOID;
        }

        public IOpenClass getDeclaringClass() {
            return openClass;
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenClass getType() {
            return JavaOpenClass.STRING;
        }

        public boolean isStatic() {
            return false;
        }

        public String getDisplayName(int mode) {
            return getName();
        }

        public String getName() {
            return "toString";
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            StringBuilder builder = new StringBuilder(openClass.getDisplayName(0) + "{ ");
            Map<String, IOpenField> fields = openClass.getFields();
            for (Entry<String, IOpenField> field : fields.entrySet()) {
                builder.append(field.getKey()).append("=").append(field.getValue().get(target, env)).append(" ");
            }
            builder.append('}');
            return builder.toString();
        }
        
        @Override
        public boolean isConstructor() {
            return false;
        }
    }

    /**
     * Method that compares two objects by fields defined in some {@link IOpenClass}
     *
     * @author PUdalau
     */
    public static class EqualsMethod implements IOpenMethod {
        private IOpenClass openClass;

        public EqualsMethod(IOpenClass forClass) {
            this.openClass = forClass;
        }

        public IMethodSignature getSignature() {
            return new MethodSignature(new IOpenClass[]{JavaOpenClass.OBJECT});
        }

        public IOpenClass getDeclaringClass() {
            return openClass;
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenClass getType() {
            return JavaOpenClass.BOOLEAN;
        }

        public boolean isStatic() {
            return false;
        }

        public String getDisplayName(int mode) {
            return getName();
        }

        public String getName() {
            return "equals";
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            // TODO: remove usage of the EqualsBuilder
            //
            EqualsBuilder builder = new EqualsBuilder();
            Map<String, IOpenField> fields = openClass.getFields();
            for (IOpenField field : fields.values()) {
                builder.append(field.get(target, env), field.get(params[0], env));
            }
            return builder.isEquals();
        }
        
        @Override
        public boolean isConstructor() {
            return false;
        }

    }

    /**
     * Methods that returns hash code calculated using fields.
     *
     * @author PUdalau
     */
    public static class HashCodeMethod implements IOpenMethod {
        private IOpenClass openClass;

        public HashCodeMethod(IOpenClass forClass) {
            this.openClass = forClass;
        }

        public IMethodSignature getSignature() {
            return IMethodSignature.VOID;
        }

        public IOpenClass getDeclaringClass() {
            return openClass;
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenClass getType() {
            return JavaOpenClass.INT;
        }

        public boolean isStatic() {
            return false;
        }

        public String getDisplayName(int mode) {
            return getName();
        }

        public String getName() {
            return "hashCode";
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            int result = 0;
            Map<String, IOpenField> fields = openClass.getFields();

            for (IOpenField field : fields.values()) {
                result = 31 * result + (field != null ? field.hashCode() : 0);
            }

            return result;
        }
        
        @Override
        public boolean isConstructor() {
            return false;
        }

    }
}
