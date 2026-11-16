package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

/**
 * Learns of the steps of a service method call: the call of the method itself and the call of each of its
 * interceptors.
 *
 * <p>A step is reported before it runs and after it ends. Each report gets the exception the call has at that moment,
 * or {@code null} while the call passes. The report after a step gets the result of the service method, or of its
 * around interceptor: an after interceptor does not change the result the listeners get.
 *
 * <p>When a before interceptor rejects the call, the method and its around interceptor are not invoked. Their
 * listeners are still called, before and after, with the exception of the interceptor. The other interceptors after
 * the rejecting one are not reported.
 *
 * <p>While the call fails, a listener that throws a runtime exception does not change the outcome of the call: the
 * exception is logged, and the next listeners are still called. While the call passes, the exception fails the call.
 */
public interface ServiceInvocationAdviceListener {

    /**
     * Called before the service method is invoked, or would be invoked if a before interceptor had not rejected the
     * call. A method with an around interceptor reports the interceptor instead.
     */
    default void beforeMethodInvocation(Method interfaceMethod,
                                        Object[] args,
                                        @Nullable Object result,
                                        @Nullable Exception ex,
                                        Instantiator postProcessAdvice) {
    }

    /**
     * Called after the service method ends, with its result or its exception. For a rejected call, the exception is
     * the one of the before interceptor.
     */
    default void afterMethodInvocation(Method interfaceMethod,
                                       Object[] args,
                                       @Nullable Object result,
                                       @Nullable Exception ex,
                                       Instantiator postProcessAdvice) {
    }

    /**
     * Called before an interceptor of the service method runs.
     */
    default void beforeServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
                                           Method interfaceMethod,
                                           Object[] args,
                                           @Nullable Object result,
                                           @Nullable Exception ex,
                                           Instantiator postProcessAdvice) {
    }

    /**
     * Called after an interceptor of the service method ends, with the exception the interceptor leaves: the one it
     * throws, or none when it handles the exception it gets. For an around interceptor of a rejected call, the
     * exception is the one of the before interceptor.
     */
    default void afterServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
                                          Method interfaceMethod,
                                          Object[] args,
                                          @Nullable Object result,
                                          @Nullable Exception ex,
                                          Instantiator postProcessAdvice) {
    }

    @FunctionalInterface
    interface Instantiator {
        <T> T instantiate(Class<T> clazz);
    }
}
