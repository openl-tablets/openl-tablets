/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.types;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.openl.base.INamedThing;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.impl.module.WrapModuleSpecificTypes;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.BelongsToModuleOpenClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.impl.MethodKey;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Open class for types represented as datatype table components in openl.
 *
 * @author snshor
 */
@Slf4j
public class DatatypeOpenClass extends ADynamicClass implements BelongsToModuleOpenClass, WrapModuleSpecificTypes {


    @Getter
    @Setter
    private IOpenClass superClass;

    @Getter
    private final String javaName;

    @Getter
    private final String packageName;

    @Getter
    @Setter
    private TableSyntaxNode tableSyntaxNode;

    @Getter
    @Setter
    private byte[] bytecode;

    @Getter
    @Setter
    private XlsModuleOpenClass module;

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
        // FIXME: instance class have to be defined to prevent multiple NPEs in CastFactory
        super(name, null);
        if (StringUtils.isBlank(packageName)) {
            javaName = name;
        } else {
            javaName = packageName + '.' + name;
        }
        this.packageName = packageName;
    }

    @Override
    public String getExternalRefName() {
        if (module == null) {
            throw new IllegalStateException("moduleName is not defined");
        }
        return "`" + module.getModuleName() + "`." + getName();
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    @Override
    public Collection<IOpenClass> superClasses() {
        if (superClass != null) {
            return List.of(superClass);
        } else {
            return List.of();
        }
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

    private volatile Map<String, IOpenField> fields;
    private volatile Map<String, IOpenField> staticFields;

    @Override
    public Collection<IOpenField> getFields() {
        ensureFieldsInitialized();
        return Collections.unmodifiableCollection(this.fields.values());
    }

    private void ensureFieldsInitialized() {
        if (this.fields == null || this.staticFields == null) {
            synchronized (this) {
                if (this.fields == null || this.staticFields == null) {
                    initializeFields();
                }
            }
        }
    }

    private void initializeFields() {
        var fields = new LinkedHashMap<String, IOpenField>();
        var staticFields = new LinkedHashMap<String, IOpenField>();
        Iterable<IOpenClass> superClasses = superClasses();
        for (IOpenClass superClassValue : superClasses) {
            for (IOpenField field : superClassValue.getFields()) {
                fields.put(field.getName(), field);
            }
        }
        fieldMap().forEach(fields::putIfAbsent);
        staticFields.put("class", new JavaOpenClass.JavaClassClassField(instanceClass, this));
        this.fields = fields;
        this.staticFields = staticFields;
        Optional.ofNullable(superClass).map(IOpenClass::getIndexField).ifPresent(this::setIndexField);
    }

    @Override
    public void addField(IOpenField field) throws DuplicatedFieldException {
        this.fields = null;
        super.addField(field);
        invalidateInternalData();
    }

    @Override
    public Collection<IOpenField> getDeclaredFields() {
        return Collections.unmodifiableCollection(fieldMap().values());
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        Object instance = null;
        try {
            instance = getInstanceClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("{}", this, e);
        }
        return instance;
    }

    @Override
    public IOpenClass getComponentClass() {
        return null;
    }

    @Override
    public String toString() {
        return javaName;
    }

    private IOpenMethod wrapDatatypeOpenMethod(IOpenMethod method) {
        if (method instanceof JavaOpenMethod javaOpenMethod) {
            var javaMethod = javaOpenMethod.getJavaMethod();
            for (IOpenField field : fieldMap().values()) {
                if (field instanceof DatatypeOpenField datatypeOpenField) {
                    if (Objects.equals(datatypeOpenField.getGetter(), javaMethod)) {
                        return new DatatypeOpenMethod(javaOpenMethod,
                                this,
                                javaOpenMethod.getParameterTypes(),
                                field.getType());
                    }
                    if (Objects.equals(datatypeOpenField.getSetter(), javaMethod)) {
                        IOpenClass[] parameterTypes = new IOpenClass[]{field.getType()};
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
        var methodMap = new HashMap<MethodKey, IOpenMethod>(OBJECT_CLASS_METHODS);

        for (Entry<MethodKey, IOpenMethod> m : methods.entrySet()) {
            var m1 = wrapDatatypeOpenMethod(m.getValue());
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
        var constructorMap = new HashMap<MethodKey, IOpenMethod>(1);
        for (Entry<MethodKey, IOpenMethod> constructor : constructors.entrySet()) {
            var wrapped = wrapDatatypeOpenConstructor(constructor.getKey(), constructor.getValue());
            if (wrapped == constructor.getValue()) {
                constructorMap.put(constructor.getKey(), constructor.getValue());
            } else {
                constructorMap.put(new MethodKey(wrapped), wrapped);
            }
        }
        return constructorMap;
    }

    private IOpenMethod wrapDatatypeOpenConstructor(MethodKey mk, IOpenMethod method) {
        if (method instanceof JavaOpenConstructor javaOpenConstructor) {
            if (javaOpenConstructor.getNumberOfParameters() == 0) {
                return new DatatypeOpenConstructor(javaOpenConstructor, this);
            } else {
                var candidate = new MethodKey(
                        getFields().stream().map(IOpenMember::getType).toArray(IOpenClass[]::new));
                if (mk.equals(candidate)) {
                    var parameters = getFields().stream()
                            .map(f -> new ParameterDeclaration(f.getType(), f.getName()))
                            .toArray(ParameterDeclaration[]::new);
                    return new DatatypeOpenConstructor(javaOpenConstructor, this, parameters);
                }
            }
        }
        return method;
    }

    @Override
    public String getDisplayName(int mode) {
        if (mode == INamedThing.LONG) {
            return getPackageName() + "." + getName();
        }
        return getName();
    }

    private static final Map<MethodKey, IOpenMethod> OBJECT_CLASS_METHODS;

    static {
        var objectClassMethods = new HashMap<MethodKey, IOpenMethod>();
        for (IOpenMethod m : JavaOpenClass.OBJECT.getMethods()) {
            objectClassMethods.put(new MethodKey(m), m);
        }
        OBJECT_CLASS_METHODS = Collections.unmodifiableMap(objectClassMethods);
    }

    @Override
    public IOpenField getStaticField(String fname) {
        ensureFieldsInitialized();
        return staticFields.get(fname);
    }

    @Override
    public Collection<IOpenField> getStaticFields() {
        ensureFieldsInitialized();
        return staticFields.values();
    }

    @Override
    public IOpenField getStaticField(String name, boolean strictMatch) {
        ensureFieldsInitialized();
        Optional<String> first = staticFields.keySet().stream().filter(f -> f.equalsIgnoreCase(name)).findFirst();
        return first.map(s -> staticFields.get(s)).orElse(null);
    }

    @Override
    protected void invalidateInternalData() {
        super.invalidateInternalData();
        synchronized (this) {
            this.fields = null;
            this.staticFields = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        var that = (DatatypeOpenClass) o;

        return Objects.equals(module, that.module);
    }

    @Override
    public int hashCode() {
        var result = super.hashCode();
        result = 31 * result + (module != null ? module.hashCode() : 0);
        return result;
    }
}
