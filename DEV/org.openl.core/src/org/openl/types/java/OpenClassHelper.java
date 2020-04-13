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
        for (IOpenMethod m : openClass.getMethods()) {
            if (methodName.equals(m.getName()) && methodArgs.length == m.getSignature().getNumberOfParameters()) {
                boolean f = true;
                for (int i = 0; i < m.getSignature().getNumberOfParameters(); i++) {
                    if (!Objects.equals(methodArgs[i], m.getSignature().getParameterType(i).getInstanceClass())) {
                        f = false;
                        break;
                    }
                }
                if (f) {
                    return m;
                }
            }
        }
        return null;
    }

    public static IOpenMethod findRulesMethod(IOpenClass openClass, Method javaMethod) {
        Objects.requireNonNull(javaMethod, "javaMethod cannot be null");
        return findRulesMethod(openClass, javaMethod.getName(), javaMethod.getParameterTypes());
    }
}
