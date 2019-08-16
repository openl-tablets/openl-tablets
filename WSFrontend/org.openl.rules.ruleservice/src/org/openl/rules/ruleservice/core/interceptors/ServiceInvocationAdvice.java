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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.core.RuleServiceOpenLCompilationException;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationHelper;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptors;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptors;
import org.openl.rules.testmethod.OpenLUserRuntimeException;
import org.openl.runtime.IEngineWrapper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

/**
 * Advice for processing method intercepting. Exception wrapping. And fix memory leaks.
 * <p/>
 * Only for RuleService internal use.
 *
 * @author Marat Kamalov
 */
public final class ServiceInvocationAdvice implements MethodInterceptor, Ordered {

    private final Logger log = LoggerFactory.getLogger(ServiceInvocationAdvice.class);

    private static final String MSG_SEPARATOR = "; ";

    private Map<Method, List<ServiceMethodBeforeAdvice>> beforeInterceptors = new HashMap<>();
    private Map<Method, List<ServiceMethodAfterAdvice<?>>> afterInterceptors = new HashMap<>();
    private Map<Method, ServiceMethodAroundAdvice<?>> aroundInterceptors = new HashMap<>();
    private Map<Method, ServiceExtraMethodHandler<?>> serviceExtraMethodAnnotations = new HashMap<>();

    private Object serviceTarget;
    private Class<?> serviceClass;
    private ClassLoader serviceClassLoader;
    private IOpenClass openClass;

    public ServiceInvocationAdvice(IOpenClass openClass,
            Object serviceTarget,
            Class<?> serviceClass,
            ClassLoader serviceClassLoader) {
        this.serviceTarget = serviceTarget;
        this.serviceClass = serviceClass;
        this.serviceClassLoader = serviceClassLoader;
        this.openClass = openClass;
        init();
    }

    public Map<Method, List<ServiceMethodAfterAdvice<?>>> getAfterInterceptors() {
        return afterInterceptors;
    }

    public Map<Method, List<ServiceMethodBeforeAdvice>> getBeforeInterceptors() {
        return beforeInterceptors;
    }

    public Map<Method, ServiceMethodAroundAdvice<?>> getAroundInterceptors() {
        return aroundInterceptors;
    }

    public Map<Method, ServiceExtraMethodHandler<?>> getServiceExtraMethodAnnotations() {
        return serviceExtraMethodAnnotations;
    }

