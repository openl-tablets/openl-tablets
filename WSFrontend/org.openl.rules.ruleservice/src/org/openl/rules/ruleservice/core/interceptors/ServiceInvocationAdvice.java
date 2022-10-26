package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenLUserDetailedRuntimeException;
import org.openl.exception.OpenLUserRuntimeException;
import org.openl.rules.calc.CombinedSpreadsheetResultOpenClass;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanClass;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.ExceptionDetails;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.core.RuleServiceOpenLCompilationException;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationHelper;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;
import org.openl.rules.ruleservice.core.annotations.BeanToSpreadsheetResultConvert;
import org.openl.rules.ruleservice.core.annotations.ExternalParam;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.runtime.AbstractOpenLMethodHandler;
import org.openl.runtime.IEngineWrapper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.util.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;

/**
 * Advice for processing method intercepting. Exception wrapping. And fix memory leaks.
 * <p/>
 * Only for RuleService internal use.
 *
 * @author Marat Kamalov
 */
public final class ServiceInvocationAdvice extends AbstractOpenLMethodHandler<Method, Method> implements Ordered {

    private final Logger log = LoggerFactory.getLogger(ServiceInvocationAdvice.class);

    private static final String MSG_SEPARATOR = "; ";

    private final Map<Method, List<ServiceMethodBeforeAdvice>> beforeInterceptors = new HashMap<>();
    private final Map<Method, List<ServiceMethodAfterAdvice<?>>> afterInterceptors = new HashMap<>();
    private final Map<Method, ServiceMethodAroundAdvice<?>> aroundInterceptors = new HashMap<>();
    private final Map<Method, ServiceExtraMethodHandler<?>> serviceExtraMethodAnnotations = new HashMap<>();

    private final Object serviceTarget;
    private final Class<?> serviceTargetClass;
    private final Class<?> serviceClass;
    private final ClassLoader serviceClassLoader;
    private final IOpenClass openClass;
    private final Collection<ServiceInvocationAdviceListener> serviceMethodAdviceListeners;
    private final Map<Method, IOpenMember> openMemberMap = new HashMap<>();
    private final Map<Class<?>, CustomSpreadsheetResultOpenClass> mapClassToSprOpenClass;
    private final Map<Method, Method> methodMap = new HashMap<>();
    private final RulesDeploy rulesDeploy;
    private final SpreadsheetResultBeanPropertyNamingStrategy sprBeanPropertyNamingStrategy;

