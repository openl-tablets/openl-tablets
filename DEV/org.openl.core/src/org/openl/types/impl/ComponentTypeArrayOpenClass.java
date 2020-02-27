package org.openl.types.impl;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.meta.IMetaInfo;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ComponentTypeArrayOpenClass extends AOpenClass {

    protected IOpenClass componentClass;
    protected HashMap<String, IOpenField> fieldMap;
    protected IOpenIndex index;
    private final String javaName;

    public static ComponentTypeArrayOpenClass createComponentTypeArrayOpenClass(IOpenClass componentClass,
            int dim) {
        ComponentTypeArrayOpenClass componentTypeArrayOpenClass = null;
        if (dim > 0) {
            componentTypeArrayOpenClass = new ComponentTypeArrayOpenClass(componentClass);
        }
        for (int i = 0; i < dim - 1; i++) {
            componentTypeArrayOpenClass = new ComponentTypeArrayOpenClass(componentTypeArrayOpenClass);
        }
        return componentTypeArrayOpenClass;
    }

    public ComponentTypeArrayOpenClass(IOpenClass componentClass) {
        IOpenField lengthOpenField = new ComponentTypeArrayLengthOpenField();
        this.componentClass = componentClass;
        this.fieldMap = new HashMap<>(1);
        this.fieldMap.put(lengthOpenField.getName(), lengthOpenField);
        this.javaName = createJavaName(componentClass);
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<IOpenClass> superClasses() {
        return Collections.emptyList();
    }

    @Override
    protected Map<String, IOpenField> fieldMap() {
        return fieldMap;
    }

    @Override
    public IOpenClass getComponentClass() {
        return componentClass;
    }

    @Override
    public String getDisplayName(int mode) {
        return componentClass.getDisplayName(mode) + "[]";
    }

    @Override
    public Class<?> getInstanceClass() {
        if (componentClass.getInstanceClass() != null) {
            return JavaOpenClass.makeArrayClass(componentClass.getInstanceClass());
        } else {
            return null;
        }
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        if (ioc == null || ioc.getInstanceClass() == null) {
            return false;
        }
        if (ioc.isArray()) {
            return getComponentClass().isAssignableFrom(ioc.getComponentClass());
        }
        return getInstanceClass().isAssignableFrom(ioc.getInstanceClass());
    }

    @Override
    public boolean isInstance(Object instance) {
        return getInstanceClass().isInstance(instance);
    }

    @Override
    public String getName() {
        return componentClass.getName() + "[]";
    }

    @Override
    public String getJavaName() {
        return javaName;
    }

    private String createJavaName(IOpenClass componentClass) {
        String componentName = componentClass.getJavaName();
        if (componentName.charAt(0) == '[') {
            return '[' + componentName;
        } else {
            return "[L" + componentName + ';';
        }
    }

    @Override
    public String getPackageName() {
        return componentClass.getPackageName();
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public IMetaInfo getMetaInfo() {
        return componentClass.getMetaInfo();
    }

    static class ComponentTypeArrayLengthOpenField extends ArrayLengthOpenField {

        @Override
        public int getLength(Object target) {
            if (target == null) {
                return 0;
            }
            return Array.getLength(target);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ComponentTypeArrayOpenClass that = (ComponentTypeArrayOpenClass) o;
        return Objects.equals(componentClass, that.componentClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaName);
    }

    @Override
    public String toString() {
        return javaName;
    }
}
