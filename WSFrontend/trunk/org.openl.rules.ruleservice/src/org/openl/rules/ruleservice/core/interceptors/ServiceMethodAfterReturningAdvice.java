package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public interface ServiceMethodAfterReturningAdvice<T> {
	
	public T afterReturning(Method method, Object result, Object... args)
			throws Throwable;

}
