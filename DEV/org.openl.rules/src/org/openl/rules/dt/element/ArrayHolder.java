package org.openl.rules.dt.element;

import java.lang.reflect.Array;

import org.openl.binding.BindingDependencies;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public class ArrayHolder {
    private final Object[] values1;
    private final Object[][] values2;
    private final IOpenClass componentType;

    public ArrayHolder(IOpenClass componentType, Object[] values) {
        this.values1 = values;
        this.values2 = null;
        this.componentType = componentType;
    }

    public ArrayHolder(IOpenClass componentType, Object[][] values) {
        if (!componentType.isArray()) {
            throw new IllegalStateException("Expected an array component type");
        }
        this.values1 = null;
        this.values2 = values;
        this.componentType = componentType;
    }

    public boolean is2DimArray() {
        return values2 != null;
    }

    public Object[][] get2DimValues() {
        return values2;
    }

    public Object[] getValues() {
        return values1;
    }

    public Object invoke(Object target, Object[] dtParams, IRuntimeEnv env) {
        if (values2 != null) {
            var res = componentType.getAggregateInfo().makeIndexedAggregate(componentType, values2.length);
            for (var i = 0; i < values2.length; i++) {
                if (values2[i] != null) {
                    var array = invokeValues(values2[i], componentType.getComponentClass(), target, dtParams, env);
                    Array.set(res, i, array);
                }
            }
            return res;
        } else {
            return invokeValues(values1, componentType, target, dtParams, env);
        }
    }

    /**
     * Builds an array of the given values, where each formula is replaced with its result and each empty value with
     * the empty value of the type.
     */
    private Object invokeValues(Object[] values,
                                IOpenClass valueType,
                                Object target,
                                Object[] dtParams,
                                IRuntimeEnv env) {
        var res = componentType.getAggregateInfo().makeIndexedAggregate(valueType, values.length);
        for (var i = 0; i < values.length; i++) {
            if (values[i] instanceof CompositeMethod compositeMethod) {
                var result = compositeMethod.invoke(target, dtParams, env);
                Array.set(res, i, result);
            } else {
                Array.set(res, i, values[i] == null ? valueType.nullObject() : values[i]);
            }
        }
        return res;
    }

    public void updateDependency(BindingDependencies dependencies) {
        if (values2 != null) {
            for (Object[] array : values2) {
                updateValuesDependency(array, dependencies);
            }
        } else {
            updateValuesDependency(values1, dependencies);
        }
    }

    private static void updateValuesDependency(Object[] array, BindingDependencies dependencies) {
        for (Object method : array) {
            if (method instanceof CompositeMethod compositeMethod) {
                compositeMethod.updateDependency(dependencies);
            }
        }
    }
}
