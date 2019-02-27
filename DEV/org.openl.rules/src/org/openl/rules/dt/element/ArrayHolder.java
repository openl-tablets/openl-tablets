package org.openl.rules.dt.element;

import java.lang.reflect.Array;

import org.openl.binding.BindingDependencies;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public class ArrayHolder {

    private CompositeMethod[] methods;
    private IOpenClass componentType;

    public ArrayHolder(IOpenClass componentType, CompositeMethod[] methods) {
        this.methods = methods;
        this.componentType = componentType;
    }

    public Object invoke(Object target, Object[] dtParams, IRuntimeEnv env) {

        Object res = componentType.getAggregateInfo().makeIndexedAggregate(componentType, methods.length);

        for (int i = 0; i < methods.length; i++) {

            CompositeMethod compositeMethod = methods[i];

            if (compositeMethod != null) {
                Object result = compositeMethod.invoke(target, dtParams, env);
                Array.set(res, i, result);
            }
        }

        return res;
    }

    public void updateDependency(BindingDependencies dependencies) {
        for (CompositeMethod method : methods) {
            if (method != null) {
                method.updateDependency(dependencies);
            }
        }
    }
}
