package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

/***
 * Abstract implementation ServiceMethodAfterAdvice that can be usefull if you
 * should intercept only after throwing situation. After returning situation is
 * implemented.
 * 
 * @author Marat Kamalov
 * 
 * @param <T>
 */
public abstract class AbstractServiceMethodAfterThrowingAdvice<T> implements ServiceMethodAfterAdvice<T> {

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public final T afterReturning(Method method, Object result, Object... args) throws Exception {
        return (T) result;
    }
}
