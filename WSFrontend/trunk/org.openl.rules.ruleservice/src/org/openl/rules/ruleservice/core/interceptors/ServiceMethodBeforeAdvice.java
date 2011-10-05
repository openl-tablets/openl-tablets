package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public interface ServiceMethodBeforeAdvice {
	
	void before(Method method, Object proxy, Object... args) throws Throwable;
	
}