    public ServiceInvocationAdvice(IOpenClass openClass,
            Object serviceTarget,
            Class<?> serviceTargetClass,
            Class<?> serviceClass,
            ClassLoader serviceClassLoader,
            Collection<ServiceInvocationAdviceListener> serviceMethodAdviceListeners,
            RulesDeploy rulesDeploy) {
        this.serviceTarget = serviceTarget;
        this.serviceClass = serviceClass;
        this.serviceTargetClass = serviceTargetClass;
        this.serviceClassLoader = serviceClassLoader;
        this.openClass = openClass;
        this.serviceMethodAdviceListeners = serviceMethodAdviceListeners != null ? new ArrayList<>(
            serviceMethodAdviceListeners) : new ArrayList<>();
        this.mapClassToSprOpenClass = initMapClassToSprOpenClass();
        this.rulesDeploy = rulesDeploy;
        PropertyNamingStrategy propertyNamingStrategy = ProjectJacksonObjectMapperFactoryBean
            .extractPropertyNamingStrategy(rulesDeploy, serviceClassLoader);
        if (propertyNamingStrategy instanceof SpreadsheetResultBeanPropertyNamingStrategy) {
            this.sprBeanPropertyNamingStrategy = (SpreadsheetResultBeanPropertyNamingStrategy) propertyNamingStrategy;
        } else {
            this.sprBeanPropertyNamingStrategy = null;
        }
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

    private Map<Class<?>, CustomSpreadsheetResultOpenClass> initMapClassToSprOpenClass() {
        XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
        Map<Class<?>, CustomSpreadsheetResultOpenClass> mapClassToCustomSpreadsheetResultOpenClass = new HashMap<>();
        for (IOpenClass type : xlsModuleOpenClass.getTypes()) {
            if (type instanceof CustomSpreadsheetResultOpenClass) {
                CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) type;
                if (customSpreadsheetResultOpenClass.isGenerateBeanClass()) {
                    mapClassToCustomSpreadsheetResultOpenClass.put(customSpreadsheetResultOpenClass.getBeanClass(),
                        customSpreadsheetResultOpenClass);
                }
            }
        }
        for (CombinedSpreadsheetResultOpenClass combinedSpreadsheetResultOpenClass : xlsModuleOpenClass
            .getCombinedSpreadsheetResultOpenClasses()) {
            if (combinedSpreadsheetResultOpenClass.isGenerateBeanClass()) {
                mapClassToCustomSpreadsheetResultOpenClass.put(combinedSpreadsheetResultOpenClass.getBeanClass(),
                    combinedSpreadsheetResultOpenClass);
            }
        }
        CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass = xlsModuleOpenClass
            .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
            .toCustomSpreadsheetResultOpenClass();
        if (customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass.isGenerateBeanClass()) {
            mapClassToCustomSpreadsheetResultOpenClass.put(
                customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass.getBeanClass(),
                customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass);
        }
        return Collections.unmodifiableMap(mapClassToCustomSpreadsheetResultOpenClass);
    }

