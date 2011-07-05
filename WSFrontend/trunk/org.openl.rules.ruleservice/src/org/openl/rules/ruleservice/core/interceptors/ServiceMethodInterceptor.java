package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public class ServiceMethodInterceptor {

	private Method method;

	public ServiceMethodInterceptor(Method method) {
		this.method = method;
	}

	public Method getMethod() {
		return method;
	}

}
