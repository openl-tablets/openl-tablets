package org.openl.binding.impl.cast;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.openl.binding.IBindingContext;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.java.AutoCastResultOpenMethod;
import org.openl.types.java.JavaOpenMethod;

public class DefaultAutoCastFactory implements AutoCastFactory {

	@Override
	public IMethodCaller build(IBindingContext bindingContext, IMethodCaller methodCaller,
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
				int i = 0;
				for (Annotation[] parameterAnnotations : javaMethod.getParameterAnnotations()) {
					for (Annotation annotation : parameterAnnotations) {
						if (annotation instanceof ReturnType) {
							ReturnType returnType = (ReturnType) annotation;
							return AutoCastResultOpenMethod.buildAutoCastResultOpenMethod(bindingContext, methodCaller,
									method, parameterTypes[i],
									returnType.arrayDimension() >= 0 ? returnType.arrayDimension() : null);
						}
					}
					i++;
				}
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