    private void init() {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(serviceClassLoader);
            for (Method method : serviceClass.getMethods()) {
                Annotation[] methodAnnotations = method.getAnnotations();
                Pair<IOpenMember, Class<?>[]> openMemberResolved = findIOpenMember(method);
                IOpenMember openMember = openMemberResolved.getLeft();
                openMemberMap.put(method, openMember);
                if (openMember != null) {
                    Method serviceTargetMethod = MethodUtil.getMatchingAccessibleMethod(serviceTargetClass,
                        method.getName(),
                        openMemberResolved.getRight());
                    methodMap.put(method, serviceTargetMethod);
                }

                for (Annotation annotation : methodAnnotations) {
                    checkForBeforeInterceptor(method, openMember, annotation);
                    checkForAfterInterceptor(method, openMember, annotation);
                    checkForAroundInterceptor(method, openMember, annotation);
                    checkForServiceExtraMethodAnnotation(method, openMember, annotation);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private void processAware(Object o, IOpenMember openMember) {
        try {
            AnnotationUtils.inject(o, InjectOpenClass.class, (e) -> openClass);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to inject '{}' class instance.", IOpenClass.class.getTypeName());
        }
        try {
            AnnotationUtils.inject(o, InjectOpenMember.class, (e) -> openMember);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to inject '{}' class instance.", IOpenMember.class.getTypeName());
        }
        try {
            AnnotationUtils.inject(o, InjectRulesDeploy.class, (e) -> rulesDeploy);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to inject '{}' class instance.", RulesDeploy.class.getTypeName());
        }
        try {
            AnnotationUtils.inject(o, InjectServiceClassLoader.class, (e) -> serviceClassLoader);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to inject '{}' class instance.", ClassLoader.class.getTypeName());
        }
        if (o instanceof IOpenClassAware) {
            ((IOpenClassAware) o).setIOpenClass(openClass);
        }
        if (o instanceof IOpenMemberAware) {
            if (openMember != null) {
                ((IOpenMemberAware) o).setIOpenMember(openMember);
            }
        }
        if (o instanceof ServiceClassLoaderAware) {
            ((ServiceClassLoaderAware) o).setServiceClassLoader(serviceClassLoader);
        }
        if (o instanceof RulesDeployAware) {
            ((RulesDeployAware) o).setRulesDeploy(rulesDeploy);
        }
    }

    private Pair<IOpenMember, Class<?>[]> findIOpenMember(Method method) {
        for (Class<?> clazz : serviceTarget.getClass().getInterfaces()) {
            try {
                Class<?>[] parameterTypes = method.getParameterTypes();
                int i = 0;
                for (Parameter parameter : method.getParameters()) {
                    if (parameter.isAnnotationPresent(ExternalParam.class)) {
                        parameterTypes[i] = null;
                    } else if (parameter.isAnnotationPresent(BeanToSpreadsheetResultConvert.class)) {
                        Class<?> t = parameterTypes[i];
                        int dim = 0;
                        while (t.isArray()) {
                            t = t.getComponentType();
                            dim++;
                        }
                        if (t.isAnnotationPresent(SpreadsheetResultBeanClass.class)) {
                            parameterTypes[i] = dim > 0 ? Array.newInstance(SpreadsheetResult.class, dim).getClass()
                                                        : SpreadsheetResult.class;
                        }
                    }
                    i++;
                }
                parameterTypes = Arrays.stream(parameterTypes).filter(Objects::nonNull).toArray(Class<?>[]::new);
                Method m = clazz.getMethod(method.getName(), parameterTypes);
                return Pair.of(RuleServiceOpenLServiceInstantiationHelper.getOpenMember(m, serviceTarget),
                    parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return Pair.of(null, null);
    }

    private void checkForAroundInterceptor(Method method, IOpenMember openMember, Annotation annotation) {
        if (annotation instanceof ServiceCallAroundInterceptor) {
            Class<? extends ServiceMethodAroundAdvice<?>> interceptorClass = ((ServiceCallAroundInterceptor) annotation)
                .value();
            try {
                ServiceMethodAroundAdvice<?> aroundInterceptor = interceptorClass.newInstance();
                processAware(aroundInterceptor, openMember);
                aroundInterceptors.put(method, aroundInterceptor);
            } catch (Exception e) {
                throw new RuleServiceRuntimeException(String.format(
                    "Failed to instantiate 'around' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                    MethodUtil.printQualifiedMethodName(method),
                    interceptorClass.getTypeName()), e);
            }
        }
    }

    private void checkForBeforeInterceptor(Method method, IOpenMember openMember, Annotation annotation) {
        if (annotation instanceof ServiceCallBeforeInterceptor) {
            Class<? extends ServiceMethodBeforeAdvice>[] interceptorClasses = ((ServiceCallBeforeInterceptor) annotation)
                .value();
            List<ServiceMethodBeforeAdvice> interceptors = beforeInterceptors.computeIfAbsent(method,
                e -> new ArrayList<>());
            for (Class<? extends ServiceMethodBeforeAdvice> interceptorClass : interceptorClasses) {
                try {
                    ServiceMethodBeforeAdvice preInterceptor = interceptorClass.newInstance();
                    processAware(preInterceptor, openMember);
                    interceptors.add(preInterceptor);
                } catch (Exception e) {
                    throw new RuleServiceRuntimeException(String.format(
                        "Failed to instantiate 'before' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                        MethodUtil.printQualifiedMethodName(method),
                        interceptorClass.getTypeName()), e);
                }
            }
        }
    }

    private void checkForServiceExtraMethodAnnotation(Method method, IOpenMember openMember, Annotation annotation) {
        if (annotation instanceof ServiceExtraMethod) {
            Class<? extends ServiceExtraMethodHandler<?>> serviceExtraMethodHandlerClass = ((ServiceExtraMethod) annotation)
                .value();
            try {
                ServiceExtraMethodHandler<?> serviceExtraMethodHandler = serviceExtraMethodHandlerClass.newInstance();
                processAware(serviceExtraMethodHandler, openMember);
                serviceExtraMethodAnnotations.put(method, serviceExtraMethodHandler);
            } catch (Exception e) {
                throw new RuleServiceRuntimeException(String.format(
                    "Failed to instantiate service method handler for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                    MethodUtil.printQualifiedMethodName(method),
                    serviceExtraMethodHandlerClass.getTypeName()), e);
            }
        }
    }

    private void checkForAfterInterceptor(Method method, IOpenMember openMember, Annotation annotation) {
        if (annotation instanceof ServiceCallAfterInterceptor) {
            Class<? extends ServiceMethodAfterAdvice<?>>[] interceptorClasses = ((ServiceCallAfterInterceptor) annotation)
                .value();
            List<ServiceMethodAfterAdvice<?>> interceptors = afterInterceptors.computeIfAbsent(method,
                e -> new ArrayList<>());
            for (Class<? extends ServiceMethodAfterAdvice<?>> interceptorClass : interceptorClasses) {
                try {
                    ServiceMethodAfterAdvice<?> postInterceptor = interceptorClass.newInstance();
                    processAware(postInterceptor, openMember);
                    interceptors.add(postInterceptor);
                } catch (Exception e) {
                    throw new RuleServiceRuntimeException(String.format(
                        "Failed to instantiate 'afterReturning' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                        MethodUtil.printQualifiedMethodName(method),
                        interceptorClass.getTypeName()), e);
                }
            }
        }
    }

    private void beforeInvocation(Method interfaceMethod, IOpenMember openMember, Object... args) throws Throwable {
        List<ServiceMethodBeforeAdvice> preInterceptors = beforeInterceptors.get(interfaceMethod);
        if (preInterceptors != null) {
            for (ServiceMethodBeforeAdvice interceptor : preInterceptors) {
                invokeBeforeServiceMethodAdviceOnListeners(interceptor, interfaceMethod, openMember, args, null, null);
                interceptor.before(interfaceMethod, serviceTarget, args);
                invokeAfterServiceMethodAdviceOnListeners(interceptor, interfaceMethod, openMember, args, null, null);
            }
        }
    }

    private Object serviceExtraMethodInvoke(Method interfaceMethod,
            Object serviceBean,
            Object... args) throws Exception {
        ServiceExtraMethodHandler<?> serviceExtraMethodHandler = serviceExtraMethodAnnotations.get(interfaceMethod);
        if (serviceExtraMethodHandler != null) {
            return serviceExtraMethodHandler.invoke(interfaceMethod, serviceBean, args);
        }
        throw new OpenLRuntimeException("Service method advice is not found.");
    }

    private Object afterInvocation(Method interfaceMethod,
            IOpenMember openMember,
            Object result,
            Exception t,
            Object... args) throws Exception {
        List<ServiceMethodAfterAdvice<?>> postInterceptors = afterInterceptors.get(interfaceMethod);
        Object ret = result;
        if (postInterceptors != null && !postInterceptors.isEmpty()) {
            for (ServiceMethodAfterAdvice<?> interceptor : postInterceptors) {
                invokeBeforeServiceMethodAdviceOnListeners(interceptor, interfaceMethod, openMember, args, result, t);
                if (t == null) {
                    ret = interceptor.afterReturning(interfaceMethod, ret, args);
                } else {
                    try {
                        ret = interceptor.afterThrowing(interfaceMethod, t, args);
                        t = null;
                    } catch (Exception e) {
                        t = e;
                    }
                }
                invokeAfterServiceMethodAdviceOnListeners(interceptor, interfaceMethod, openMember, args, result, t);
            }
        }
        if (t != null) {
            throw t;
        }
        return ret;
    }

    private void invokeAfterServiceMethodAdviceOnListeners(ServiceMethodAdvice interceptor,
            Method interfaceMethod,
            IOpenMember openMember,
            Object[] args,
            Object ret,
            Exception ex) {
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            listener.afterServiceMethodAdvice(interceptor,
                interfaceMethod,
                args,
                ret,
                ex,
                e -> processAware(e, openMember));
        }
    }

    private void invokeBeforeServiceMethodAdviceOnListeners(ServiceMethodAdvice interceptor,
            Method interfaceMethod,
            IOpenMember openMember,
            Object[] args,
            Object ret,
            Exception ex) {
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            listener.beforeServiceMethodAdvice(interceptor,
                interfaceMethod,
                args,
                ret,
                ex,
                e -> processAware(e, openMember));
        }
    }

    @Override
    public Object invoke(Method calledMethod, Object[] args) {
        String methodName = calledMethod.getName();
        Class<?>[] parameterTypes = calledMethod.getParameterTypes();
        Object result = null;
        IOpenMember openMember = openMemberMap.get(calledMethod);
        try {
            Method beanMethod = null;
            if (!calledMethod.isAnnotationPresent(ServiceExtraMethod.class)) {
                beanMethod = methodMap.get(calledMethod);
                if (beanMethod == null) {
                    throw new OpenLRuntimeException(String.format(
                        "Called method is not found in the service bean. Please, check that excel file contains method '%s'.",
                        MethodUtil.printMethod(methodName, parameterTypes)));
                }
            }
            try {
                ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(serviceClassLoader);
                    beforeInvocation(calledMethod, openMember, args);
                    ServiceMethodAroundAdvice<?> serviceMethodAroundAdvice = aroundInterceptors.get(calledMethod);
                    Exception ex = null;
                    if (serviceMethodAroundAdvice != null) {
                        invokeBeforeServiceMethodAdviceOnListeners(serviceMethodAroundAdvice,
                            calledMethod,
                            openMember,
                            args,
                            null,
                            null);
                        try {
                            args = processArguments(calledMethod, beanMethod, args);
                            result = serviceMethodAroundAdvice.around(calledMethod, beanMethod, serviceTarget, args);
                        } catch (Exception e) {
                            ex = e;
                        } finally {
                            invokeAfterServiceMethodAdviceOnListeners(serviceMethodAroundAdvice,
                                calledMethod,
                                openMember,
                                args,
                                result,
                                ex);
                        }
                    } else {
                        invokeBeforeMethodInvocationOnListeners(calledMethod, openMember, args);
                        try {
                            if (beanMethod != null) {
                                args = processArguments(calledMethod, beanMethod, args);
                                result = beanMethod.invoke(serviceTarget, args);
                            } else {
                                result = serviceExtraMethodInvoke(calledMethod, serviceTarget, args);
                            }
                        } catch (InvocationTargetException | UndeclaredThrowableException e) {
                            Throwable t = extractInvocationTargetException(e);
                            if (t instanceof Exception) {
                                ex = (Exception) t;
                                ex.addSuppressed(e);
                            } else {
                                ex = e;
                            }
                        } catch (Exception e) {
                            ex = e;
                        } finally {
                            invokeAfterMethodInvocationOnListeners(calledMethod, openMember, args, result, ex);
                        }
                    }
                    result = afterInvocation(calledMethod, openMember, result, ex, args);
                    // repack result if arrays inside it doesn't have the returnType as interfaceMethod
                    if (calledMethod.getReturnType().isArray()) {
                        result = ArrayUtils.repackArray(result, calledMethod.getReturnType());
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(oldClassLoader);
                }
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                throw new Exception(t);
            }
            return result;
        } catch (Exception t) {
            Pair<ExceptionType, ExceptionDetails> p = getExceptionDetailAndType(t, sprBeanPropertyNamingStrategy);
            if (ExceptionType.isServerError(p.getLeft())) {
                log.error(p.getRight().getMessage(), t);
            }
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
                    "Service bean does not implement IEngineWrapper interface. Please, don't use deprecated static wrapper classes.");
            }
        }
    }

    private Object[] processArguments(Method interfaceMethod, Method beanMethod, Object[] args) {
        Object[] newArgs = new Object[beanMethod.getParameterCount()];
        int i = 0;
        int j = 0;
        for (Parameter parameter : interfaceMethod.getParameters()) {
            if (!parameter.isAnnotationPresent(ExternalParam.class)) {
                if (parameter.isAnnotationPresent(BeanToSpreadsheetResultConvert.class)) {
                    newArgs[j++] = SpreadsheetResult.convertBeansToSpreadsheetResults(args[i], mapClassToSprOpenClass);
                } else {
                    newArgs[j++] = args[i];
                }
            }
            i++;
        }
        return newArgs;
    }

    private void invokeAfterMethodInvocationOnListeners(Method interfaceMethod,
            IOpenMember openMember,
            Object[] args,
            Object result,
            Exception ex) {
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            listener.afterMethodInvocation(interfaceMethod, args, result, ex, e -> processAware(e, openMember));
        }
    }

    private void invokeBeforeMethodInvocationOnListeners(Method interfaceMethod,
            IOpenMember openMember,
            Object[] args) {
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            listener.beforeMethodInvocation(interfaceMethod, args, null, null, e -> processAware(e, openMember));
        }
    }

