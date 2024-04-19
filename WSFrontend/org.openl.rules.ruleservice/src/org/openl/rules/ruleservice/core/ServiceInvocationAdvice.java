package org.openl.rules.ruleservice.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import org.openl.binding.MethodUtil;
import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenLUserRuntimeException;
import org.openl.rules.calc.CombinedSpreadsheetResultOpenClass;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.annotations.BeanToSpreadsheetResultConvert;
import org.openl.rules.ruleservice.core.annotations.ExternalParam;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;
import org.openl.rules.ruleservice.core.interceptors.IOpenClassAware;
import org.openl.rules.ruleservice.core.interceptors.IOpenMemberAware;
import org.openl.rules.ruleservice.core.interceptors.RulesDeployAware;
import org.openl.rules.ruleservice.core.interceptors.ServiceClassLoaderAware;
import org.openl.rules.ruleservice.core.interceptors.ServiceInvocationAdviceListener;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAdvice;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAroundAdvice;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;
import org.openl.rules.runtime.LoggingCapability;
import org.openl.rules.runtime.LoggingHandler;
import org.openl.rules.serialization.DefaultTypingMode;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.runtime.AbstractOpenLMethodHandler;
import org.openl.runtime.IEngineWrapper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.util.ArrayUtils;

/**
 * Advice for processing method intercepting. Exception wrapping. And fix memory leaks.
 * <p/>
 * Only for RuleService internal use.
 *
 * @author Marat Kamalov
 */
public final class ServiceInvocationAdvice extends AbstractOpenLMethodHandler<Method, Method> implements Ordered, LoggingCapability {

    private final Logger log = LoggerFactory.getLogger(ServiceInvocationAdvice.class);

    private static final String MSG_SEPARATOR = "; ";

    private final Map<Method, List<ServiceMethodBeforeAdvice>> beforeInterceptors = new HashMap<>();
    private final Map<Method, List<ServiceMethodAfterAdvice<?>>> afterInterceptors = new HashMap<>();
    private final Map<Method, ServiceMethodAroundAdvice<?>> aroundInterceptors = new HashMap<>();
    private final Map<Method, ServiceExtraMethodHandler<?>> serviceExtraMethodAnnotations = new HashMap<>();

    private final Object serviceTarget;
    private final ClassLoader serviceClassLoader;
    private final IOpenClass openClass;
    private final Collection<ServiceInvocationAdviceListener> serviceMethodAdviceListeners;
    private final Map<Class<?>, CustomSpreadsheetResultOpenClass> mapClassToSprOpenClass;
    private final Map<Method, Method> methodMap;
    private final RulesDeploy rulesDeploy;
    private final SpreadsheetResultBeanPropertyNamingStrategy sprBeanPropertyNamingStrategy;

    private final ThreadLocal<IOpenMember> iOpenMethodHolder = new ThreadLocal<>();

    final ConfigurableApplicationContext serviceContext;
    private final boolean loggingEnabled;

    private final Function<Object, String> serializer;

