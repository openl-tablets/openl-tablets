package org.openl.rules.ruleservice.core;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RuntimeContextInstantiationStrategyEnhancer;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancer;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.interceptors.DynamicInterfaceAnnotationEnhancerHelper;
import org.openl.rules.ruleservice.core.interceptors.ServiceInvocationAdvice;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.rules.ruleservice.publish.RuleServiceInstantiationStrategyFactory;
import org.openl.rules.ruleservice.publish.RuleServiceInstantiationStrategyFactoryImpl;
import org.openl.rules.ruleservice.publish.lazy.CompiledOpenClassCache;
import org.openl.runtime.IEngineWrapper;
import org.openl.types.IOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;

/**
 * Default implementation of RuleServiceOpenLServiceInstantiationFactory. Depend on RuleLoader.
 *
 * @author Marat Kamalov
 */
public class RuleServiceOpenLServiceInstantiationFactoryImpl implements RuleServiceInstantiationFactory {
    private final Logger log = LoggerFactory.getLogger(RuleServiceOpenLServiceInstantiationFactoryImpl.class);

    private RuleServiceLoader ruleServiceLoader;

    private RuleServiceInstantiationStrategyFactory instantiationStrategyFactory = new RuleServiceInstantiationStrategyFactoryImpl();

    private Map<String, Object> externalParameters;

    private Map<DeploymentDescription, RuleServiceDeploymentRelatedDependencyManager> dependencyManagerMap = new HashMap<>();

    private void initService(ServiceDescription serviceDescription,
            RuleServiceDeploymentRelatedDependencyManager dependencyManager,
            OpenLService service) throws RuleServiceInstantiationException,
                                  RulesInstantiationException,
                                  ClassNotFoundException {
        RulesInstantiationStrategy instantiationStrategy = instantiationStrategyFactory.getStrategy(serviceDescription,
            dependencyManager);
        Map<String, Object> parameters = ProjectExternalDependenciesHelper
            .getExternalParamsWithProjectDependencies(externalParameters, service.getModules());
        instantiationStrategy.setExternalParameters(parameters);

        if (service.isProvideVariations()) {
            instantiationStrategy = new VariationInstantiationStrategyEnhancer(instantiationStrategy);
        }
        if (service.isProvideRuntimeContext()) {
            instantiationStrategy = new RuntimeContextInstantiationStrategyEnhancer(instantiationStrategy);
        }
        compileOpenClass(service, instantiationStrategy);
        resolveServiceClassLoader(service, instantiationStrategy);
        Object serviceTarget = resolveInterfaceAndClassLoader(service, serviceDescription, instantiationStrategy);
        resolveRmiInterface(service);
        instantiateServiceBean(service, serviceTarget, service.getClassLoader());
    }

    private void resolveServiceClassLoader(OpenLService service,
            RulesInstantiationStrategy instantiationStrategy) throws RulesInstantiationException,
                                                              RuleServiceInstantiationException {
        ClassLoader moduleGeneratedClassesClassLoader = ((XlsModuleOpenClass) service.getOpenClass())
            .getClassGenerationClassLoader();
        OpenLBundleClassLoader openLBundleClassLoader = new OpenLBundleClassLoader(null);
        openLBundleClassLoader.addClassLoader(moduleGeneratedClassesClassLoader);
        openLBundleClassLoader.addClassLoader(instantiationStrategy.getClassLoader());
        service.setClassLoader(openLBundleClassLoader);

    }

    private void compileOpenClass(OpenLService service,
            RulesInstantiationStrategy instantiationStrategy) throws RulesInstantiationException {
        CompiledOpenClass compiledOpenClass = instantiationStrategy.compile();
        service.setOpenClass(compiledOpenClass.getOpenClass());
    }