    private static Throwable extractInvocationTargetException(Throwable e) {
        Throwable t = e;
        while (t instanceof InvocationTargetException || t instanceof UndeclaredThrowableException) {
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            }
            if (t instanceof UndeclaredThrowableException) {
                t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
            }
        }
        return t;
    }

    public static Pair<ExceptionType, ExceptionDetails> getExceptionDetailAndType(Exception ex,
            SpreadsheetResultBeanPropertyNamingStrategy sprBeanPropertyNamingStrategy) {
        Throwable t = ex;

        ExceptionType type = ExceptionType.SYSTEM;
        ExceptionDetails exceptionDetails = new ExceptionDetails(ex.getMessage());

        boolean f = true;
        while (f) {
            t = extractInvocationTargetException(t);
            if (t instanceof OpenLUserRuntimeException) {
                type = ExceptionType.USER_ERROR;
                if (t instanceof OpenLUserDetailedRuntimeException) {
                    OpenLUserDetailedRuntimeException uex = (OpenLUserDetailedRuntimeException) t;
                    if (uex.getBody() instanceof OpenLUserDetailedRuntimeException.Body) {
                        var body = (OpenLUserDetailedRuntimeException.Body) uex.getBody();
                        exceptionDetails = new ExceptionDetails(body.getCode(), body.getMessage());
                    } else {
                        Object body = SpreadsheetResult.convertSpreadsheetResult(uex.getBody(),
                            sprBeanPropertyNamingStrategy);
                        exceptionDetails = new ExceptionDetails(body);
                    }
                } else {
                    exceptionDetails = new ExceptionDetails(t.getMessage());
                }
            } else if (t instanceof OutsideOfValidDomainException) {
                type = ExceptionType.VALIDATION;
                exceptionDetails = new ExceptionDetails(((OutsideOfValidDomainException) t).getOriginalMessage());
            } else if (t instanceof OpenLRuntimeException) {
                type = ExceptionType.RULES_RUNTIME;
                exceptionDetails = new ExceptionDetails(((OpenLRuntimeException) t).getOriginalMessage());
            } else if (t instanceof OpenLCompilationException || t instanceof RuleServiceOpenLCompilationException) {
                type = ExceptionType.COMPILATION;
                exceptionDetails = new ExceptionDetails(t.getMessage());
            }
            if (t.getCause() == null) {
                f = false;
            } else {
                t = t.getCause();
            }
        }
        return new ImmutablePair<>(type, exceptionDetails);
    }

    private String getExceptionMessage(Method method, Throwable ex, Object... args) {
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
                argsValues.append(arg);

            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("During OpenL rule execution exception was occurred. Method name is '".toUpperCase());
        sb.append(method.getName());
        sb.append("'. Arguments types are: ");
        sb.append(argsTypes);
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
            if ((t instanceof OpenLException) && t.getMessage() != null) {
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

    @Override
    public Object getTarget() {
        return serviceTarget;
    }

    @Override
    public Method getTargetMember(Method key) {
        return methodMap.get(key);
    }
}
