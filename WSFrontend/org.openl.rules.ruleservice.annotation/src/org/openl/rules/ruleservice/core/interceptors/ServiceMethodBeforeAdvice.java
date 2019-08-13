package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

/**
 * Before advice for intercepting method calls.
 *
 * @author Marat Kamalov
 *
 */
public interface ServiceMethodBeforeAdvice {

    /**
     * If before advice defined for a service method, invokes this method.
     *
     * @param method service method
     * @param proxy service bean
     * @param args method arguments
     * @throws Throwable
     */
    void before(Method interfaceMethod, Object serviceTarget, Object... args) throws Throwable;

}
