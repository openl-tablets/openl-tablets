package org.openl.rules.ruleservice.core;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanClass;
import org.openl.rules.ruleservice.core.annotations.BeanToSpreadsheetResultConvert;
import org.openl.runtime.ASMProxyFactory;
import org.openl.runtime.ASMProxyHandler;
import org.openl.runtime.IOpenLMethodHandler;
import org.openl.types.IOpenMember;

public final class RuleServiceOpenLServiceInstantiationHelper {

    private RuleServiceOpenLServiceInstantiationHelper() {
        // Hidden constructor
    }

    /**
     * The rules member behind a service method.
     *
     * <p>The target implements either the service class, which takes a spreadsheet result as its generated bean,
     * or the rules themselves, which take the spreadsheet result: a method whose parameters are such beans is
     * looked up both ways.
     */
    public static IOpenMember getOpenMember(Method method, Object serviceTarget) {
        var member = getOpenMember(method.getName(), method.getParameterTypes(), serviceTarget);
        return member != null ? member : getOpenMember(method.getName(), rulesParameterTypes(method), serviceTarget);
    }

    private static Class<?>[] rulesParameterTypes(Method method) {
        var parameterTypes = method.getParameterTypes();
        var parameters = method.getParameters();
        for (var i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(BeanToSpreadsheetResultConvert.class)) {
                parameterTypes[i] = spreadsheetResultTypeOf(parameterTypes[i]);
            }
        }
        return parameterTypes;
    }

    /**
     * The spreadsheet result type a generated bean class stands for, an array of one for an array of beans; any
     * other type as it is.
     */
    public static Class<?> spreadsheetResultTypeOf(Class<?> type) {
        var elementType = type;
        var dimensions = 0;
        while (elementType.isArray()) {
            elementType = elementType.getComponentType();
            dimensions++;
        }
        if (!elementType.isAnnotationPresent(SpreadsheetResultBeanClass.class)) {
            return type;
        }
        return dimensions > 0 ? Array.newInstance(SpreadsheetResult.class, dimensions).getClass() : SpreadsheetResult.class;
    }

    public static IOpenMember getOpenMember(String methodName, Class<?>[] paramTypes, Object serviceTarget) {
        for (Class<?> clazz : serviceTarget.getClass().getInterfaces()) {
            try {
                var m = clazz.getMethod(methodName, paramTypes);
                return findOpenMember(m, serviceTarget);
            } catch (NoSuchMethodException ignored) {
                // method not found on this interface; try next
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static IOpenMember findOpenMember(Method method, Object serviceTarget) {
        if (ASMProxyFactory.isProxy(serviceTarget)) {
            ASMProxyHandler proxyHandler = ASMProxyFactory.getProxyHandler(serviceTarget);
            if (proxyHandler instanceof IOpenLMethodHandler) {
                return ((IOpenLMethodHandler<Method, ?>) proxyHandler).getOpenMember(method);
            }
        }
        return null;
    }
}
