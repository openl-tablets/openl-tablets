package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

/**
 * Around advice for intercepting method calls. Implementation must be thread safe.
 *
 * @author Marat Kamalov
 *
 */
public interface ServiceMethodAroundAdvice<T> extends ServiceMethodAdvice {
    /**
     * If around advice defined for a service method, invokes this method.
     *
     * @param method service method
     * @param proxy service bean
     * @param args method arguments
     * @throws Throwable
     */
    T around(Method interfaceMethod, Method serviceTargetMethod, Object serviceTarget, Object... args) throws Throwable;

}
