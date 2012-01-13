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

    private Map<Method, List<ServiceMethodBeforeAdvice>> beforeInterceptors = new HashMap<Method, List<ServiceMethodBeforeAdvice>>();
    private Map<Method, List<ServiceMethodAfterAdvice<?>>> afterInterceptors = new HashMap<Method, List<ServiceMethodAfterAdvice<?>>>();
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
            Class<? extends ServiceMethodBeforeAdvice>[] interceptorClasses = ((ServiceCallBeforeInterceptor) annotation)
                    .value();
            List<ServiceMethodBeforeAdvice> interceptors = beforeInterceptors.get(method);
            if (interceptors == null) {
                interceptors = new ArrayList<ServiceMethodBeforeAdvice>();
                beforeInterceptors.put(method, interceptors);
            }
            for (Class<? extends ServiceMethodBeforeAdvice> interceptorClass : interceptorClasses) {
                try {
                    ServiceMethodBeforeAdvice preInterceptor = interceptorClass.getConstructor().newInstance();
                    interceptors.add(preInterceptor);
                } catch (Exception e) {
                    throw new OpenLRuntimeException(String.format(
                            "Wrong annotation definining before interceptor for method \"%s\" of class \"%s\"",
                            method.getName(), serviceClass.getName()), e);
                }
            }
        }
    }

    private void checkForAfterInterceptor(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallAfterInterceptor) {
            Class<? extends ServiceMethodAfterAdvice<?>>[] interceptorClasses = ((ServiceCallAfterInterceptor) annotation)
                    .value();
            List<ServiceMethodAfterAdvice<?>> interceptors = afterInterceptors.get(method);
            if (interceptors == null) {
                interceptors = new ArrayList<ServiceMethodAfterAdvice<?>>();
                afterInterceptors.put(method, interceptors);
            }
            for (Class<? extends ServiceMethodAfterAdvice<?>> interceptorClass : interceptorClasses) {
                try {
                    ServiceMethodAfterAdvice<?> postInterceptor = interceptorClass.getConstructor().newInstance();
                    interceptors.add(postInterceptor);
                } catch (Exception e) {
                    throw new OpenLRuntimeException(String.format(
                            "Wrong annotation definining afterReturning interceptor for method \"%s\" of class \"%s\"",
                            method.getName(), serviceClass.getName()), e);
                }
            }
        }
    }

    protected void beforeInvocation(Method interfaceMethod, Object... args) throws Throwable {
        List<ServiceMethodBeforeAdvice> preInterceptors = beforeInterceptors.get(interfaceMethod);
        if (preInterceptors != null && preInterceptors.size() > 0) {
            for (ServiceMethodBeforeAdvice interceptor : preInterceptors) {
                interceptor.before(interfaceMethod, serviceBean, args);
            }
        }
    }

    protected Object afterInvocation(Method interfaceMethod, Object result, Throwable t, Object... args)
            throws Throwable {
        List<ServiceMethodAfterAdvice<?>> postInterceptors = afterInterceptors.get(interfaceMethod);
        if (postInterceptors != null && postInterceptors.size() > 0) {
            Object ret = result;
            Throwable lastOccuredException = t;
            for (ServiceMethodAfterAdvice<?> interceptor : postInterceptors) {
                try {
                    if (lastOccuredException == null) {
                        ret = interceptor.afterReturning(interfaceMethod, ret, args);
                    } else {
                        ret = interceptor.afterThrowing(interfaceMethod, lastOccuredException, args);
                    }
                    lastOccuredException = null;
                } catch (Throwable e) {
                    lastOccuredException = e;
                    ret = null;
                }
            }
            if (lastOccuredException != null) {
                throw lastOccuredException;
            } else {
                return ret;
            }
        } else {
            if (t != null) {
                throw t;
            } else {
                return result;
            }
        }
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method calledMethod = invocation.getMethod();
        Object[] args = invocation.getArguments();
        Method interfaceMethod = MethodUtils.getMatchingAccessibleMethod(serviceClass, calledMethod.getName(),
                calledMethod.getParameterTypes());
        Object result = null;
        // boolean isWrappedException = true;
        try {
            beforeInvocation(interfaceMethod, args);
            Method beanMethod = MethodUtils.getMatchingAccessibleMethod(serviceBean.getClass(), calledMethod.getName(),
                    calledMethod.getParameterTypes());
            try {
                if (beanMethod == null){
                    throw new OpenLRuntimeException("Called method not found in ServiceBean");
                }
                result = beanMethod.invoke(serviceBean, args);
                result = afterInvocation(interfaceMethod, result, null, args);
            } catch (Throwable e) {
                result = afterInvocation(interfaceMethod, null, e, args);
                // isWrappedException = false;
            }
            return result;
        } catch (Throwable t) {
            // if (isWrappedException) {
            throw new RuleServiceWrapperException(getExceptionMessage(calledMethod, t, args), t);
            // } else {
            // throw t;
            // }
        }
    }

    protected String getExceptionMessage(Method method, Throwable ex, Object... args) {
        StringBuilder argsTypes = new StringBuilder();
        boolean f = false;
        for (Class<?> clazz : method.getParameterTypes()) {
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
        sb.append(method.getName());
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
        boolean isNotFirst = false;
        while (t != null && t.getCause() != t) {
            if ((t instanceof OpenLRuntimeException || t instanceof OpenLException) && t.getMessage() != null) {
                if (isNotFirst)
                    sb.append(MSG_SEPARATOR);
                isNotFirst = true;
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