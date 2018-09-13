package org.openl.types.impl;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    public final static ComponentTypeArrayOpenClass createComponentTypeArrayOpenClass(IOpenClass componentClass,
                                                                                      int dimensions) {
        ComponentTypeArrayOpenClass componentTypeArrayOpenClass = null;
        for (int i = 0; i <= dimensions; i++) {
            componentTypeArrayOpenClass = new ComponentTypeArrayOpenClass(componentClass);
        }
        return componentTypeArrayOpenClass;
    }

    public ComponentTypeArrayOpenClass(IOpenClass componentClass) {
        IOpenField lengthOpenField = new ComponentTypeArrayLengthOpenField();
        this.componentClass = componentClass;
        this.fieldMap = new HashMap<>(1);
        this.fieldMap.put(lengthOpenField.getName(), lengthOpenField);
    }

    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    public Object newInstance(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

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

    public String getDisplayName(int mode) {
        return componentClass.getDisplayName(mode) + "[]";
    }

    public Class<?> getInstanceClass() {
        if (componentClass.getInstanceClass() != null) {
            return JavaOpenClass.makeArrayClass(componentClass.getInstanceClass());
        } else {
            return null;
        }
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        return getInstanceClass().isAssignableFrom(ioc.getInstanceClass());
    }

    @Override
    public boolean isAssignableFrom(Class<?> c) {
        return getInstanceClass().isAssignableFrom(c);
    }

    @Override
    public boolean isInstance(Object instance) {
        return getInstanceClass().isInstance(instance);
    }

    public String getName() {
        return componentClass.getName() + "[]";
    }

    @Override
    public String getJavaName() {
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
}
