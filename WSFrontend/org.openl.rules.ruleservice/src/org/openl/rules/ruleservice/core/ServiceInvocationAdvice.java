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
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import org.openl.binding.MethodUtil;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.calc.CombinedSpreadsheetResultOpenClass;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultBeanPropertyNamingStrategy;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.model.ProjectDescriptor;
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
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.runtime.AbstractOpenLMethodHandler;
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
@Slf4j
public final class ServiceInvocationAdvice extends AbstractOpenLMethodHandler<Method, Method> implements Ordered, LoggingCapability {

    public static final String OBJECT_MAPPER_ID = "serviceObjectMapper";

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
    private final @Nullable RulesDeploy rulesDeploy;
    private final @Nullable SpreadsheetResultBeanPropertyNamingStrategy sprBeanPropertyNamingStrategy;

    private final ThreadLocal<IOpenMember> iOpenMethodHolder = new ThreadLocal<>();

    final ConfigurableApplicationContext serviceContext;
    private final boolean loggingEnabled;

    private final Function<Object, String> serializer;

    // The advice takes the compiled rules, the service target and the runtime services it invokes them with.
    @SuppressWarnings("java:S107")
    public ServiceInvocationAdvice(IOpenClass openClass,
                                   Object serviceTarget,
                                   Map<Method, Method> methodMap,
                                   ClassLoader serviceClassLoader,
                                   @Nullable Collection<ServiceInvocationAdviceListener> serviceMethodAdviceListeners,
                                   ApplicationContext applicationContext,
                                   Optional<RulesDeploy> rulesDeployProvider,
                                   Optional<ProjectDescriptor> projectDescriptorProvider) {
        this.serviceTarget = serviceTarget;
        this.methodMap = methodMap;
        this.serviceClassLoader = serviceClassLoader;
        this.openClass = Objects.requireNonNull(openClass, "openClass");
        this.serviceMethodAdviceListeners = serviceMethodAdviceListeners != null ? new ArrayList<>(
                serviceMethodAdviceListeners) : new ArrayList<>();
        this.mapClassToSprOpenClass = initMapClassToSprOpenClass();
        this.rulesDeploy = rulesDeployProvider.orElse(null);
        PropertyNamingStrategy propertyNamingStrategy = ProjectJacksonObjectMapperFactoryBean
                .extractPropertyNamingStrategy(rulesDeploy, serviceClassLoader);
        if (propertyNamingStrategy instanceof SpreadsheetResultBeanPropertyNamingStrategy sprNamingStrategy) {
            this.sprBeanPropertyNamingStrategy = sprNamingStrategy;
        } else {
            this.sprBeanPropertyNamingStrategy = null;
        }

        final var mapper = configureObjectMapper(applicationContext, rulesDeploy, serviceClassLoader, (XlsModuleOpenClass) openClass);
        serializer = (Object x) -> {
            try {
                Object object;
                if (x instanceof Throwable ex) {
                    var exc = RuleServiceWrapperException.create(ex, sprBeanPropertyNamingStrategy);
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

        var configurationClass = getConfigurationClass(serviceClassLoader);
        var projectClassLoader = configurationClass != null ? configurationClass.getClassLoader() : serviceClassLoader;
        var serviceSpringContext = new AnnotationConfigApplicationContext() {
            @Override
            protected void resetCommonCaches() {
                super.resetCommonCaches();
                // Spring clears the caches of the bean class loader only: clear them for the class loader it wraps too.
                CachedIntrospectionResults.clearClassLoader(projectClassLoader);
            }
        };
        serviceSpringContext.setClassLoader(new ProxyDefiningClassLoader(projectClassLoader));
        if (configurationClass != null) {
            serviceSpringContext.register(configurationClass);
        }
        serviceSpringContext.setParent(applicationContext);
        var beanFactory = serviceSpringContext.getBeanFactory();
        beanFactory.registerSingleton("openClass", openClass);
        rulesDeployProvider.ifPresent(deployConfig -> beanFactory.registerSingleton("rulesDeploy", deployConfig));
        projectDescriptorProvider.ifPresent(projectDescriptor -> beanFactory.registerSingleton("projectDescriptor", projectDescriptor));
        beanFactory.registerSingleton("serviceClassLoader", serviceClassLoader);
        beanFactory.registerSingleton(OBJECT_MAPPER_ID, mapper);
        beanFactory.registerResolvableDependency(IOpenMember.class, (ObjectFactory<IOpenMember>) iOpenMethodHolder::get);
        serviceSpringContext.refresh();

        this.serviceContext = serviceSpringContext;

        var oldClassLoader = Thread.currentThread().getContextClassLoader();
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

    private @Nullable Class<?> getConfigurationClass(ClassLoader serviceClassLoader) {
        try {
            var configurationClass = Class.forName("spring.SpringConfig", false, serviceClassLoader);
            if (configurationClass.isAnnotationPresent(Configuration.class)) {
                return configurationClass;
            }
        } catch (ClassNotFoundException ignore) {
            // Ignore
        }
        return null;
    }

    private ObjectMapper configureObjectMapper(ApplicationContext context,
                                               @Nullable RulesDeploy rulesDeploy,
                                               ClassLoader classLoader,
                                               XlsModuleOpenClass openClass) {
        var objectMapperFactory = new ProjectJacksonObjectMapperFactoryBean();
        objectMapperFactory.setRulesDeploy(rulesDeploy);
        objectMapperFactory.setEnvironment(context.getEnvironment());
        objectMapperFactory.setXlsModuleOpenClass(openClass);
        objectMapperFactory.setClassLoader(classLoader);

        try {
            return objectMapperFactory.createJacksonObjectMapper();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Class<?>, CustomSpreadsheetResultOpenClass> initMapClassToSprOpenClass() {
        var xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
        var mapClassToCustomSpreadsheetResultOpenClass = new HashMap<Class<?>, CustomSpreadsheetResultOpenClass>();
        for (IOpenClass type : xlsModuleOpenClass.getTypes()) {
            if (type instanceof CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
                mapClassToCustomSpreadsheetResultOpenClass.put(customSpreadsheetResultOpenClass.getBeanClass(),
                        customSpreadsheetResultOpenClass);
            }
        }
        for (CombinedSpreadsheetResultOpenClass combinedSpreadsheetResultOpenClass : xlsModuleOpenClass
                .getCombinedSpreadsheetResultOpenClasses()) {
            mapClassToCustomSpreadsheetResultOpenClass.put(combinedSpreadsheetResultOpenClass.getBeanClass(),
                    combinedSpreadsheetResultOpenClass);
        }
        if (xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes() != null) {
            var customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass = xlsModuleOpenClass
                    .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                    .toCustomSpreadsheetResultOpenClass();
            mapClassToCustomSpreadsheetResultOpenClass.put(
                    customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass.getBeanClass(),
                    customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass);
        }
        return Collections.unmodifiableMap(mapClassToCustomSpreadsheetResultOpenClass);
    }

    private <T> T createBean(Method method, Class<T> interceptorClass) {
        var openMember = getOpenMember(method);
        try {
            iOpenMethodHolder.set(openMember);
            var beanFactory = serviceContext.getAutowireCapableBeanFactory();
            var o = beanFactory.createBean(interceptorClass);
            if (o instanceof IOpenClassAware aware) {
                aware.setIOpenClass(openClass);
            }
            if (o instanceof IOpenMemberAware aware && openMember != null) {
                aware.setIOpenMember(openMember);
            }
            if (o instanceof ServiceClassLoaderAware aware) {
                aware.setServiceClassLoader(serviceClassLoader);
            }
            if (o instanceof RulesDeployAware aware) {
                aware.setRulesDeploy(rulesDeploy);
            }
            return o;
        } finally {
            iOpenMethodHolder.remove();
        }
    }

    private void checkForAroundInterceptor(Method method) {
        var annotation = method.getAnnotation(ServiceCallAroundInterceptor.class);
        if (annotation != null) {
            var interceptorClass = annotation.value();
            try {
                var aroundInterceptor = createBean(method, interceptorClass);
                aroundInterceptors.put(method, aroundInterceptor);
            } catch (Exception e) {
                throw new RuleServiceRuntimeException("Failed to instantiate 'around' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.".formatted(
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
                    throw new RuleServiceRuntimeException("Failed to instantiate 'before' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.".formatted(
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

    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    class Inst implements ServiceInvocationAdviceListener.Instantiator {

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
                throw new RuleServiceRuntimeException("Failed to instantiate service method handler for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.".formatted(
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
                    afterInterceptors.computeIfAbsent(method, e -> new ArrayList<>()).add(postInterceptor);
                } catch (Exception e) {
                    throw new RuleServiceRuntimeException("Failed to instantiate 'afterReturning' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.".formatted(
                            MethodUtil.printQualifiedMethodName(method),
                            interceptorClass.getTypeName()), e);
                }
            }
        }
    }

    /**
     * Runs the before interceptors of the method in their order and returns the exception that one of them throws.
     * Returns {@code null} when all of them pass.
     *
     * <p>The interceptors after the failed one are not run. The listeners learn the exception of the failed
     * interceptor as its outcome.
     */
    private @Nullable Exception beforeInvocation(Method interfaceMethod, Object... args) throws Throwable {
        for (var interceptor : beforeInterceptors.getOrDefault(interfaceMethod, List.of())) {
            invokeBeforeServiceMethodAdviceOnListeners(interceptor, interfaceMethod, args, null, null);
            try {
                interceptor.before(interfaceMethod, serviceTarget, args);
            } catch (Exception e) {
                invokeAfterServiceMethodAdviceOnListeners(interceptor, interfaceMethod, args, null, e);
                return e;
            }
            invokeAfterServiceMethodAdviceOnListeners(interceptor, interfaceMethod, args, null, null);
        }
        return null;
    }

    /**
     * Lets each listener learn of a step of the call.
     *
     * <p>While the call fails, a listener that throws a runtime exception does not change the outcome of the call: the
     * exception is logged, and the next listeners are still called. While the call passes, the exception fails it.
     */
    private void notifyListeners(@Nullable Exception callFailure,
                                 Consumer<ServiceInvocationAdviceListener> notification) {
        if (callFailure == null) {
            serviceMethodAdviceListeners.forEach(notification);
            return;
        }
        for (var listener : serviceMethodAdviceListeners) {
            try {
                notification.accept(listener);
            } catch (RuntimeException e) {
                log.error("A listener failed on a failed service call.", e);
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

    /**
     * Runs the after interceptors of the method in their order and returns the result they give.
     *
     * <p>Each interceptor gets the result or the exception that the previous step leaves. An interceptor that
     * handles an exception passes its result on, and one that fails to handle it passes its own exception on.
     *
     * <p>An exception that {@code afterReturning} throws ends the call at once. The listeners learn the outcome of
     * every interceptor that runs, its exception included.
     */
    private @Nullable Object afterInvocation(Method interfaceMethod,
                                   @Nullable Object result,
                                   @Nullable Exception t,
                                   Object... args) throws Exception {
        var ret = result;
        for (var interceptor : afterInterceptors.getOrDefault(interfaceMethod, List.of())) {
            invokeBeforeServiceMethodAdviceOnListeners(interceptor, interfaceMethod, args, result, t);
            if (t == null) {
                try {
                    ret = interceptor.afterReturning(interfaceMethod, ret, args);
                } catch (Exception e) {
                    invokeAfterServiceMethodAdviceOnListeners(interceptor, interfaceMethod, args, result, e);
                    throw e;
                }
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
                                                           @Nullable Object ret,
                                                           @Nullable Exception ex) {
        if (serviceMethodAdviceListeners.isEmpty()) {
            return;
        }
        var inst = new Inst(interfaceMethod);
        notifyListeners(ex,
                listener -> listener.afterServiceMethodAdvice(interceptor, interfaceMethod, args, ret, ex, inst));
    }

    private void invokeBeforeServiceMethodAdviceOnListeners(ServiceMethodAdvice interceptor,
                                                            Method interfaceMethod,
                                                            Object[] args,
                                                            @Nullable Object ret,
                                                            @Nullable Exception ex) {
        if (serviceMethodAdviceListeners.isEmpty()) {
            return;
        }
        var inst = new Inst(interfaceMethod);
        notifyListeners(ex,
                listener -> listener.beforeServiceMethodAdvice(interceptor, interfaceMethod, args, ret, ex, inst));
    }

    @Override
    public @Nullable Object invoke(Method calledMethod, Object[] args) {
        var beanMethod = findBeanMethod(calledMethod);
        try {
            return invokeInServiceContext(calledMethod, beanMethod, args);
        } catch (Throwable t) {
            throw toWrapperException(t);
        }
    }

    /**
     * Invokes the service method with its interceptors, under the class loader and the logging of the service.
     *
     * <p>When a before interceptor fails, the method is not invoked and the after interceptors are not run. The
     * listeners still learn of the call, with the exception of the interceptor as its outcome, and the exception is
     * thrown.
     */
    private @Nullable Object invokeInServiceContext(Method calledMethod,
                                          @Nullable Method beanMethod,
                                          Object[] args) throws Throwable {
        var oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            LoggingHandler.setup(this);
            Thread.currentThread().setContextClassLoader(serviceClassLoader);
            var aroundAdvice = aroundInterceptors.get(calledMethod);
            var rejection = beforeInvocation(calledMethod, args);
            if (rejection != null) {
                notifyRejected(aroundAdvice, calledMethod, beanMethod, args, rejection);
                throw rejection;
            }
            var outcome = aroundAdvice != null
                    ? invokeAround(aroundAdvice, calledMethod, beanMethod, args)
                    : invokeMethod(calledMethod, beanMethod, args);
            var result = afterInvocation(calledMethod, outcome.result, outcome.error, outcome.args);
            // repack result if arrays inside it doesn't have the returnType as interfaceMethod
            if (calledMethod.getReturnType().isArray()) {
                result = ArrayUtils.repackArray(result, calledMethod.getReturnType());
            }
            return result;
        } finally {
            LoggingHandler.remove();
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    /**
     * Lets the listeners of the service method, or of its around interceptor, learn of a call that a before
     * interceptor rejected. Neither is invoked: the listeners get the rejection, before and after.
     *
     * <p>The listeners get the arguments of the call before. After, they get them without the external parameters,
     * as on an invoked call, but not converted: the method never gets them, and the conversion may fail on the
     * input the interceptor rejected.
     */
    private void notifyRejected(@Nullable ServiceMethodAroundAdvice<?> aroundAdvice,
                                Method calledMethod,
                                @Nullable Method beanMethod,
                                Object[] args,
                                Exception rejection) {
        if (serviceMethodAdviceListeners.isEmpty()) {
            return;
        }
        if (aroundAdvice != null) {
            invokeBeforeServiceMethodAdviceOnListeners(aroundAdvice, calledMethod, args, null, rejection);
            var methodArgs = argumentsOfRejectedCall(calledMethod, beanMethod, args);
            invokeAfterServiceMethodAdviceOnListeners(aroundAdvice, calledMethod, methodArgs, null, rejection);
        } else {
            invokeBeforeMethodInvocationOnListeners(calledMethod, args, rejection);
            var methodArgs = argumentsOfRejectedCall(calledMethod, beanMethod, args);
            invokeAfterMethodInvocationOnListeners(calledMethod, methodArgs, null, rejection);
        }
    }

    private static Object[] argumentsOfRejectedCall(Method calledMethod, @Nullable Method beanMethod, Object[] args) {
        return beanMethod == null ? args : methodArguments(calledMethod, beanMethod, args, (parameter, arg) -> arg);
    }

    /**
     * The result or the exception of an invoked service method, with the arguments it was invoked with.
     *
     * <p>A class, not a record: the arguments are an array, and an outcome is never compared.
     */
    @RequiredArgsConstructor
    private static final class Outcome {
        private final @Nullable Object result;
        private final @Nullable Exception error;
        private final Object[] args;
    }

    private Outcome invokeAround(ServiceMethodAroundAdvice<?> aroundAdvice,
                                 Method calledMethod,
                                 @Nullable Method beanMethod,
                                 Object[] args) throws Throwable {
        invokeBeforeServiceMethodAdviceOnListeners(aroundAdvice, calledMethod, args, null, null);
        Object result = null;
        Exception ex = null;
        try {
            if (beanMethod != null) {
                args = processArguments(calledMethod, beanMethod, args);
            }
            result = aroundAdvice.around(calledMethod, beanMethod, serviceTarget, args);
        } catch (Exception e) {
            ex = e;
        } finally {
            invokeAfterServiceMethodAdviceOnListeners(aroundAdvice, calledMethod, args, result, ex);
        }
        return new Outcome(result, ex, args);
    }

    private Outcome invokeMethod(Method calledMethod, @Nullable Method beanMethod, Object[] args) {
        invokeBeforeMethodInvocationOnListeners(calledMethod, args, null);
        Object result = null;
        Exception ex = null;
        try {
            if (beanMethod != null) {
                args = processArguments(calledMethod, beanMethod, args);
                result = beanMethod.invoke(serviceTarget, args);
            } else {
                result = serviceExtraMethodInvoke(calledMethod, serviceTarget, args);
            }
        } catch (InvocationTargetException | UndeclaredThrowableException e) {
            ex = unwrapInvocationException(e);
        } catch (Exception e) {
            ex = e;
        } finally {
            invokeAfterMethodInvocationOnListeners(calledMethod, args, result, ex);
        }
        return new Outcome(result, ex, args);
    }

    /**
     * Returns the service bean method that the called method delegates to, or {@code null} for a service extra
     * method.
     */
    private @Nullable Method findBeanMethod(Method calledMethod) {
        Method beanMethod = null;
        if (!calledMethod.isAnnotationPresent(ServiceExtraMethod.class)) {
            beanMethod = methodMap.get(calledMethod);
            if (beanMethod == null) {
                var msg = "Called method is not found in the service bean. Please, check that excel file contains method '%s'.".formatted(
                        MethodUtil.printMethod(calledMethod.getName(), calledMethod.getParameterTypes()));
                throw new RuleServiceWrapperException(msg, ExceptionType.SYSTEM);
            }
        }
        return beanMethod;
    }

    /**
     * Returns the exception that the invoked method has thrown, with the reflection wrapper added as suppressed. The
     * wrapper itself is returned when its cause is not an exception.
     */
    private static Exception unwrapInvocationException(Exception e) {
        var t = e.getCause();
        if (t instanceof Exception exception) {
            exception.addSuppressed(e);
            return exception;
        }
        return e;
    }

    /**
     * Wraps the failure of a service method call, and logs it when it is a server error.
     */
    private RuleServiceWrapperException toWrapperException(Throwable t) {
        var error = RuleServiceWrapperException.create(t, sprBeanPropertyNamingStrategy);
        if (error.getType().isServerError()) {
            log.error(error.getMessage(), t);
        }
        return error;
    }

    private Object[] processArguments(Method interfaceMethod, Method beanMethod, Object[] args) {
        return methodArguments(interfaceMethod, beanMethod, args, (parameter, arg) -> parameter
                .isAnnotationPresent(BeanToSpreadsheetResultConvert.class)
                        ? SpreadsheetResult.convertBeansToSpreadsheetResults(arg, mapClassToSprOpenClass)
                        : arg);
    }

    /**
     * Returns the arguments of the call without its external parameters, each as the given function turns it.
     */
    private static Object[] methodArguments(Method interfaceMethod,
                                            Method beanMethod,
                                            Object[] args,
                                            BiFunction<Parameter, @Nullable Object, @Nullable Object> argument) {
        var newArgs = new Object[beanMethod.getParameterCount()];
        var i = 0;
        var j = 0;
        for (Parameter parameter : interfaceMethod.getParameters()) {
            if (!parameter.isAnnotationPresent(ExternalParam.class)) {
                newArgs[j++] = argument.apply(parameter, args[i]);
            }
            i++;
        }
        return newArgs;
    }

    private void invokeAfterMethodInvocationOnListeners(Method interfaceMethod,
                                                        Object[] args,
                                                        @Nullable Object result,
                                                        @Nullable Exception ex) {
        if (serviceMethodAdviceListeners.isEmpty()) {
            return;
        }
        var inst = new Inst(interfaceMethod);
        notifyListeners(ex, listener -> listener.afterMethodInvocation(interfaceMethod, args, result, ex, inst));
    }

    private void invokeBeforeMethodInvocationOnListeners(Method interfaceMethod,
                                                         Object[] args,
                                                         @Nullable Exception ex) {
        if (serviceMethodAdviceListeners.isEmpty()) {
            return;
        }
        var inst = new Inst(interfaceMethod);
        notifyListeners(ex, listener -> listener.beforeMethodInvocation(interfaceMethod, args, null, ex, inst));
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
