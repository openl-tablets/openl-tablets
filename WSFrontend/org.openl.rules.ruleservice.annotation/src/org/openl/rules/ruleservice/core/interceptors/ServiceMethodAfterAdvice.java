package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

/**
 * After advice for intercepting method calls.
 * 
 * @author Marat Kamalov
 * 
 * @param <T>
 */
public interface ServiceMethodAfterAdvice<T> {

    /**
     * If after advice defined for a service method, invokes this method after method execution finished. Return value
     * can be changed to another object or change returning with throwing exception. Return object should be assignable
     * to service method return type.
     * 
     * @param method service method
     * @param result method return value
     * @param args method arguments
     * @return method return value
     * @throws Exception
     */
    T afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception;

    /**
     * If after advice defined for a service method, invokes this method after method execution finished with throwing
     * exception. Thrown exception can be changed to another or return object instead exception. Return object should be
     * assignable to service method return type.
     * 
     * @param method service method
     * @param result method return value
     * @param args method arguments
     * @return method return value
     * @throws Exception
     */
    T afterThrowing(Method interfaceMethod, Exception t, Object... args) throws Exception;

}
