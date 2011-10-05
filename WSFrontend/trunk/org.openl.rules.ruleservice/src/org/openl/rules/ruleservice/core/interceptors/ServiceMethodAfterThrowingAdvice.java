package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public interface ServiceMethodAfterThrowingAdvice {
	
	void afterThrowing(Method method, Throwable t, Object... args) throws Throwable;
	
}
