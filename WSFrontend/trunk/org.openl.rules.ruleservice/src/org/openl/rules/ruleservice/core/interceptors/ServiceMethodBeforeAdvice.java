package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public interface ServiceMethodBeforeAdvice {
	
	public void before(Method method, Object proxy, Object... args) throws Throwable;
	
}
