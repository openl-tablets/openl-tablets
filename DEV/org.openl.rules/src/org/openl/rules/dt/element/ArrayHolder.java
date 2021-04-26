package org.openl.rules.dt.element;

import java.lang.reflect.Array;

import org.openl.binding.BindingDependencies;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public interface ArrayHolder {

    Object invoke(Object target, Object[] dtParams, IRuntimeEnv env);

    void updateDependency(BindingDependencies dependencies);
}