    private void init() {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(serviceClassLoader);
            for (Method method : serviceClass.getMethods()) {
                Annotation[] methodAnnotations = method.getAnnotations();
                for (Annotation annotation : methodAnnotations) {
                    checkForBeforeInterceptors(method, annotation);
                    checkForAfterInterceptors(method, annotation);
                    checkForAroundInterceptor(method, annotation);
                    checkForServiceExtraMethodAnnotation(method, annotation);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private void processAwareInterfaces(Object o, Method method) {
        if (o instanceof IOpenClassAware) {
            ((IOpenClassAware) o).setIOpenClass(openClass);
        }
        if (o instanceof IOpenMemberAware) {
            IOpenMember openMember = RuleServiceOpenLServiceInstantiationHelper.getOpenMember(method, serviceTarget);
            ((IOpenMemberAware) o).setIOpenMember(openMember);
        }
    }

    private void checkForAroundInterceptor(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallAroundInterceptor) {
            Class<? extends ServiceMethodAroundAdvice<?>> interceptorClass = ((ServiceCallAroundInterceptor) annotation)
                .value();
            try {
                ServiceMethodAroundAdvice<?> aroundInterceptor = interceptorClass.newInstance();
                processAwareInterfaces(aroundInterceptor, method);
                aroundInterceptors.put(method, aroundInterceptor);
            } catch (Exception e) {
                throw new RuleServiceRuntimeException(
                    String.format("Failed to instante 'around' interceptor for method '%s' in class '%s'.",
                        method.getName(),
                        serviceClass.getName()),
                    e);
            }
        }
    }

    private void checkForBeforeInterceptors(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallBeforeInterceptor) {
            Class<? extends ServiceMethodBeforeAdvice>[] interceptorClasses = ((ServiceCallBeforeInterceptor) annotation)
                .value();
            List<ServiceMethodBeforeAdvice> interceptors = beforeInterceptors.computeIfAbsent(method,
                e -> new ArrayList<>());
            for (Class<? extends ServiceMethodBeforeAdvice> interceptorClass : interceptorClasses) {

                try {
                    ServiceMethodBeforeAdvice preInterceptor = interceptorClass.newInstance();
                    processAwareInterfaces(preInterceptor, method);
                    interceptors.add(preInterceptor);
                } catch (Exception e) {
                    throw new RuleServiceRuntimeException(
                        String.format("Failed to instante 'before' interceptor for method '%s' in class '%s'.",
                            method.getName(),
                            serviceClass.getName()),
                        e);
                }
            }
        }
        if (annotation instanceof ServiceCallBeforeInterceptors) {
            ServiceCallBeforeInterceptor[] serviceCallBeforeInterceptors = ((ServiceCallBeforeInterceptors) annotation)
                .value();
            for (ServiceCallBeforeInterceptor serviceCallBeforeInterceptor : serviceCallBeforeInterceptors) {
                checkForBeforeInterceptors(method, serviceCallBeforeInterceptor);
            }
        }
    }

    private void checkForServiceExtraMethodAnnotation(Method method, Annotation annotation) {
        if (annotation instanceof ServiceExtraMethod) {
            Class<? extends ServiceExtraMethodHandler<?>> serviceExtraMethodHandlerClass = ((ServiceExtraMethod) annotation)
                .value();
            try {
                ServiceExtraMethodHandler<?> serviceExtraMethodHandler = serviceExtraMethodHandlerClass.newInstance();
                processAwareInterfaces(serviceExtraMethodHandler, method);
                serviceExtraMethodAnnotations.put(method, serviceExtraMethodHandler);
            } catch (Exception e) {
                throw new RuleServiceRuntimeException(
                    String.format("Failed to instante service method handler for method '%s' in class '%s'.",
                        method.getName(),
                        serviceClass.getName()),
                    e);
            }
        }
    }

    private void checkForAfterInterceptors(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallAfterInterceptor) {
            Class<? extends ServiceMethodAfterAdvice<?>>[] interceptorClasses = ((ServiceCallAfterInterceptor) annotation)
                .value();
            List<ServiceMethodAfterAdvice<?>> interceptors = afterInterceptors.computeIfAbsent(method,
                e -> new ArrayList<>());
            for (Class<? extends ServiceMethodAfterAdvice<?>> interceptorClass : interceptorClasses) {
                try {
                    ServiceMethodAfterAdvice<?> postInterceptor = interceptorClass.newInstance();
                    processAwareInterfaces(postInterceptor, method);
                    interceptors.add(postInterceptor);
                } catch (Exception e) {
                    throw new RuleServiceRuntimeException(
                        String.format("Failed to instante 'afterReturning' interceptor for method '%s' in class '%s'.",
                            method.getName(),
                            serviceClass.getName()),
                        e);
                }
            }
        }
        if (annotation instanceof ServiceCallAfterInterceptors) {
            ServiceCallAfterInterceptor[] serviceCallAfterInterceptors = ((ServiceCallAfterInterceptors) annotation)
                .value();
            for (ServiceCallAfterInterceptor serviceCallAfterInterceptor : serviceCallAfterInterceptors) {
                checkForAfterInterceptors(method, serviceCallAfterInterceptor);
            }
        }
    }

    protected void beforeInvocation(Method interfaceMethod, Object... args) throws Throwable {
        List<ServiceMethodBeforeAdvice> preInterceptors = beforeInterceptors.get(interfaceMethod);
        if (preInterceptors != null && !preInterceptors.isEmpty()) {
            for (ServiceMethodBeforeAdvice interceptor : preInterceptors) {
                interceptor.before(interfaceMethod, serviceTarget, args);
            }
        }
    }

    protected Object serviceExtraMethodInvoke(Method interfaceMethod,
            Object serviceBean,
            Object... args) throws Throwable {
        ServiceExtraMethodHandler<?> serviceExtraMethodHandler = serviceExtraMethodAnnotations.get(interfaceMethod);
        if (serviceExtraMethodHandler != null) {
            return serviceExtraMethodHandler.invoke(interfaceMethod, serviceBean, args);
        }
        throw new OpenLRuntimeException("Service method advice hasn't been found!");
    }

