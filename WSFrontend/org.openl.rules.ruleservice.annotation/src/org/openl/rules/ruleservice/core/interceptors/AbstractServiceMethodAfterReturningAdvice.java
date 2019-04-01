package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

/***
 * Abstract implementation ServiceMethodAfterAdvice that can be usefull if you should intercept only after throwing
 * situation. After throwing situation is implemented.
 *
 * @author Marat Kamalov
 *
 * @param <T>
 */
public abstract class AbstractServiceMethodAfterReturningAdvice<T> implements ServiceMethodAfterAdvice<Object> {

    /** {@inheritDoc} */
    @Override
    public final T afterThrowing(Method method, Exception t, Object... args) throws Exception {
        throw t;
    }
}
