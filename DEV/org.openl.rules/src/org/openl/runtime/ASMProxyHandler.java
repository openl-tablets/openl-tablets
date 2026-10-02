package org.openl.runtime;

import java.lang.reflect.Method;

@FunctionalInterface
public interface ASMProxyHandler {

    // Handles any method of a generated proxy, so it hands on whatever the invoked method throws.
    @SuppressWarnings("java:S112")
    Object invoke(Method method, Object[] args) throws Exception;
}