    protected Object afterInvocation(Method interfaceMethod,
            Object result,
            Exception t,
            Object... args) throws Throwable {
        List<ServiceMethodAfterAdvice<?>> postInterceptors = afterInterceptors.get(interfaceMethod);
        if (postInterceptors != null && !postInterceptors.isEmpty()) {
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

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method calledMethod = invocation.getMethod();
        Object[] args = invocation.getArguments();
        Method interfaceMethod = MethodUtils
            .getMatchingAccessibleMethod(serviceClass, calledMethod.getName(), calledMethod.getParameterTypes());
        Object result = null;
        try {
            Method beanMethod = null;
            if (!calledMethod.isAnnotationPresent(ServiceExtraMethod.class)) {
                beanMethod = MethodUtils.getMatchingAccessibleMethod(serviceTarget.getClass(),
                    calledMethod.getName(),
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
                        "Called method hasn't been found in service bean. Please, check that excel file contains method with name '" + calledMethod
                            .getName() + "' and arguments (" + sb.toString() + ").");
                }
            }
            try {
                ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(serviceClassLoader);
                    beforeInvocation(interfaceMethod, args);
                    if (aroundInterceptors.containsKey(interfaceMethod)) {
                        result = aroundInterceptors.get(interfaceMethod)
                            .around(interfaceMethod, beanMethod, serviceTarget, args);
                    } else {
                        if (beanMethod != null) {
                            result = beanMethod.invoke(serviceTarget, args);
                        } else {
                            result = serviceExtraMethodInvoke(interfaceMethod, serviceTarget, args);
                        }
                    }
                    result = afterInvocation(interfaceMethod, result, null, args);
                } finally {
                    Thread.currentThread().setContextClassLoader(oldClassLoader);
                }
            } catch (Exception e) {
                if (e instanceof InvocationTargetException) {
                    Throwable t = extractInvocationTargetException(e);
                    if (t instanceof Exception) {
                        result = afterInvocation(interfaceMethod, null, (Exception) t, args);
                    } else {
                        throw t;
                    }
                } else {
                    result = afterInvocation(interfaceMethod, null, e, args);
                }
            }
            return result;
        } catch (Exception t) {
            Pair<ExceptionType, String> p = getExceptionDetailAndType(t);
            throw new RuleServiceWrapperException(p.getRight(),
                p.getLeft(),
                getExceptionMessage(calledMethod, t, args),
                t);
        } finally {
            // Memory leaks fix.
            if (serviceTarget instanceof IEngineWrapper) {
                IEngineWrapper engine = (IEngineWrapper) serviceTarget;
                engine.release();
            } else {
                log.warn(
                    "Service bean doesn't implement IEngineWrapper interface. Plese, don't use deprecated static wrapper classes. It can be cause of memory leaks!!!");
            }
        }
    }

    private Throwable extractInvocationTargetException(Throwable e) {
        Throwable t = e;
        while (t instanceof InvocationTargetException || t instanceof UndeclaredThrowableException) {
            if (t instanceof InvocationTargetException) {
                Throwable t1 = ((InvocationTargetException) t).getTargetException();
                t = t1;
            }
            if (t instanceof UndeclaredThrowableException) {
                Throwable t1 = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
                t = t1;
            }
        }
        return t;
    }

    protected Pair<ExceptionType, String> getExceptionDetailAndType(Exception ex) {
        Throwable t = ex;

        ExceptionType type = ExceptionType.SYSTEM;
        String message = ex.getMessage();

        boolean f = true;
        while (f) {
            t = extractInvocationTargetException(t);
            if (t instanceof OpenLUserRuntimeException) {
                type = ExceptionType.USER_ERROR;
                message = t.getMessage();
            } else if (t instanceof OutsideOfValidDomainException) {
                type = ExceptionType.VALIDATION;
                message = ((OutsideOfValidDomainException) t).getOriginalMessage();
            } else if (t instanceof OpenLRuntimeException) {
                type = ExceptionType.RULES_RUNTIME;
                message = ((OpenLRuntimeException) t).getOriginalMessage();
            } else if (t instanceof OpenLCompilationException || t instanceof RuleServiceOpenLCompilationException) {
                type = ExceptionType.COMPILATION;
                message = t.getMessage();
            }
            if (t.getCause() == null) {
                f = false;
            } else {
                t = t.getCause();
            }
        }
        return new ImmutablePair<>(type, message);
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
        sb.append("During OpenL rule execution exception was occurred. Method name is '".toUpperCase());
        sb.append(method.getName());
        sb.append("'. Arguments types are: ");
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
                if (isNotFirst) {
                    sb.append(MSG_SEPARATOR);
                }
                isNotFirst = true;
                if (t instanceof OpenLRuntimeException) {
                    sb.append(((OpenLRuntimeException) t).getOriginalMessage());
                } else {
                    sb.append(t.getMessage());
                }
            }
            t = t.getCause();
        }
        return sb.toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}