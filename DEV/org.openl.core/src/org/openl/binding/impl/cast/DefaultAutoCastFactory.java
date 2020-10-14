package org.openl.binding.impl.cast;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.method.AutoCastableResultOpenMethod;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.OpenClassUtils;

public class DefaultAutoCastFactory implements AutoCastFactory {

    private IOpenClass extractSimpleReturnType(IOpenClass type, Integer arrayDim) {
        IOpenClass simpleReturnType = type;
        int d = 0;
        while (simpleReturnType.isArray()) {
            if (simpleReturnType.getAggregateInfo() != null) {
                simpleReturnType = simpleReturnType.getAggregateInfo().getComponentType(simpleReturnType);
            } else {
                simpleReturnType = simpleReturnType.getComponentClass();
            }
            d++;
        }

        if (arrayDim != null && d > arrayDim) {
            simpleReturnType = JavaOpenClass.OBJECT;
        }
        return simpleReturnType;
    }

    @Override
    public IMethodCaller build(IBindingContext bindingContext,
            IMethodCaller methodCaller,
            JavaOpenMethod javaOpenMethod,
            IOpenClass[] parameterTypes) {
        Method javaMethod = javaOpenMethod.getJavaMethod();
        AutoCastReturnType autoCastReturnType = javaMethod.getAnnotation(AutoCastReturnType.class);
        if (autoCastReturnType != null) {
            int i = 0;
            for (Annotation[] parameterAnnotations : javaMethod.getParameterAnnotations()) {
                for (Annotation annotation : parameterAnnotations) {
                    if (annotation instanceof ReturnType) {
                        ReturnType returnType = (ReturnType) annotation;
                        IOpenClass type;
                        if (i < javaOpenMethod.getNumberOfParameters() - 1) {
                            type = parameterTypes[i];
                        } else {
                            type = parameterTypes[i];
                            for (int j = i + 1; j < parameterTypes.length; j++) {
                                type = bindingContext.findParentClass(type, parameterTypes[j]);
                            }
                        }
                        return buildAutoCastResultOpenMethod(bindingContext,
                            methodCaller,
                            javaOpenMethod,
                            extractSimpleReturnType(type,
                                returnType.arrayDimension() >= 0 ? returnType.arrayDimension() : null));

                    }
                }
                i++;
            }
        }
        return methodCaller;
    }

    private IMethodCaller buildAutoCastResultOpenMethod(IBindingContext bindingContext,
            IMethodCaller methodCaller,
            JavaOpenMethod method,
            IOpenClass simpleReturnType) {
        if (NullOpenClass.the.equals(simpleReturnType)) {
            return methodCaller;
        }
        if (!method.getType().isArray()) {
            IOpenCast cast = bindingContext.getCast(method.getType(), simpleReturnType);
            if (cast != null) {
                return new AutoCastableResultOpenMethod(methodCaller, simpleReturnType, cast);
            }
        } else {
            IOpenClass v = method.getType();
            int dims = 0;
            while (v.isArray()) {
                v = v.getComponentClass();
                dims++;
            }

            IOpenClass arrayOpenClass = simpleReturnType.getArrayType(dims);
            IOpenCast cast = bindingContext.getCast(method.getType(), arrayOpenClass);
            if (cast != null) {
                return new AutoCastableResultOpenMethod(methodCaller, arrayOpenClass, cast);
            }
        }
        return methodCaller;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface ReturnType {
        int arrayDimension() default -1;
    }
}
