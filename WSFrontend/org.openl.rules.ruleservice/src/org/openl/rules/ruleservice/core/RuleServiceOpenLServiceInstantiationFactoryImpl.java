package org.openl.rules.ruleservice.core;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RuntimeContextInstantiationStrategyEnhancer;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancer;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.interceptors.DynamicInterfaceAnnotationEnhancerHelper;
import org.openl.rules.ruleservice.core.interceptors.ServiceInvocationAdvice;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallInterceptorGroup;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.rules.ruleservice.publish.RuleServiceInstantiationStrategyFactory;
import org.openl.rules.ruleservice.publish.RuleServiceInstantiationStrategyFactoryImpl;
import org.openl.rules.ruleservice.publish.lazy.CompiledOpenClassCache;
import org.openl.runtime.IEngineWrapper;
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

    private ServiceCallInterceptorGroup[] serviceCallInterceptorGroups = new ServiceCallInterceptorGroup[] {};

    private void initService(ServiceDescription serviceDescription, RuleServiceDeploymentRelatedDependencyManager dependencyManager,
            OpenLService service) throws RuleServiceInstantiationException, RulesInstantiationException, ClassNotFoundException {
        RulesInstantiationStrategy instantiationStrategy = instantiationStrategyFactory
            .getStrategy(serviceDescription, dependencyManager);
        Map<String, Object> parameters = ProjectExternalDependenciesHelper
            .getExternalParamsWithProjectDependencies(externalParameters, service.getModules());
        instantiationStrategy.setExternalParameters(parameters);

        if (service.isProvideVariations()) {
            instantiationStrategy = new VariationInstantiationStrategyEnhancer(instantiationStrategy);
        }
        if (service.isProvideRuntimeContext()) {
            instantiationStrategy = new RuntimeContextInstantiationStrategyEnhancer(instantiationStrategy);
        }
        resolveInterfaceAndClassLoader(serviceDescription, service, instantiationStrategy);
        resolveRmiInterface(service);
        instantiateServiceBean(service, instantiationStrategy);
    }

    private void instantiateServiceBean(OpenLService service,
            RulesInstantiationStrategy instantiationStrategy) throws RuleServiceInstantiationException, RulesInstantiationException,
                                                              ClassNotFoundException {
        Class<?> serviceClass = service.getServiceClass();
        CompiledOpenClass compiledOpenClass = instantiationStrategy.compile();
        service.setOpenClass(compiledOpenClass.getOpenClass());
        Object serviceBean = instantiationStrategy.instantiate();

        ProxyFactory factory = new ProxyFactory();
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(instantiationStrategy.getClassLoader());
            factory.addAdvice(new ServiceInvocationAdvice(serviceBean,
                serviceClass,
                getServiceCallInterceptorGroups(),
                instantiationStrategy.getClassLoader()));
            if (serviceClass.isInterface()) {
                factory.addInterface(serviceClass);
                if (!service.isProvideRuntimeContext()) {
                    factory.addInterface(IEngineWrapper.class);
                }
            } else {
                // deprecated approach with wrapper: service class is not
                // interface
                factory.setTarget(serviceBean);
                factory.setProxyTargetClass(!Proxy.isProxyClass(serviceBean.getClass()));
            }

            Object proxyServiceBean = factory.getProxy();
            service.setServiceBean(proxyServiceBean);
        } catch (Exception t) {
            throw new RuleServiceRuntimeException("Failed to create a proxy of service bean object.", t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private void resolveInterfaceAndClassLoader(ServiceDescription serviceDescription, OpenLService service,
            RulesInstantiationStrategy instantiationStrategy) throws RuleServiceInstantiationException, RulesInstantiationException,
                                                              ClassNotFoundException {
        String serviceClassName = service.getServiceClassName();
        Class<?> serviceClass = null;
        ClassLoader serviceClassLoader = instantiationStrategy.getClassLoader();
        service.setClassLoader(serviceClassLoader);
        if (serviceClassName != null) {
            try {
                serviceClass = serviceClassLoader.loadClass(serviceClassName.trim());
                Class<?> interfaceForInstantiationStrategy = RuleServiceInstantiationFactoryHelper
                    .getInterfaceForInstantiationStrategy(instantiationStrategy, serviceClass);
                instantiationStrategy.setServiceClass(interfaceForInstantiationStrategy);
            } catch (ClassNotFoundException e) {
                log.error("Failed to load service class '{}'.", serviceClassName, e);
            } catch (NoClassDefFoundError e) {
                log.error("Failed to load service class '{}'.", serviceClassName, e);
            }
        }
        if (serviceClass == null) {
            log.info("Service class is undefined for service '{}'. Generated interface has been used.",
                service.getName());
            Class<?> instanceClass = instantiationStrategy.getInstanceClass();
            serviceClass = processGeneratedServiceClass(instantiationStrategy,
                serviceDescription,
                service,
                instanceClass,
                serviceClassLoader);
            service.setServiceClassName(null); // Generated class is used.
        }
        service.setServiceClass(serviceClass);
    }

    private void resolveRmiInterface(OpenLService service) throws RuleServiceInstantiationException, ClassNotFoundException {
        String rmiServiceClassName = service.getRmiServiceClassName();
        Class<?> serviceClass = null;
        ClassLoader serviceClassLoader = service.getClassLoader();
        if (rmiServiceClassName != null) {
            try {
                serviceClass = serviceClassLoader.loadClass(rmiServiceClassName.trim());
            } catch (ClassNotFoundException e) {
                log.error("Failed to load rmi service class '{}'", rmiServiceClassName, e);
            } catch (NoClassDefFoundError e) {
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

    private Class<?> processGeneratedServiceClass(RulesInstantiationStrategy instantiationStrategy,
            ServiceDescription serviceDescription,
            OpenLService service,
            Class<?> serviceClass,
            ClassLoader classLoader) {
        Class<?> resultClass = processInterceptingTemplateClassConfiguration(serviceDescription, serviceClass, classLoader);
        if (resultClass == null) {
            throw new IllegalStateException("It must not happen!");
        }
        try {
            Class<?> interfaceForService = RuleServiceInstantiationFactoryHelper
                .getInterfaceForService(instantiationStrategy, resultClass);
            return interfaceForService;
        } catch (Exception e) {
            log.error("Failed to applying intercepting template class for '{}'.", resultClass.getCanonicalName(), e);
        }
        return resultClass;
    }

    private Class<?> processInterceptingTemplateClassConfiguration(ServiceDescription serviceDescription,
            Class<?> serviceClass,
            ClassLoader classLoader) {
        String clazzName = serviceDescription.getAnnotationTemplateClassName();
        if (clazzName != null) {
            try {
                Class<?> interceptingTemplateClass = classLoader.loadClass(clazzName.trim());
                if (interceptingTemplateClass.isInterface()) {
                    Class<?> decoratedClass = DynamicInterfaceAnnotationEnhancerHelper
                        .decorate(serviceClass, interceptingTemplateClass, classLoader);
                    log.info("Interceptor template class '{}' has been used for service: {}.",
                        clazzName,
                        serviceDescription.getName());
                    return decoratedClass;
                }
                log.error(
                    "Interface is required! Intercepting template class hasn't been used! Failed to load or apply intercepting template class '{}'.",
                    clazzName);
            } catch (Exception e) {
                log.error(
                    "Intercepting template class hasn't been used! Failed to load or apply intercepting template class '{}'.",
                    clazzName,
                    e);
            } catch (NoClassDefFoundError e) {
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
    public OpenLService createService(final ServiceDescription serviceDescription) {
        log.debug("Resoliving modules for service '{}'.", serviceDescription.getName());
        Collection<Module> modules = serviceDescription.getModules();

        OpenLService.OpenLServiceBuilder builder = new OpenLService.OpenLServiceBuilder();
        builder.setName(serviceDescription.getName())
            .setUrl(serviceDescription.getUrl())
            .setServiceClassName(serviceDescription.getServiceClassName())
            .setRmiServiceClassName(serviceDescription.getRmiServiceClassName())
            .setProvideRuntimeContext(serviceDescription.isProvideRuntimeContext())
            .setProvideVariations(serviceDescription.isProvideVariations())
            .addModules(modules);

        if (serviceDescription.getPublishers() != null) {
            for (String key : serviceDescription.getPublishers()) {
                builder.addPublisher(key);
            }
        }

        final OpenLService openLService = builder.build(new AbstractOpenLServiceInitializer() {
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

    private Map<DeploymentDescription, RuleServiceDeploymentRelatedDependencyManager> dependencyManagerMap = new HashMap<>();

    public void clear(DeploymentDescription deploymentDescription) {
        dependencyManagerMap.remove(deploymentDescription);
        CompiledOpenClassCache.getInstance().removeAll(deploymentDescription);
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

    public ServiceCallInterceptorGroup[] getServiceCallInterceptorGroups() {
        return serviceCallInterceptorGroups;
    }

    public void setServiceCallInterceptorGroups(ServiceCallInterceptorGroup[] serviceCallInterceptorGroups) {
        this.serviceCallInterceptorGroups = serviceCallInterceptorGroups;
    }

}