    public ServiceInvocationAdvice(IOpenClass openClass,
                                   Object serviceTarget,
                                   Map<Method, Method> methodMap,
                                   ClassLoader serviceClassLoader,
                                   Collection<ServiceInvocationAdviceListener> serviceMethodAdviceListeners,
                                   ApplicationContext applicationContext,
                                   RulesDeploy rulesDeploy) {
        this.serviceTarget = serviceTarget;
        this.methodMap = methodMap;
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

        final ObjectMapper mapper = configureObjectMapper(rulesDeploy, serviceClassLoader, (XlsModuleOpenClass) openClass);
        serializer = (Object x) -> {
            try {
                Object object;
                if (x instanceof Throwable) {
                    ExceptionDetails exc = getExceptionDetailAndType((Throwable) x, sprBeanPropertyNamingStrategy).getValue();
                    object = exc.getBody() != null ? exc.getBody() : exc.getMessage();
                } else {
                    object = SpreadsheetResult.convertSpreadsheetResult(x, sprBeanPropertyNamingStrategy);
                }
                return mapper.writeValueAsString(object);
            } catch (Exception ignore) {
                log.warn("Exception.", ignore);
                return x.toString();
            }
        };

        this.loggingEnabled = Boolean
                .parseBoolean(applicationContext.getEnvironment().getProperty("ruleservice.logging.enabled"));

        AnnotationConfigApplicationContext serviceContext = new AnnotationConfigApplicationContext();
        serviceContext.setClassLoader(serviceClassLoader);
        serviceContext.setParent(applicationContext);
        serviceContext.getBeanFactory().registerSingleton("openClass", openClass);
        if (rulesDeploy != null) {
            serviceContext.getBeanFactory().registerSingleton("rulesDeploy", rulesDeploy);
        }
        serviceContext.getBeanFactory().registerSingleton("serviceClassLoader", serviceClassLoader);
        serviceContext.getBeanFactory()
                .registerResolvableDependency(IOpenMember.class, (ObjectFactory<IOpenMember>) iOpenMethodHolder::get);
        try {
            Class<?> configurationClass = Class.forName("spring.SpringConfig", false, serviceClassLoader);
            if (configurationClass.isAnnotationPresent(Configuration.class)) {
                serviceContext.register(configurationClass);
            }
        } catch (ClassNotFoundException ignore) {
            // Ignore
        }
        serviceContext.refresh();

        this.serviceContext = serviceContext;

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.serviceClassLoader);
            for (Method method : methodMap.keySet()) {
                checkForBeforeInterceptor(method);
                checkForAfterInterceptor(method);
                checkForAroundInterceptor(method);
                checkForServiceExtraMethodAnnotation(method);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private ObjectMapper configureObjectMapper(RulesDeploy rulesDeploy,
                                               ClassLoader classLoader,
                                               XlsModuleOpenClass openClass) {
        ProjectJacksonObjectMapperFactoryBean objectMapperFactory = new ProjectJacksonObjectMapperFactoryBean();
        objectMapperFactory.setRulesDeploy(rulesDeploy);
        objectMapperFactory.setXlsModuleOpenClass(openClass);
        objectMapperFactory.setClassLoader(classLoader);
        // Default values from webservices. TODO this should be configurable
        objectMapperFactory.setPolymorphicTypeValidation(true);
        objectMapperFactory.setDefaultDateFormatAsString("yyyy-MM-dd'T'HH:mm:ss.SSS");
        objectMapperFactory.setCaseInsensitiveProperties(false);
        objectMapperFactory.setDefaultTypingMode(DefaultTypingMode.JAVA_LANG_OBJECT);
        objectMapperFactory.setSerializationInclusion(JsonInclude.Include.USE_DEFAULTS);

        try {
            return objectMapperFactory.createJacksonObjectMapper();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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
        if (xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes() != null) {
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass = xlsModuleOpenClass
                    .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                    .toCustomSpreadsheetResultOpenClass();
            if (customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass.isGenerateBeanClass()) {
                mapClassToCustomSpreadsheetResultOpenClass.put(
                        customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass.getBeanClass(),
                        customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass);
            }
        }
        return Collections.unmodifiableMap(mapClassToCustomSpreadsheetResultOpenClass);
    }

    private <T> T createBean(Method method, Class<T> interceptorClass) {
        IOpenMember openMember = getOpenMember(method);

        iOpenMethodHolder.set(openMember);

        var beanFactory = serviceContext.getAutowireCapableBeanFactory();

        T o = beanFactory.createBean(interceptorClass);

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
        iOpenMethodHolder.remove();
        return o;
    }

    private void checkForAroundInterceptor(Method method) {
        var annotation = method.getAnnotation(ServiceCallAroundInterceptor.class);
        if (annotation != null) {
            var interceptorClass = annotation.value();
            try {
                var aroundInterceptor = createBean(method, interceptorClass);
                aroundInterceptors.put(method, aroundInterceptor);
            } catch (Exception e) {
                throw new RuleServiceRuntimeException(String.format(
                        "Failed to instantiate 'around' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                        MethodUtil.printQualifiedMethodName(method),
                        interceptorClass.getTypeName()), e);
            }
        }
    }

    private void checkForBeforeInterceptor(Method method) {
        var annotations = method.getAnnotationsByType(ServiceCallBeforeInterceptor.class);
        for (var annotation : annotations) {
            for (var interceptorClass : annotation.value()) {
                try {
                    var preInterceptor = createBean(method, interceptorClass);
                    beforeInterceptors.computeIfAbsent(method, e -> new ArrayList<>()).add(preInterceptor);
                } catch (Exception e) {
                    throw new RuleServiceRuntimeException(String.format(
                            "Failed to instantiate 'before' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                            MethodUtil.printQualifiedMethodName(method),
                            interceptorClass.getTypeName()), e);
                }
            }
        }
    }

    @Override
    public boolean loggingEnabled() {
        return loggingEnabled;
    }

    @Override
    public Function<Object, String> serializer() {
        return serializer;
    }

    class Inst implements ServiceInvocationAdviceListener.Instantiator {
        Inst(Method method) {
            this.method = method;
        }

        final Method method;

        @Override
        public <T> T instantiate(Class<T> clazz) {
            return createBean(method, clazz);
        }
    }

    private void checkForServiceExtraMethodAnnotation(Method method) {
        var annotation = method.getAnnotation(ServiceExtraMethod.class);
        if (annotation != null) {
            var serviceExtraMethodHandlerClass = annotation.value();
            try {
                var serviceExtraMethodHandler = createBean(method, serviceExtraMethodHandlerClass);
                serviceExtraMethodAnnotations.put(method, serviceExtraMethodHandler);
            } catch (Exception e) {
                throw new RuleServiceRuntimeException(String.format(
                        "Failed to instantiate service method handler for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                        MethodUtil.printQualifiedMethodName(method),
                        serviceExtraMethodHandlerClass.getTypeName()), e);
            }
        }
    }

    private void checkForAfterInterceptor(Method method) {
        var annotations = method.getAnnotationsByType(ServiceCallAfterInterceptor.class);
        for (var annotation : annotations) {
            for (var interceptorClass : annotation.value()) {
                try {
                    var postInterceptor = createBean(method, interceptorClass);
                    ;
                    afterInterceptors.computeIfAbsent(method, e -> new ArrayList<>()).add(postInterceptor);
                } catch (Exception e) {
                    throw new RuleServiceRuntimeException(String.format(
                            "Failed to instantiate 'afterReturning' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                            MethodUtil.printQualifiedMethodName(method),
                            interceptorClass.getTypeName()), e);
                }
            }
        }
    }

    private void beforeInvocation(Method interfaceMethod, Object... args) throws Throwable {
        for (var interceptor : beforeInterceptors.getOrDefault(interfaceMethod, Collections.emptyList())) {
            invokeBeforeServiceMethodAdviceOnListeners(interceptor, interfaceMethod, args, null, null);
            interceptor.before(interfaceMethod, serviceTarget, args);
            invokeAfterServiceMethodAdviceOnListeners(interceptor, interfaceMethod, args, null, null);
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
                                   Object result,
                                   Exception t,
                                   Object... args) throws Exception {
        Object ret = result;
        for (var interceptor : afterInterceptors.getOrDefault(interfaceMethod, Collections.emptyList())) {
            invokeBeforeServiceMethodAdviceOnListeners(interceptor, interfaceMethod, args, result, t);
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
            invokeAfterServiceMethodAdviceOnListeners(interceptor, interfaceMethod, args, result, t);
        }
        if (t != null) {
            throw t;
        }
        return ret;
    }

    private void invokeAfterServiceMethodAdviceOnListeners(ServiceMethodAdvice interceptor,
                                                           Method interfaceMethod,
                                                           Object[] args,
                                                           Object ret,
                                                           Exception ex) {
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            listener.afterServiceMethodAdvice(interceptor, interfaceMethod, args, ret, ex, new Inst(interfaceMethod));
        }
    }

    private void invokeBeforeServiceMethodAdviceOnListeners(ServiceMethodAdvice interceptor,
                                                            Method interfaceMethod,
                                                            Object[] args,
                                                            Object ret,
                                                            Exception ex) {
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            listener.beforeServiceMethodAdvice(interceptor, interfaceMethod, args, ret, ex, new Inst(interfaceMethod));
        }
    }

    @Override
    public Object invoke(Method calledMethod, Object[] args) {
        String methodName = calledMethod.getName();
        Class<?>[] parameterTypes = calledMethod.getParameterTypes();
        Object result = null;
        Method beanMethod = null;
        if (!calledMethod.isAnnotationPresent(ServiceExtraMethod.class)) {
            beanMethod = getTargetMember(calledMethod);
            if (beanMethod == null) {
                var msg = String.format(
                        "Called method is not found in the service bean. Please, check that excel file contains method '%s'.",
                        MethodUtil.printMethod(methodName, parameterTypes));
                throw new RuleServiceWrapperException(new ExceptionDetails(msg), ExceptionType.SYSTEM, msg, null);
            }
        }
        try {
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                LoggingHandler.setup(this);
                Thread.currentThread().setContextClassLoader(serviceClassLoader);
                beforeInvocation(calledMethod, args);
                ServiceMethodAroundAdvice<?> serviceMethodAroundAdvice = aroundInterceptors.get(calledMethod);
                Exception ex = null;
                if (serviceMethodAroundAdvice != null) {
                    invokeBeforeServiceMethodAdviceOnListeners(serviceMethodAroundAdvice,
                            calledMethod,
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
                                args,
                                result,
                                ex);
                    }
                } else {
                    invokeBeforeMethodInvocationOnListeners(calledMethod, args);
                    try {
                        if (beanMethod != null) {
                            args = processArguments(calledMethod, beanMethod, args);
                            result = beanMethod.invoke(serviceTarget, args);
                        } else {
                            result = serviceExtraMethodInvoke(calledMethod, serviceTarget, args);
                        }
                    } catch (InvocationTargetException | UndeclaredThrowableException e) {
                        Throwable t = e.getCause();
                        if (t instanceof Exception) {
                            ex = (Exception) t;
                            ex.addSuppressed(e);
                        } else {
                            ex = e;
                        }
                    } catch (Exception e) {
                        ex = e;
                    } finally {
                        invokeAfterMethodInvocationOnListeners(calledMethod, args, result, ex);
                    }
                }
                result = afterInvocation(calledMethod, result, ex, args);
                // repack result if arrays inside it doesn't have the returnType as interfaceMethod
                if (calledMethod.getReturnType().isArray()) {
                    result = ArrayUtils.repackArray(result, calledMethod.getReturnType());
                }
            } finally {
                LoggingHandler.remove();
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        } catch (Throwable t) {
            Pair<ExceptionType, ExceptionDetails> p = getExceptionDetailAndType(t, sprBeanPropertyNamingStrategy);
            if (ExceptionType.isServerError(p.getLeft())) {
                log.error(p.getRight().getMessage(), t);
            }
            var msg = getExceptionMessage(calledMethod, t, args);
            throw new RuleServiceWrapperException(p.getRight(), p.getLeft(), msg, t);
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
        return result;
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
                                                        Object[] args,
                                                        Object result,
                                                        Exception ex) {
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            listener.afterMethodInvocation(interfaceMethod, args, result, ex, new Inst(interfaceMethod));
        }
    }

