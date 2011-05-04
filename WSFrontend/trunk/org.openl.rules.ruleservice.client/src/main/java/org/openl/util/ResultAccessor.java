package org.openl.util;

import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

public class ResultAccessor {

    private Object target;

    public ResultAccessor(Object target) {
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

    public Object getFieldValue(String fieldName) throws IllegalAccessException, NoSuchFieldException {
        Field field = target.getClass().getField(fieldName);
        return field.get(target);
    }

    public void setValue(String setterName, Object value) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        invokeMethod(makeSetter(setterName), value);
    }

    private String makeSetter(String fieldName) {
        return "set" + StringUtils.capitalize(fieldName);
    }
}