    private void instantiateServiceBean(OpenLService service,
            Object serviceTarget,
            ClassLoader classLoader) throws RuleServiceInstantiationException {
        Class<?> serviceClass = service.getServiceClass();

        ProxyFactory factory = new ProxyFactory();
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            ServiceInvocationAdvice serviceInvocationAdvice = new ServiceInvocationAdvice(service.getOpenClass(),
                serviceTarget,
                serviceClass,
                classLoader);
            factory.addAdvice(serviceInvocationAdvice);
            if (serviceClass.isInterface()) {
                factory.addInterface(serviceClass);
                if (!service.isProvideRuntimeContext()) {
                    factory.addInterface(IEngineWrapper.class);
                }
            } else {
                // deprecated approach with wrapper: service class is not
                // interface
                factory.setTarget(serviceTarget);
                factory.setProxyTargetClass(!Proxy.isProxyClass(serviceTarget.getClass()));
            }

            Object proxyServiceBean = factory.getProxy();
            service.setServiceBean(proxyServiceBean);
        } catch (Exception t) {
            throw new RuleServiceRuntimeException("Failed to create a proxy for service target object.", t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private Object resolveInterfaceAndClassLoader(OpenLService service,
            ServiceDescription serviceDescription,
            RulesInstantiationStrategy instantiationStrategy) throws RuleServiceInstantiationException,
                                                              RulesInstantiationException,
                                                              ClassNotFoundException {
        String serviceClassName = service.getServiceClassName();
        Class<?> serviceClass = null;

        if (serviceClassName != null) {
            try {
                serviceClass = service.getClassLoader().loadClass(serviceClassName.trim());
                Class<?> interfaceForInstantiationStrategy = RuleServiceInstantiationFactoryHelper
                    .getInterfaceForInstantiationStrategy(serviceClass, instantiationStrategy.getClassLoader());
                instantiationStrategy.setServiceClass(interfaceForInstantiationStrategy);
                service.setServiceClass(serviceClass);
                return instantiationStrategy.instantiate();
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                log.error("Failed to load service class '{}'.", serviceClassName, e);
            }
        }
        log.info("Service class is undefined for service '{}'. Generated interface will be used.", service.getName());
        Class<?> instanceClass = instantiationStrategy.getInstanceClass();
        Object serviceTarget = instantiationStrategy.instantiate();
        serviceClass = processGeneratedServiceClass(serviceDescription,
            service.getOpenClass(),
            instanceClass,
            serviceTarget,
            service.getClassLoader());
        service.setServiceClassName(null); // Generated class is used.
        service.setServiceClass(serviceClass);
        return serviceTarget;
    }

    private void resolveRmiInterface(OpenLService service) throws RuleServiceInstantiationException,
                                                           ClassNotFoundException {
        String rmiServiceClassName = service.getRmiServiceClassName();
        Class<?> serviceClass = null;
        ClassLoader serviceClassLoader = service.getClassLoader();
        if (rmiServiceClassName != null) {
            try {
                serviceClass = serviceClassLoader.loadClass(rmiServiceClassName.trim());
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                log.error("Failed to load rmi service class '{}'", rmiServiceClassName, e);
            }
        }
        if (serviceClass == null) {
            log.info("Service class is undefined for service '{}'. Default RMI interface has been used.",
                service.getName());
            service.setRmiServiceClassName(null); // RMI default will be used
        }
        service.setRmiServiceClass(serviceClass);
    }

    private Class<?> processGeneratedServiceClass(ServiceDescription serviceDescription,
            IOpenClass openClass,
            Class<?> serviceClass,
            Object serviceTarget,
            ClassLoader serviceClassLoader) {
        Class<?> annotatedClass = processInterceptingTemplateClass(serviceDescription,
            serviceClass,
            serviceClassLoader);
        if (annotatedClass == null) {
            throw new IllegalStateException("It must not happen!");
        }
        try {
            return RuleServiceInstantiationFactoryHelper
                .getInterfaceForService(openClass, annotatedClass, serviceTarget, serviceClassLoader);
        } catch (Exception e) {
            log.error("Failed to applying annotation template class for '{}'.", annotatedClass.getCanonicalName(), e);
        }
        return annotatedClass;
    }

    private Class<?> processInterceptingTemplateClass(ServiceDescription serviceDescription,
            Class<?> serviceClass,
            ClassLoader classLoader) {
        String clazzName = serviceDescription.getAnnotationTemplateClassName();
        if (clazzName != null) {
            try {
                Class<?> annotationTemplateClass = classLoader.loadClass(clazzName.trim());
                if (annotationTemplateClass.isInterface()) {
                    Class<?> decoratedClass = DynamicInterfaceAnnotationEnhancerHelper
                        .decorate(serviceClass, annotationTemplateClass, classLoader);
                    log.info("Interceptor template class '{}' has been used for service: {}.",
                        clazzName,
                        serviceDescription.getName());
                    return decoratedClass;
                }
                log.error(
                    "Interface is required! Intercepting template class hasn't been used! Failed to load or apply intercepting template class '{}'.",
                    clazzName);
            } catch (Exception | NoClassDefFoundError e) {
                log.error(
                    "Intercepting template class hasn't been used! Failed to load or apply intercepting template class '{}'.",
                    clazzName,
                    e);
            }
        }
        return serviceClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenLService createService(final ServiceDescription serviceDescription) {
        log.debug("Resoliving modules for service '{}'.", serviceDescription.getName());
        Collection<Module> modules = serviceDescription.getModules();

        OpenLService.OpenLServiceBuilder builder = new OpenLService.OpenLServiceBuilder();
        builder.setName(serviceDescription.getName())
            .setUrl(serviceDescription.getUrl())
            .setServiceClassName(serviceDescription.getServiceClassName())
            .setRmiServiceClassName(serviceDescription.getRmiServiceClassName())
            .setRmiName(serviceDescription.getRmiName())
            .setProvideRuntimeContext(serviceDescription.isProvideRuntimeContext())
            .setProvideVariations(serviceDescription.isProvideVariations())
            .addModules(modules);

        if (serviceDescription.getPublishers() != null) {
            for (String key : serviceDescription.getPublishers()) {
                builder.addPublisher(key);
            }
        }

        final OpenLService openLService = builder.build(new AbstractOpenLServiceInitializer() {
            @Override
            public void init(OpenLService openLService) throws RuleServiceInstantiationException {
                try {
                    initService(serviceDescription, getDependencyManager(serviceDescription), openLService);
                } catch (RuleServiceInstantiationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuleServiceInstantiationException(
                        String.format("Failed to initialiaze OpenL service '%s'.", openLService.getName()),
                        e);
                }
            }
        });
        return openLService;
    }

    public RuleServiceLoader getRuleServiceLoader() {
        return ruleServiceLoader;
    }

    public void setRuleServiceLoader(RuleServiceLoader ruleServiceLoader) {
        if (ruleServiceLoader == null) {
            throw new IllegalArgumentException("rulesLoader arg must not be null.");
        }
        this.ruleServiceLoader = ruleServiceLoader;
    }

    public RuleServiceInstantiationStrategyFactory getInstantiationStrategyFactory() {
        return instantiationStrategyFactory;
    }

    public void setInstantiationStrategyFactory(RuleServiceInstantiationStrategyFactory instantiationStrategyFactory) {
        if (instantiationStrategyFactory == null) {
            throw new IllegalArgumentException("instantiationStrategyFactory arg must not be null.");
        }
        this.instantiationStrategyFactory = instantiationStrategyFactory;
    }

    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    public void setExternalParameters(Map<String, Object> externalParameters) {
        this.externalParameters = externalParameters;
    }

    @Override
    public void clean(ServiceDescription serviceDescription) {
        dependencyManagerMap.remove(serviceDescription.getDeployment());
        CompiledOpenClassCache.getInstance().removeAll(serviceDescription.getDeployment());
    }

    private RuleServiceDeploymentRelatedDependencyManager getDependencyManager(ServiceDescription serviceDescription) {
        RuleServiceDeploymentRelatedDependencyManager dependencyManager;
        DeploymentDescription deployment = serviceDescription.getDeployment();
        if (dependencyManagerMap.containsKey(deployment)) {
            dependencyManager = dependencyManagerMap.get(deployment);
        } else {
            boolean isLazy = false;
            if (instantiationStrategyFactory instanceof RuleServiceInstantiationStrategyFactoryImpl) {
                isLazy = ((RuleServiceInstantiationStrategyFactoryImpl) instantiationStrategyFactory).isLazy();
            }
            ClassLoader rootClassLoader = RuleServiceOpenLServiceInstantiationFactoryImpl.class.getClassLoader();
            dependencyManager = new RuleServiceDeploymentRelatedDependencyManager(deployment,
                ruleServiceLoader,
                rootClassLoader,
                isLazy);
            dependencyManager.setExternalParameters(externalParameters);
            dependencyManagerMap.put(deployment, dependencyManager);
        }
        return dependencyManager;
    }

}
