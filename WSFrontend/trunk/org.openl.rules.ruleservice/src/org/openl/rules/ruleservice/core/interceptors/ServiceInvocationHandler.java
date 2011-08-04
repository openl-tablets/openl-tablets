package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.MethodUtils;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;

public class ServiceInvocationHandler implements InvocationHandler {
    private Map<Method, ServiceMethodBeforeInterceptor> beforeInterceptors = new HashMap<Method, ServiceMethodBeforeInterceptor>();
    private Map<Method, ServiceMethodAfterInterceptor<?>> afterInterceptors = new HashMap<Method, ServiceMethodAfterInterceptor<?>>();
    private Object serviceBean;
    private Class<?> serviceClass;

    public ServiceInvocationHandler(Object serviceBean, Class<?> serviceClass) {
        this.serviceBean = serviceBean;
        this.serviceClass = serviceClass;
        init();
    }

    private void init() {
        for (Method method : serviceClass.getMethods()) {
            Annotation[] methodAnnotations = method.getAnnotations();
            for (Annotation annotation : methodAnnotations) {
                checkForBeforeInterceptor(method, annotation);
                checkForAfterInterceptor(method, annotation);
            }
        }
    }

    private void checkForBeforeInterceptor(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallBeforeInterceptor) {
            Class<? extends ServiceMethodBeforeInterceptor>[] interceptorClasses = ((ServiceCallBeforeInterceptor) annotation)
                    .interceptorClass();

            for (Class<? extends ServiceMethodBeforeInterceptor> interceptorClass : interceptorClasses) {
                try {
                    ServiceMethodBeforeInterceptor preInterceptor = interceptorClass.getConstructor(Method.class)
                            .newInstance(method);
                    beforeInterceptors.put(method, preInterceptor);
                } catch (Exception e) {
                    throw new RuntimeException(String.format(
                            "Wrong annotation definining BeforeInterceptor for method \"%s\" of class \"%s\"",
                            method.getName(), serviceClass.getName()));
                }

            }
        }
    }

    private void checkForAfterInterceptor(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallAfterInterceptor) {
            Class<? extends ServiceMethodAfterInterceptor<?>>[] interceptorClasses = ((ServiceCallAfterInterceptor) annotation)
                    .interceptorClass();
            for (Class<? extends ServiceMethodAfterInterceptor<?>> interceptorClass : interceptorClasses) {
                try {
                    ServiceMethodAfterInterceptor<?> postInterceptor = interceptorClass.getConstructor(Method.class)
                            .newInstance(method);
                    afterInterceptors.put(method, postInterceptor);
                } catch (Exception e) {
                    throw new RuntimeException(String.format(
                            "Wrong annotation definining AfterInterceptor for method \"%s\" of class \"%s\"",
                            method.getName(), serviceClass.getName()));
                }
            }
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (beforeInterceptors.get(method) != null) {
            beforeInterceptors.get(method).invoke(proxy, args);
        }
        Method matchingAccessibleMethod = MethodUtils.getMatchingAccessibleMethod(serviceBean.getClass(),
                method.getName(), method.getParameterTypes());
        Object result = matchingAccessibleMethod.invoke(serviceBean, args);
        if (afterInterceptors.get(method) != null) {
            result = afterInterceptors.get(method).invoke(result, args);
        }
        return result;
    }

}
