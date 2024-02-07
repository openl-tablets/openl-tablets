package org.openl.rules.helpers;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.MethodCallerWrapper;
import org.openl.binding.impl.cast.MethodSearchTuner;
import org.openl.binding.impl.method.AutoCastableResultOpenMethod;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.impl.StaticDomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;

/**
 * Implementation of {@link MethodCallerWrapper} for getValues method from {@link RulesUtils}.
 */
public class GetValuesMethodCallerWrapper implements MethodCallerWrapper {

    @Override
    public IMethodCaller handle(IMethodCaller methodCaller,
                                JavaOpenMethod javaOpenMethod,
                                IOpenClass[] callParams,
                                ICastFactory castFactory) {

        Method javaMethod = javaOpenMethod.getJavaMethod();
        MethodSearchTuner autoCastReturnType = javaMethod.getAnnotation(MethodSearchTuner.class);
        if (autoCastReturnType != null) {
            IOpenClass arrayType = JavaOpenClass.getOpenClass(
                    Array.newInstance(((StaticDomainOpenClass) callParams[0]).getDelegate().getInstanceClass(), 1)
                            .getClass());
            IOpenCast cast = castFactory.getCast(javaOpenMethod.getType(), arrayType);
            if (cast != null) {
                return new AutoCastableResultOpenMethod(methodCaller, arrayType, cast);
            }
        }

        return methodCaller;
    }
}
