package org.openl.rules.helpers;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.AutoCastFactory;
import org.openl.binding.impl.cast.AutoCastReturnType;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.method.AutoCastableResultOpenMethod;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;

public class RulesUtilsGetValuesAutoCastFactory implements AutoCastFactory {

    @Override
    public IMethodCaller build(IBindingContext bindingContext,
            IMethodCaller methodCaller,
            JavaOpenMethod javaOpenMethod,
            IOpenClass[] parameterTypes) {

        Method javaMethod = javaOpenMethod.getJavaMethod();
        AutoCastReturnType autoCastReturnType = javaMethod.getAnnotation(AutoCastReturnType.class);
        if (autoCastReturnType != null) {
            IOpenClass arrayType = JavaOpenClass
                .getOpenClass(Array.newInstance(parameterTypes[0].getInstanceClass(), 1).getClass());
            IOpenCast cast = bindingContext.getCast(javaOpenMethod.getType(), arrayType);
            if (cast != null) {
                return new AutoCastableResultOpenMethod(methodCaller, arrayType, cast);
            }
        }

        return methodCaller;
    }
}
