package org.openl.rules.ruleservice.publish.rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openl.rules.ruleservice.rmi.DefaultRmiHandler;

class DefaultRmiInvocationHandler implements DefaultRmiHandler {

    private Object target;
    private Map<String, List<Method>> methodMap;

    DefaultRmiInvocationHandler(Object target, Map<String, List<Method>> methodMap) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        this.methodMap = Objects.requireNonNull(methodMap, "methodMap cannot be null");
    }

    @Override
    public Object execute(String ruleName, Class<?>[] inputParamsTypes, Object[] params) throws RemoteException {
        if (inputParamsTypes.length != params.length) {
            throw new IllegalArgumentException("inputParamTypes size must be equals to params size.");
        }
        return invoke(ruleName, inputParamsTypes, params, true);
    }

    @Override
    public Object execute(String ruleName, Object... params) throws RemoteException {
        Class<?>[] inputParamsTypes = new Class<?>[params.length];
        int i = 0;
        for (Object o : params) {
            inputParamsTypes[i] = o.getClass();
            i++;
        }
        return invoke(ruleName, inputParamsTypes, params, false);
    }

    private Object invoke(String ruleName, Class<?>[] inputParamsTypes, Object[] params, boolean strictMatch) {
        List<Method> methods = methodMap.get(ruleName);
        if (methods == null) {
            throw new IllegalArgumentException("Method with requested ruleName is not found.");
        }

        int match = 0;
        Method matchedMethod = null;
        for (Method m : methods) {
            int i = 0;
            boolean f = true;
            for (Class<?> inputParamsType : inputParamsTypes) {
                if (strictMatch && !m.getParameterTypes()[i].equals(inputParamsType)) {
                    f = false;
                    break;
                }
                if (!strictMatch && !m.getParameterTypes()[i].isAssignableFrom(inputParamsType)) {
                    f = false;
                    break;
                }
                i++;
            }
            if (f) {
                match++;
                matchedMethod = m;
            }
        }
        if (match == 1) {
            try {
                return matchedMethod.invoke(target, params);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            if (match > 1) {
                throw new IllegalArgumentException(
                    "More than one method is found with requested ruleName and parameters.");
            } else {
                throw new IllegalArgumentException("Method with requested ruleName and parameters is not found.");
            }
        }
    }
}