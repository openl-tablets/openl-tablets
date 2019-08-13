package org.openl.rules.ruleservice.publish.rmi;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.openl.runtime.IOpenLInvocationHandler;

class DefaultRmiInvocationHandler implements IOpenLInvocationHandler<String, List<Method>> {

    private Object target;
    private Map<String, List<Method>> methodMap;

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public List<Method> getTargetMember(String key) {
        return methodMap.get(key);
    }

    public DefaultRmiInvocationHandler(Object target, Map<String, List<Method>> methodMap) {
        if (target == null) {
            throw new IllegalArgumentException("target argument must not be null!");
        }
        if (methodMap == null) {
            throw new IllegalArgumentException("methodMap argument must not be null!");
        }
        this.target = target;
        this.methodMap = methodMap;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String ruleName = (String) args[0];
        Class<?>[] inputParamsTypes = null;
        Object[] params = null;
        boolean strictMatch = true;
        if (args.length == 3) {
            inputParamsTypes = (Class<?>[]) args[1];
            params = (Object[]) args[2];
            if (inputParamsTypes.length != params.length) {
                throw new IllegalArgumentException("inputParamTypes size must be equals to params size!");
            }
        } else {
            strictMatch = false;
            params = (Object[]) args[1];
            inputParamsTypes = new Class<?>[params.length];
            int i = 0;
            for (Object o : params) {
                inputParamsTypes[i] = o.getClass();
                i++;
            }
        }
        List<Method> methods = methodMap.get(ruleName);
        if (methods == null) {
            throw new IllegalArgumentException("Method with requested ruleName hasn't been found!");
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
            return matchedMethod.invoke(target, params);
        } else {
            if (match > 1) {
                throw new IllegalArgumentException(
                    "More than one method has been found with requested ruleName and parameters.");
            } else {
                throw new IllegalArgumentException("Method with requested ruleName and parameters hasn't been found.");
            }
        }
    }
}