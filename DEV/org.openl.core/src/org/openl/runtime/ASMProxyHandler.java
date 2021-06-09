package org.openl.runtime;

import java.lang.reflect.Method;

@FunctionalInterface
public interface ASMProxyHandler {

    Object invoke(Method method, Object[] args) throws Exception;
}
