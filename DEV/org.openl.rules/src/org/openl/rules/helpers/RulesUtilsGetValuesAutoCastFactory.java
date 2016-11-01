package org.openl.rules.helpers;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.AutoCastFactory;
import org.openl.binding.impl.cast.AutoCastReturnType;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.java.AutoCastResultOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;

public class RulesUtilsGetValuesAutoCastFactory implements AutoCastFactory {

    @Override
    public IMethodCaller build(IBindingContext bindingContext,
            IMethodCaller methodCaller,
            IOpenClass[] parameterTypes) {
        JavaOpenMethod method = null;
        if (methodCaller instanceof CastingMethodCaller) {
            CastingMethodCaller castingMethodCaller = (CastingMethodCaller) methodCaller;
            if (castingMethodCaller.getMethod() instanceof JavaOpenMethod) {
                method = (JavaOpenMethod) castingMethodCaller.getMethod();
            }
        }

        if (methodCaller instanceof JavaOpenMethod) {
            method = (JavaOpenMethod) methodCaller;
        }

        if (method instanceof JavaOpenMethod) {
            JavaOpenMethod javaOpenMethod = (JavaOpenMethod) method;
            Method javaMethod = javaOpenMethod.getJavaMethod();
            AutoCastReturnType autoCastReturnType = javaMethod.getAnnotation(AutoCastReturnType.class);
            if (autoCastReturnType != null) {
                IOpenClass arrayType = JavaOpenClass
                    .getOpenClass(Array.newInstance(parameterTypes[0].getInstanceClass(), 1).getClass());
                IOpenCast cast = bindingContext.getCast(method.getType(), arrayType);
                if (cast != null) {
                    return new AutoCastResultOpenMethod(methodCaller, arrayType, cast);
                }
            }
        }

        Object o = 1;

        int a = (int) (Integer) o;

        return methodCaller;
    }
}
