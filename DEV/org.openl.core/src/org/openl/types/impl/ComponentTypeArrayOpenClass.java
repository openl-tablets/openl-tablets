package org.openl.types.impl;

import java.lang.reflect.Array;
import java.util.Collections;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ComponentTypeArrayOpenClass extends ArrayOpenClass {

    public final static ComponentTypeArrayOpenClass createComponentTypeArrayOpenClass(IOpenClass componentClass,
            int dimensions) {
        ComponentTypeArrayOpenClass componentTypeArrayOpenClass = null;
        for (int i = 0; i <= dimensions; i++) {
            componentTypeArrayOpenClass = new ComponentTypeArrayOpenClass(componentClass);
        }
        return componentTypeArrayOpenClass;
    }

    public ComponentTypeArrayOpenClass(IOpenClass componentClass) {
        super(componentClass, new ComponentTypeArrayLengthOpenField());
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
