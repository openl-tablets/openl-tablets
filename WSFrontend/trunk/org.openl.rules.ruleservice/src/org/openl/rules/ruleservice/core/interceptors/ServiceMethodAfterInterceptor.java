package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public abstract class ServiceMethodAfterInterceptor<T> extends ServiceMethodInterceptor{
	public ServiceMethodAfterInterceptor(Method method) {
		super(method);
	}

	public abstract T invoke(Object result, Object... args);
}
