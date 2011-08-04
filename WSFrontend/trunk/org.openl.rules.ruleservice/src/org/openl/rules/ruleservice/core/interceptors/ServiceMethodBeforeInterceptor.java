package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public abstract class ServiceMethodBeforeInterceptor extends ServiceMethodInterceptor{
	public ServiceMethodBeforeInterceptor(Method method) {
		super(method);
	}

	public abstract void invoke(Object proxy, Object... args);
}
