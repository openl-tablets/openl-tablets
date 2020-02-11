package org.openl.runtime;

import java.util.Arrays;

import org.openl.exception.OpenlNotCheckedException;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

public final class OpenLJavaAssistProxy {

    private OpenLJavaAssistProxy() {
    }

    public static Object create(ClassLoader classLoader, MethodHandler methodHandler, Class[] interfaces) {
        if (methodHandler == null) {
            throw new IllegalArgumentException("MethodHandler can not be null");
        }
        ProxyFactory factory = new ProxyFactory() {
            @Override
            protected ClassLoader getClassLoader() {
                return classLoader;
            }
        };
        interfaces = Arrays.stream(interfaces)
            .filter(i -> !i.getName().equals(ProxyObject.class.getName()))
            .toArray(Class[]::new);
        factory.setInterfaces(interfaces);
        try {
            Object instance = factory.createClass().newInstance();
            ((ProxyObject) instance).setHandler(methodHandler);
            return instance;
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to instantiate a proxy.", e);
        }
    }
}
