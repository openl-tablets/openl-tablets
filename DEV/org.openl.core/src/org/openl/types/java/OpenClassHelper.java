package org.openl.types.java;

import java.lang.reflect.Method;
import java.util.Objects;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.util.ClassUtils;

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

    public static IOpenField findRulesField(IOpenClass openClass, String methodName) {
        if (methodName.startsWith("get")) {
            // Build field name to find.
            //
            String fieldName = ClassUtils.toFieldName(methodName);
            // Try to find appropriate field.
            //
            IOpenField rulesField = openClass.getField(fieldName, true);

            if (rulesField == null) {
                fieldName = ClassUtils.capitalize(fieldName);
                rulesField = openClass.getField(fieldName, true);
            }
            return rulesField;
        }
        return null;
    }

    public static IOpenField findRulesField(IOpenClass openClass, String methodName, Class<?>... methodArgs) {
        Objects.requireNonNull(openClass, "openClass cannot be null");
        Objects.requireNonNull(methodName, "methodName cannot be null");
        if (methodName.startsWith("get") && methodArgs.length == 0) {
            IOpenField rulesField = findRulesField(openClass, methodName);
            if (rulesField != null) {
                // Cast method return type to appropriate OpenClass
                // type.
                //
                IOpenClass methodReturnType = openClass.getField(rulesField.getName()).getType();

                if (methodReturnType.isAssignableFrom(rulesField.getType())) {
                    return rulesField;
                }
            }
        }
        return null;
    }

    public static IOpenMember findRulesMember(IOpenClass openClass, String methodName, Class<?>... methodArgs) {
        IOpenMethod openMethod = findRulesMethod(openClass, methodName, methodArgs);
        if (openMethod != null) {
            return openMethod;
        }
        return findRulesField(openClass, methodName, methodArgs);
    }

    public static IOpenMethod findRulesMethod(IOpenClass openClass, Method javaMethod) {
        Objects.requireNonNull(javaMethod, "javaMethod cannot be null");
        return findRulesMethod(openClass, javaMethod.getName(), javaMethod.getParameterTypes());
    }

    public static IOpenField findRulesField(IOpenClass openClass, Method interfaceMethod) {
        Objects.requireNonNull(interfaceMethod, "interfaceMethod cannot be null");
        return findRulesField(openClass, interfaceMethod.getName(), interfaceMethod.getParameterTypes());
    }

    public static IOpenMember findRulesMember(IOpenClass openClass, Method interfaceMethod) {
        Objects.requireNonNull(interfaceMethod, "interfaceMethod cannot be null");
        return findRulesMember(openClass, interfaceMethod.getName(), interfaceMethod.getParameterTypes());
    }
}