    private void invokeBeforeMethodInvocationOnListeners(Method interfaceMethod, Object[] args) {
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            listener.beforeMethodInvocation(interfaceMethod, args, null, null, new Inst(interfaceMethod));
        }
    }

    public static Pair<ExceptionType, ExceptionDetails> getExceptionDetailAndType(Throwable ex,
                                                                                  SpreadsheetResultBeanPropertyNamingStrategy sprBeanPropertyNamingStrategy) {
        Throwable t = ex;

        ExceptionType type = ExceptionType.SYSTEM;
        ExceptionDetails exceptionDetails = new ExceptionDetails(ex.getMessage());

        boolean f = true;
        while (f) {
            if (t instanceof OpenLUserRuntimeException) {
                type = ExceptionType.USER_ERROR;
                var body = ((OpenLUserRuntimeException) t).getBody();
                var convertedBody = SpreadsheetResult.convertSpreadsheetResult(body, sprBeanPropertyNamingStrategy);
                exceptionDetails = new ExceptionDetails(convertedBody);
            } else if (t instanceof OutsideOfValidDomainException) {
                type = ExceptionType.VALIDATION;
                exceptionDetails = new ExceptionDetails(((OutsideOfValidDomainException) t).getOriginalMessage());
            } else if (t instanceof OpenLRuntimeException) {
                type = ExceptionType.RULES_RUNTIME;
                exceptionDetails = new ExceptionDetails(((OpenLRuntimeException) t).getOriginalMessage());
            } else if (t instanceof OpenLCompilationException) {
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
