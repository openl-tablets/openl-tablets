package org.openl.binding.impl.cast;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.method.AutoCastableResultOpenMethod;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;

public class DefaultAutoCastFactory implements AutoCastFactory {

    protected IOpenClass extractSimpleReturnType(IOpenClass type, Integer arrayDim) {
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

    protected IOpenClass findTypeForVarargs(IOpenClass[] types, IBindingContext bindingContext) {
        if (types == null || types.length == 0) {
            throw new IllegalArgumentException("types cannot be empty or null");
        }
        IOpenClass type = types[0];
        for (int i = 1; i < types.length; i++) {
            type = bindingContext.findParentClass(type, types[i]);
        }
        return type;
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
                            type = findTypeForVarargs(Arrays.copyOfRange(parameterTypes, i, parameterTypes.length),
                                bindingContext);
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

    protected IMethodCaller buildAutoCastResultOpenMethod(IBindingContext bindingContext,
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
