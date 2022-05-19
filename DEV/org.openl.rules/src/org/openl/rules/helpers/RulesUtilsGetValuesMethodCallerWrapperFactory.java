package org.openl.rules.helpers;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.MethodCallerWrapper;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.MethodCallerWrapperFactory;
import org.openl.binding.impl.method.AutoCastableResultOpenMethod;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.impl.StaticDomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;

public class RulesUtilsGetValuesMethodCallerWrapperFactory implements MethodCallerWrapperFactory {

    @Override
    public IMethodCaller build(ICastFactory castFactory,
            IMethodCaller methodCaller,
            JavaOpenMethod javaOpenMethod,
            IOpenClass[] parameterTypes) {

        Method javaMethod = javaOpenMethod.getJavaMethod();
        MethodCallerWrapper autoCastReturnType = javaMethod.getAnnotation(MethodCallerWrapper.class);
        if (autoCastReturnType != null) {
            IOpenClass arrayType = JavaOpenClass.getOpenClass(
                Array.newInstance(((StaticDomainOpenClass) parameterTypes[0]).getDelegate().getInstanceClass(), 1)
                    .getClass());
            IOpenCast cast = castFactory.getCast(javaOpenMethod.getType(), arrayType);
            if (cast != null) {
                return new AutoCastableResultOpenMethod(methodCaller, arrayType, cast);
            }
        }

        return methodCaller;
    }
}
