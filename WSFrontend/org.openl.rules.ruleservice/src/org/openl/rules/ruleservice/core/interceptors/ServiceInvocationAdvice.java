package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
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
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptors;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptors;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallInterceptorGroup;
import org.openl.runtime.IEngineWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

/**
 * Advice for processing method intercepting.
 * Exception wrapping.
 * And fix memory leaks.
 * <p/>
 * Only for RuleService internal use.
 *
 * @author Marat Kamalov
 */
public final class ServiceInvocationAdvice implements MethodInterceptor, Ordered {

    private final Logger log = LoggerFactory.getLogger(ServiceInvocationAdvice.class);

    private static final String MSG_SEPARATOR = "; ";

    private Map<Method, List<ServiceMethodBeforeAdvice>> beforeInterceptors = new HashMap<Method, List<ServiceMethodBeforeAdvice>>();
    private Map<Method, List<ServiceMethodAfterAdvice<?>>> afterInterceptors = new HashMap<Method, List<ServiceMethodAfterAdvice<?>>>();
    private Map<Method, ServiceMethodAroundAdvice<?>> aroundInterceptors = new HashMap<Method, ServiceMethodAroundAdvice<?>>();
    private Object serviceBean;
    private Class<?> serviceClass;
    private ServiceCallInterceptorGroup[] serviceCallInterceptorGroupSupported;

    public ServiceInvocationAdvice(Object serviceBean, Class<?> serviceClass, ServiceCallInterceptorGroup[] serviceCallInterceptorGroupSupported) {
        this.serviceBean = serviceBean;
        this.serviceClass = serviceClass;
        this.serviceCallInterceptorGroupSupported = serviceCallInterceptorGroupSupported;
        init();
    }

    private void init() {
        for (Method method : serviceClass.getMethods()) {
            Annotation[] methodAnnotations = method.getAnnotations();
            for (Annotation annotation : methodAnnotations) {
                checkForBeforeInterceptors(method, annotation);
                checkForAfterInterceptors(method, annotation);
                checkForAroundInterceptor(method, annotation);
            }
        }
    }

    private boolean groupIsSupported(ServiceCallInterceptorGroup serviceCallInterceptorGroup){
        if (ServiceCallInterceptorGroup.ALL.equals(serviceCallInterceptorGroup)){
            return true;
        }
        for (ServiceCallInterceptorGroup group : serviceCallInterceptorGroupSupported){
            if (serviceCallInterceptorGroup.equals(group)){
                return true;
            }
        }
        return false;
    }
    
