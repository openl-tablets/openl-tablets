package org.openl.rules.dt.element;

import java.lang.reflect.Array;

import org.openl.binding.BindingDependencies;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public class ArrayHolder {

    private Object array;
    private CompositeMethod[] methods;

    public ArrayHolder(Object array, CompositeMethod[] methods) {
        this.array = array;
        this.methods = methods;
    }

    public Object invoke(Object target, Object[] dtParams, IRuntimeEnv env) {
        
    	Object[] res = new Object[Array.getLength(array)];
    	
        for (int i = 0; i < methods.length; i++) {
            
            CompositeMethod compositeMethod = methods[i];
            
            if (compositeMethod != null) {
                Object result = compositeMethod.invoke(target, dtParams, env);
                Array.set(res, i, result);
            }
        }

        return array;
    }

	public void updateDependency(BindingDependencies dependencies) {
		for (int i = 0; i < methods.length; i++) {
			if (methods[i] != null)
				methods[i].updateDependency(dependencies);
		}
	}
}
