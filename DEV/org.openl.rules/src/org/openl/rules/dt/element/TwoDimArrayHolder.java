package org.openl.rules.dt.element;

import java.lang.reflect.Array;

import org.openl.binding.BindingDependencies;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public class TwoDimArrayHolder implements ArrayHolder {
    private final Object[][] values;
    private final IOpenClass componentType;

    public TwoDimArrayHolder(IOpenClass componentType, Object[][] values) {
        this.values = values;
        if (!componentType.isArray()) {
            throw new IllegalStateException("Expected an array component type");
        }
        this.componentType = componentType;
    }

    public Object invoke(Object target, Object[] dtParams, IRuntimeEnv env) {
        Object res = componentType.getAggregateInfo().makeIndexedAggregate(componentType, values.length);
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                Object array = componentType.getAggregateInfo()
                    .makeIndexedAggregate(componentType.getComponentClass(), values[i].length);
                for (int j = 0; j < values[i].length; j++) {
                    if (values[i][j] instanceof CompositeMethod) {
                        CompositeMethod compositeMethod = (CompositeMethod) values[i][j];
                        Object result = compositeMethod.invoke(target, dtParams, env);
                        Array.set(array, j, result);
                    } else {
                        Array.set(array,
                            j,
                            values[i][j] == null ? componentType.getComponentClass().nullObject() : values[i][j]);
                    }
                }
                Array.set(res, i, array);
            }
        }
        return res;
    }

    public void updateDependency(BindingDependencies dependencies) {
        for (Object[] array : values) {
            for (Object method : array) {
                if (method instanceof CompositeMethod) {
                    ((CompositeMethod) method).updateDependency(dependencies);
                }
            }
        }
    }
}
