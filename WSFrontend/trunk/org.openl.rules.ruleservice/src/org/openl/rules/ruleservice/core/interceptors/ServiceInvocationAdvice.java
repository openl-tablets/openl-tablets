package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.beanutils.MethodUtils;
import org.openl.exception.OpenLException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;
import org.springframework.core.Ordered;

public class ServiceInvocationAdvice implements MethodInterceptor, Ordered {

    private static final String MSG_SEPARATOR = "; ";

    private Map<Method, List<ServiceMethodBeforeInterceptor>> beforeInterceptors = new HashMap<Method, List<ServiceMethodBeforeInterceptor>>();
    private Map<Method, List<ServiceMethodAfterInterceptor<?>>> afterInterceptors = new HashMap<Method, List<ServiceMethodAfterInterceptor<?>>>();
    private Object serviceBean;
    private Class<?> serviceClass;

    public ServiceInvocationAdvice(Object serviceBean, Class<?> serviceClass) {
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
            Class<? extends ServiceMethodBeforeInterceptor>[] interceptorClasses = ((ServiceCallBeforeInterceptor) annotation).interceptorClass();

            for (Class<? extends ServiceMethodBeforeInterceptor> interceptorClass : interceptorClasses) {
                try {
                    ServiceMethodBeforeInterceptor preInterceptor = interceptorClass.getConstructor(Method.class)
                        .newInstance(method);
                    List<ServiceMethodBeforeInterceptor> interceptors = beforeInterceptors.get(method);
                    if (interceptors == null) {
                        interceptors = new ArrayList<ServiceMethodBeforeInterceptor>();
                        beforeInterceptors.put(method, interceptors);
                    }
                    interceptors.add(preInterceptor);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Wrong annotation definining BeforeInterceptor for method \"%s\" of class \"%s\"",
                        method.getName(),
                        serviceClass.getName()));
                }

            }
        }
    }

    private void checkForAfterInterceptor(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallAfterInterceptor) {
            Class<? extends ServiceMethodAfterInterceptor<?>>[] interceptorClasses = ((ServiceCallAfterInterceptor) annotation).interceptorClass();
            for (Class<? extends ServiceMethodAfterInterceptor<?>> interceptorClass : interceptorClasses) {
                try {
                    ServiceMethodAfterInterceptor<?> postInterceptor = interceptorClass.getConstructor(Method.class)
                        .newInstance(method);
                    List<ServiceMethodAfterInterceptor<?>> interceptors = afterInterceptors.get(method);
                    if (interceptors == null) {
                        interceptors = new ArrayList<ServiceMethodAfterInterceptor<?>>();
                        afterInterceptors.put(method, interceptors);
                    }
                    interceptors.add(postInterceptor);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Wrong annotation definining AfterInterceptor for method \"%s\" of class \"%s\"",
                        method.getName(),
                        serviceClass.getName()));
                }
            }
        }
    }

    public void beforeInvocation(Object[] args, Method interfaceMethod) {
        List<ServiceMethodBeforeInterceptor> preInterceptors = beforeInterceptors.get(interfaceMethod);
        if (preInterceptors != null && preInterceptors.size() > 0) {
            for (ServiceMethodBeforeInterceptor interceptor : preInterceptors) {
                interceptor.invoke(serviceBean, args);
            }
        }
    }

    public Object afterInvocation(Object[] args, Method interfaceMethod, Object result) {
        List<ServiceMethodAfterInterceptor<?>> postInterceptors = afterInterceptors.get(interfaceMethod);
        if (postInterceptors != null && postInterceptors.size() > 0) {
            for (ServiceMethodAfterInterceptor<?> interceptor : postInterceptors) {
                result = interceptor.invoke(result, args);
            }
        }
        return result;
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method calledMethod = invocation.getMethod();
        Object[] args = invocation.getArguments();
        try {
            Method interfaceMethod = MethodUtils.getMatchingAccessibleMethod(serviceClass,
                calledMethod.getName(),
                calledMethod.getParameterTypes());
            beforeInvocation(args, interfaceMethod);
            Method beanMethod = MethodUtils.getMatchingAccessibleMethod(serviceBean.getClass(),
                calledMethod.getName(),
                calledMethod.getParameterTypes());
            Object result = beanMethod.invoke(serviceBean, args);

            result = afterInvocation(args, interfaceMethod, result);
            return result;
        } catch (Throwable t) {
            throw new RuleServiceWrapperException(getExceptionMessage(calledMethod, args, t), t);
        }
    }

    public String getExceptionMessage(Method m, Object[] args, Throwable ex) {
        StringBuilder argsTypes = new StringBuilder();
        boolean f = false;
        for (Class<?> clazz : m.getParameterTypes()) {
            if (f) {
                argsTypes.append(", ");
            } else {
                f = true;
            }
            argsTypes.append(clazz.getName());
        }
        StringBuilder argsValues = new StringBuilder();
        f = false;
        for (Object arg : args) {
            if (f) {
                argsValues.append(", ");
            } else {
                f = true;
            }
            argsValues.append(arg.toString());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("During OpenL rule execution exception was occured. Method name is \"".toUpperCase());
        sb.append(m.getName());
        sb.append("\". Arguments types are: ".toUpperCase());
        sb.append(argsTypes.toString());
        sb.append(". Arguments values are: ".toUpperCase());
        sb.append(argsValues.toString().replace("\r", "").replace("\n", ""));
        sb.append(". Exception class is: ".toUpperCase());
        sb.append(ex.getClass().toString());
        sb.append(".");
        if (ex.getMessage() != null) {
            sb.append(" Exception message is: ".toUpperCase());
            sb.append(ex.getMessage());
        }
        sb.append(" OpenL clause messages are: ".toUpperCase());
        Throwable t = ex.getCause();
        boolean g = false;
        while (t != null && t.getCause() != t) {
            if ((t instanceof OpenLRuntimeException || t instanceof OpenLException) && t.getMessage() != null) {
                if (g)
                    sb.append(MSG_SEPARATOR);
                g = true;
                sb.append(t.getMessage());
            }
            t = t.getCause();
        }
        throw new RuleServiceWrapperException(sb.toString(), ex);
    }

    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}