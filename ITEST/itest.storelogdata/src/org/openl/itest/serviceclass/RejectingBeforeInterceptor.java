package org.openl.itest.serviceclass;

import java.lang.reflect.Method;

import org.openl.exception.OpenLUserRuntimeException;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;

/**
 * Rejects every call before the rule runs, as a resolver of the input that cannot resolve it.
 */
public class RejectingBeforeInterceptor implements ServiceMethodBeforeAdvice {

    public static final String MESSAGE = "The input is rejected by the before interceptor.";

    @Override
    public void before(Method interfaceMethod, Object serviceTarget, Object... args) {
        throw new OpenLUserRuntimeException(MESSAGE);
    }
}
