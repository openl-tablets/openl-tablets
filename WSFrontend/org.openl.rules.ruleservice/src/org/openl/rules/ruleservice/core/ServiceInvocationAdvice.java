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
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public final class ServiceInvocationAdvice extends AbstractOpenLMethodHandler<Method, Method> implements Ordered, LoggingCapability {

    public static final String OBJECT_MAPPER_ID = "serviceObjectMapper";
    private final Logger log = LoggerFactory.getLogger(ServiceInvocationAdvice.class);

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
                                   Optional<RulesDeploy> rulesDeployProvider,
                                   Optional<ProjectDescriptor> projectDescriptorProvider) {
        this.serviceTarget = serviceTarget;
        this.methodMap = methodMap;
        this.serviceClassLoader = serviceClassLoader;
        this.openClass = openClass;
        this.serviceMethodAdviceListeners = serviceMethodAdviceListeners != null ? new ArrayList<>(
                serviceMethodAdviceListeners) : new ArrayList<>();
        this.mapClassToSprOpenClass = initMapClassToSprOpenClass();
        this.rulesDeploy = rulesDeployProvider.orElse(null);
        PropertyNamingStrategy propertyNamingStrategy = ProjectJacksonObjectMapperFactoryBean
                .extractPropertyNamingStrategy(rulesDeploy, serviceClassLoader);
        if (propertyNamingStrategy instanceof SpreadsheetResultBeanPropertyNamingStrategy sprBeanPropertyNamingStrategy) {
            this.sprBeanPropertyNamingStrategy = sprBeanPropertyNamingStrategy;
        } else {
            this.sprBeanPropertyNamingStrategy = null;
        }

        final ObjectMapper mapper = configureObjectMapper(applicationContext, rulesDeploy, serviceClassLoader, (XlsModuleOpenClass) openClass);
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

        AnnotationConfigApplicationContext serviceContext = new AnnotationConfigApplicationContext();
        var configurationClass = getConfigurationClass(serviceClassLoader);
        if (configurationClass != null) {
            serviceContext.setClassLoader(configurationClass.getClassLoader());
            serviceContext.register(configurationClass);
        } else {
            serviceContext.setClassLoader(serviceClassLoader);
        }
        serviceContext.setParent(applicationContext);
        var beanFactory = serviceContext.getBeanFactory();
        beanFactory.registerSingleton("openClass", openClass);
        rulesDeployProvider.ifPresent(rulesDeploy -> beanFactory.registerSingleton("rulesDeploy", rulesDeploy));
        projectDescriptorProvider.ifPresent(projectDescriptor -> beanFactory.registerSingleton("projectDescriptor", projectDescriptor));
        beanFactory.registerSingleton("serviceClassLoader", serviceClassLoader);
        beanFactory.registerSingleton(OBJECT_MAPPER_ID, mapper);
        beanFactory.registerResolvableDependency(IOpenMember.class, (ObjectFactory<IOpenMember>) iOpenMethodHolder::get);
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

    private Class<?> getConfigurationClass(ClassLoader serviceClassLoader) {
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
                                               RulesDeploy rulesDeploy,
                                               ClassLoader classLoader,
                                               XlsModuleOpenClass openClass) {
        ProjectJacksonObjectMapperFactoryBean objectMapperFactory = new ProjectJacksonObjectMapperFactoryBean();
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
        XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
        Map<Class<?>, CustomSpreadsheetResultOpenClass> mapClassToCustomSpreadsheetResultOpenClass = new HashMap<>();
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
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass = xlsModuleOpenClass
                    .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                    .toCustomSpreadsheetResultOpenClass();
            mapClassToCustomSpreadsheetResultOpenClass.put(
                    customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass.getBeanClass(),
                    customSpreadsheetResultOpenClassForSpreadsheetResultOpenClass);
        }
        return Collections.unmodifiableMap(mapClassToCustomSpreadsheetResultOpenClass);
    }

    private <T> T createBean(Method method, Class<T> interceptorClass) {
        IOpenMember openMember = getOpenMember(method);
        try {
            iOpenMethodHolder.set(openMember);
            var beanFactory = serviceContext.getAutowireCapableBeanFactory();
            T o = beanFactory.createBean(interceptorClass);
            if (o instanceof IOpenClassAware aware) {
                aware.setIOpenClass(openClass);
            }
            if (o instanceof IOpenMemberAware aware) {
                if (openMember != null) {
                    aware.setIOpenMember(openMember);
                }
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
                throw new RuleServiceWrapperException(msg, ExceptionType.SYSTEM);
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
            var error = RuleServiceWrapperException.create(t, sprBeanPropertyNamingStrategy);
            if (error.getType().isServerError()) {
                log.error(error.getMessage(), t);
            }
            throw error;
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
