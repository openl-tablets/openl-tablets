/*
 * Created on Feb 24, 2004
 */
package org.openl.util;

import java.lang.reflect.*;
import java.util.Properties;

public class PropertiesProxy implements InvocationHandler {
    Properties properties;

    public static Object newProxyInstance(Properties properties, Class<?> ifc) {
        Object inst = Proxy
                .newProxyInstance(ifc.getClassLoader(), new Class[] { ifc }, new PropertiesProxy(properties));

        return inst;
    }

    PropertiesProxy(Properties properties) {
        this.properties = properties;
    }

    Object fromString(String value, Class<?> type) {
        return null;
    }

    Object getProperty(String name, Class<?> type) {
        String value = properties.getProperty(name);

        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return value;
        }

        return fromString(value, type);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getParameterTypes().length == 0) {
            return getProperty(method.getName(), method.getReturnType());
        }

        throw new IllegalArgumentException(": " + method);
    }
}
