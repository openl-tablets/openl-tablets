package org.openl.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

public class ClassHelper {

    private Object target;

    public ClassHelper(Object target) {
        this.target = target;
    }

    public Object invokeMethod(String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    public Object invokeMethod(String methodName, Object ... params) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        List<Class<?>> paramTypes = new LinkedList<Class<?>>();
        for(Object param : params) {
            paramTypes.add(param.getClass());
        }
        Class<?>[] paramTypesArray = new Class<?>[paramTypes.size()];
        paramTypes.toArray(paramTypesArray);
        Method method = target.getClass().getMethod(methodName, paramTypesArray);
        return method.invoke(target, params);
    }

    public Object invokeMethod(String methodName, Object[] parameterValues, Class[] parameterTypes) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, parameterValues);
    }

    public Object invokeMethod(String methodName, Object parameterValues, Class parameterTypes) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, parameterValues);
    }
}
