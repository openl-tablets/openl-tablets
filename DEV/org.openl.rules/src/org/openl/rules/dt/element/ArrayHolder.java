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

    public Object invoke(Object target, Object[] dtParams, IRuntimeEnv env) {
        if (values2 != null) {
            Object res = componentType.getAggregateInfo().makeIndexedAggregate(componentType, values2.length);
            for (int i = 0; i < values2.length; i++) {
                if (values2[i] != null) {
                    Object array = componentType.getAggregateInfo()
                        .makeIndexedAggregate(componentType.getComponentClass(), values2[i].length);
                    for (int j = 0; j < values2[i].length; j++) {
                        if (values2[i][j] instanceof CompositeMethod) {
                            CompositeMethod compositeMethod = (CompositeMethod) values2[i][j];
                            Object result = compositeMethod.invoke(target, dtParams, env);
                            Array.set(array, j, result);
                        } else {
                            Array.set(array,
                                j,
                                values2[i][j] == null ? componentType.getComponentClass().nullObject() : values2[i][j]);
                        }
                    }
                    Array.set(res, i, array);
                }
            }
            return res;
        } else {
            Object res = componentType.getAggregateInfo().makeIndexedAggregate(componentType, values1.length);
            for (int i = 0; i < values1.length; i++) {
                if (values1[i] instanceof CompositeMethod) {
                    CompositeMethod compositeMethod = (CompositeMethod) values1[i];
                    Object result = compositeMethod.invoke(target, dtParams, env);
                    Array.set(res, i, result);
                } else {
                    Array.set(res, i, values1[i] == null ? componentType.nullObject() : values1[i]);
                }
            }
            return res;
        }
    }

    public void updateDependency(BindingDependencies dependencies) {
        if (values2 != null) {
            for (Object[] array : values2) {
                for (Object method : array) {
                    if (method instanceof CompositeMethod) {
                        ((CompositeMethod) method).updateDependency(dependencies);
                    }
                }
            }
        } else {
            for (Object method : values1) {
                if (method instanceof CompositeMethod) {
                    ((CompositeMethod) method).updateDependency(dependencies);
                }
            }
        }
    }
}
