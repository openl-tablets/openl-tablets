package org.openl.runtime;

import java.lang.reflect.Method;

@FunctionalInterface
public interface OpenLProxyHandler {

    Object invoke(Object proxy, Method method, Object[] args) throws Exception;
}
