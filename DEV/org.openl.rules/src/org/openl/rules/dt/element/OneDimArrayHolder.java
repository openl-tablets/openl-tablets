package org.openl.rules.dt.element;

import java.lang.reflect.Array;

import org.openl.binding.BindingDependencies;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public class OneDimArrayHolder implements ArrayHolder {
    private final Object[] values;
    private final IOpenClass componentType;

    public OneDimArrayHolder(IOpenClass componentType, Object[] values) {
        this.values = values;
        this.componentType = componentType;
    }

    public Object invoke(Object target, Object[] dtParams, IRuntimeEnv env) {
        Object res = componentType.getAggregateInfo().makeIndexedAggregate(componentType, values.length);
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof CompositeMethod) {
                CompositeMethod compositeMethod = (CompositeMethod) values[i];
                Object result = compositeMethod.invoke(target, dtParams, env);
                Array.set(res, i, result);
            } else {
                Array.set(res, i, values[i] == null ? componentType.nullObject() : values[i]);
            }
        }
        return res;
    }

    public void updateDependency(BindingDependencies dependencies) {
        for (Object method : values) {
            if (method instanceof CompositeMethod) {
                ((CompositeMethod) method).updateDependency(dependencies);
            }
        }
    }
}
