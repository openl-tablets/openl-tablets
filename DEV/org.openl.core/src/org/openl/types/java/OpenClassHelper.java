package org.openl.types.java;

import java.lang.reflect.Method;
import java.util.Objects;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public final class OpenClassHelper {

    private OpenClassHelper() {
    }

    public static IOpenMethod findRulesMethod(IOpenClass openClass, String methodName, Class<?>... methodArgs) {
        Objects.requireNonNull(openClass, "openClass cannot be null");
        Objects.requireNonNull(methodName, "methodName cannot be null");
        IOpenClass[] args = new IOpenClass[methodArgs.length];
        for (int i = 0; i < methodArgs.length; i++) {
            args[i] = JavaOpenClass.getOpenClass(methodArgs[i]);
        }
        return openClass.getMethod(methodName, args);
    }

    public static IOpenMethod findRulesMethod(IOpenClass openClass, Method javaMethod) {
        Objects.requireNonNull(javaMethod, "javaMethod cannot be null");
        return findRulesMethod(openClass, javaMethod.getName(), javaMethod.getParameterTypes());
    }
}