    private void checkForAroundInterceptor(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallAroundInterceptor) {
            ServiceCallInterceptorGroup serviceCallInterceptorGroup = ((ServiceCallAroundInterceptor) annotation).group();
            if (groupIsSupported(serviceCallInterceptorGroup)){
                Class<? extends ServiceMethodAroundAdvice<?>> interceptorClass = ((ServiceCallAroundInterceptor) annotation)
                        .value();
                    try {
                        ServiceMethodAroundAdvice<?> aroundInterceptor = interceptorClass.getConstructor().newInstance();
                        aroundInterceptors.put(method, aroundInterceptor);
                    } catch (Exception e) {
                        throw new OpenLRuntimeException(String.format(
                                "Wrong annotation definining around interceptor for method \"%s\" of class \"%s\"",
                                method.getName(), serviceClass.getName()), e);
                    }
            }
        }
    }
    
    private void checkForBeforeInterceptors(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallBeforeInterceptor) {
            ServiceCallInterceptorGroup serviceCallInterceptorGroup = ((ServiceCallBeforeInterceptor) annotation).group();
            if (groupIsSupported(serviceCallInterceptorGroup)){
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
        if (annotation instanceof ServiceCallBeforeInterceptors){
            ServiceCallBeforeInterceptor[] serviceCallBeforeInterceptors = ((ServiceCallBeforeInterceptors) annotation).value();
            for (ServiceCallBeforeInterceptor serviceCallBeforeInterceptor : serviceCallBeforeInterceptors){
                checkForBeforeInterceptors(method, serviceCallBeforeInterceptor);
            }
        }
    }

    private void checkForAfterInterceptors(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallAfterInterceptor) {
            ServiceCallInterceptorGroup serviceCallInterceptorGroup = ((ServiceCallAfterInterceptor) annotation).group();
            if (groupIsSupported(serviceCallInterceptorGroup)){
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
        if (annotation instanceof ServiceCallAfterInterceptors){
            ServiceCallAfterInterceptor[] serviceCallAfterInterceptors = ((ServiceCallAfterInterceptors) annotation).value();
            for (ServiceCallAfterInterceptor serviceCallAfterInterceptor : serviceCallAfterInterceptors){
                checkForAfterInterceptors(method, serviceCallAfterInterceptor);
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

    protected Object afterInvocation(Method interfaceMethod, Object result, Exception t, Object... args)
            throws Throwable {
        List<ServiceMethodAfterAdvice<?>> postInterceptors = afterInterceptors.get(interfaceMethod);
        if (postInterceptors != null && postInterceptors.size() > 0) {
            Object ret = result;
            Exception lastOccuredException = t;
            for (ServiceMethodAfterAdvice<?> interceptor : postInterceptors) {
                try {
                    if (lastOccuredException == null) {
                        ret = interceptor.afterReturning(interfaceMethod, ret, args);
                    } else {
                        ret = interceptor.afterThrowing(interfaceMethod, lastOccuredException, args);
                    }
                    lastOccuredException = null;
                } catch (Exception e) {
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
        try {
            Method beanMethod = MethodUtils.getMatchingAccessibleMethod(serviceBean.getClass(), calledMethod.getName(),
                    calledMethod.getParameterTypes());
            if (beanMethod == null) {
                StringBuilder sb = new StringBuilder();
                boolean flag = true;
                for (Class<?> clazz : calledMethod.getParameterTypes()) {
                    if (flag) {
                        flag = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(clazz.getCanonicalName());
                }
                throw new OpenLRuntimeException(
                        "Called method not found in service bean. Please, check that excel file contains method with name \""
                                + calledMethod.getName() + "\" and  arguments (" + sb.toString() + ").");
            }
            try {
                beforeInvocation(interfaceMethod, args);
                if (aroundInterceptors.containsKey(interfaceMethod)){
                    result = aroundInterceptors.get(interfaceMethod).around(interfaceMethod, beanMethod, serviceBean, args);
                }else{
                    result = beanMethod.invoke(serviceBean, args);
                }
                result = afterInvocation(interfaceMethod, result, null, args);
            } catch (Exception e) {
                if (e instanceof InvocationTargetException){
                	Throwable t = e;
                	while (t instanceof InvocationTargetException){
                		Throwable t1 = ((InvocationTargetException) t).getTargetException();
                		if (t1 instanceof UndeclaredThrowableException){
                			t1 = ((UndeclaredThrowableException) t1).getUndeclaredThrowable();
                		}
                		t = t1;
                	}
                    if (t instanceof Exception){
                        result = afterInvocation(interfaceMethod, null, (Exception) t, args);
                    }else{
                        throw t;
                    }
                }else{
                    result = afterInvocation(interfaceMethod, null, e, args);
                }
            }
            return result;
        } catch (Throwable t) {
            throw new RuleServiceWrapperException(getExceptionMessage(calledMethod, t, args), t);
        } finally {
            //Memory leaks fix.
            if (serviceBean instanceof IEngineWrapper) {
                IEngineWrapper engine = (IEngineWrapper) serviceBean;
                engine.release();
            } else {
                log.warn("Service bean doesn't implement IEngineWrapper interface. Doesn't use depricated static wrapper classes. It clauses memory leaks!!!");
            }
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
            if (arg == null) {
                argsValues.append("null");
            } else {
                argsValues.append(arg.toString());

            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("During OpenL rule execution exception was occured. Method name is \"".toUpperCase());
        sb.append(method.getName());
        sb.append("\". Arguments types are: ");
        sb.append(argsTypes.toString());
        sb.append(". Arguments values are: ");
        sb.append(argsValues.toString().replace("\r", "").replace("\n", ""));
        sb.append(". Exception class is: ");
        sb.append(ex.getClass().toString());
        sb.append(".");
        if (ex.getMessage() != null) {
            sb.append(" Exception message is: ");
            sb.append(ex.getMessage());
        }
        sb.append(" OpenL clause messages are: ");
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