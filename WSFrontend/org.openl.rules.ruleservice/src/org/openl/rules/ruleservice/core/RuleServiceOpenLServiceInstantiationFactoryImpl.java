package org.openl.rules.ruleservice.core;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.dependencies.CompositeRulesProjectDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RuntimeContextInstantiationStrategyEnhancer;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancer;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.instantiation.RuleServiceRuntimeContextInstantiationStrategyEnhancer;
import org.openl.rules.ruleservice.core.interceptors.DynamicInterfaceAnnotationEnchancerHelper;
import org.openl.rules.ruleservice.core.interceptors.ServiceInvocationAdvice;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.openl.rules.ruleservice.publish.RuleServiceInstantiationStrategyFactory;
import org.openl.rules.ruleservice.publish.RuleServiceInstantiationStrategyFactoryImpl;
import org.openl.runtime.IEngineWrapper;
import org.springframework.aop.framework.ProxyFactory;

/**
 * Default implementation of RuleServiceOpenLServiceInstantiationFactory. Depend
 * on RuleLoader.
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceOpenLServiceInstantiationFactoryImpl implements RuleServiceInstantiationFactory {
    private final Log log = LogFactory.getLog(RuleServiceOpenLServiceInstantiationFactoryImpl.class);

    private RuleServiceLoader ruleServiceLoader;

    private RuleServiceInstantiationStrategyFactory instantiationStrategyFactory = new RuleServiceInstantiationStrategyFactoryImpl();

    private IDependencyManager externalDependencyManager;

    private Map<String, Object> externalParameters;

    private void initService(IDependencyManager dependencyManager, OpenLService service) throws RulesInstantiationException,
                                                                                        ClassNotFoundException {
        if (service == null) {
            throw new IllegalArgumentException("service argument can't be null");
        }

        RulesInstantiationStrategy instantiationStrategy = instantiationStrategyFactory.getStrategy(service.getModules(),
            dependencyManager);

        Map<String, Object> parameters = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(externalParameters,
            service.getModules());
        instantiationStrategy.setExternalParameters(parameters);

        if (service.isProvideVariations()) {
            instantiationStrategy = new VariationInstantiationStrategyEnhancer(instantiationStrategy);
        }
        if (service.isProvideRuntimeContext()) {
            instantiationStrategy = new RuntimeContextInstantiationStrategyEnhancer(instantiationStrategy);
            if (service.isUseRuleServiceRuntimeContext()) {
                instantiationStrategy = new RuleServiceRuntimeContextInstantiationStrategyEnhancer((RuntimeContextInstantiationStrategyEnhancer) instantiationStrategy);
            }
        }
        resolveInterface(service, instantiationStrategy);
        instantiateServiceBean(service, instantiationStrategy);
    }

    private void instantiateServiceBean(OpenLService service, RulesInstantiationStrategy instantiationStrategy) throws RulesInstantiationException,
                                                                                                               ClassNotFoundException {
        Object serviceBean = null;
        Class<?> serviceClass = service.getServiceClass();
        serviceBean = instantiationStrategy.instantiate();
        ProxyFactory factory = new ProxyFactory();
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(instantiationStrategy.getClassLoader());
            factory.addAdvice(new ServiceInvocationAdvice(serviceBean, serviceClass));
            if (serviceClass.isInterface()) {
                factory.addInterface(serviceClass);
                if (!service.isProvideRuntimeContext()) {
                    factory.addInterface(IEngineWrapper.class);
                }
            } else {
                // deprecated approach with wrapper: service class is not
                // interface
                factory.setTarget(serviceBean);
                if (!Proxy.isProxyClass(serviceBean.getClass())) {
                    factory.setProxyTargetClass(true);
                } else {
                    factory.setProxyTargetClass(false);
                }
            }

            Object proxyServiceBean = null;
            proxyServiceBean = factory.getProxy();
            service.setServiceBean(proxyServiceBean);
        } catch (Throwable t) {
            throw new RuleServiceRuntimeException("Can't create a proxy of service bean object", t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private void resolveInterface(OpenLService service, RulesInstantiationStrategy instantiationStrategy) throws RulesInstantiationException,
                                                                                                         ClassNotFoundException {
        String serviceClassName = service.getServiceClassName();
        Class<?> serviceClass = null;
        ClassLoader serviceClassLoader = instantiationStrategy.getClassLoader();
        if (serviceClassName != null) {
            try {
                serviceClass = serviceClassLoader.loadClass(serviceClassName);
                instantiationStrategy.setServiceClass(RuleServiceInstantiationFactoryHelper.getInterfaceForInstantiationStrategy(instantiationStrategy,
                    serviceClass));
            } catch (ClassNotFoundException e) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Failed to load service class with name \"%s\"", serviceClassName), e);
                }
                if (log.isInfoEnabled()) {
                    log.info(String.format("Service class is undefined of service '%s'. Generated interface will be used.",
                        service.getName()));
                }
                serviceClass = processGeneratedServiceClass(instantiationStrategy.getInstanceClass(),
                    serviceClassLoader);
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info(String.format("Service class is undefined of service '%s'. Generated interface will be used.",
                    service.getName()));
            }
            serviceClass = processGeneratedServiceClass(instantiationStrategy.getInstanceClass(), serviceClassLoader);
        }
        service.setServiceClass(serviceClass);
    }

    private Class<?> processGeneratedServiceClass(Class<?> serviceClass, ClassLoader classLoader) {
        if (serviceClass == null) {
            return null; // It shouldn't happen
        }
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
        if (serviceDescription == null) {
            if (log.isWarnEnabled()) {
                log.warn("Service description didn't find! Something wrong!");
            }
            return serviceClass;
        } else {
            String clazzName = serviceDescription.getInterceptorTemplateClassName();
            if (clazzName != null) {
                try {
                    Class<?> interceptingTemplateClass = classLoader.loadClass(clazzName);
                    Class<?> decoratedClass = DynamicInterfaceAnnotationEnchancerHelper.decorate(serviceClass,
                        interceptingTemplateClass,
                        classLoader);
                    if (log.isInfoEnabled()) {
                        log.info("Interceptor template class \"" + clazzName + "\" was used for service: " + serviceDescription.getName());
                    }
                    return decoratedClass;
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format("Intercepting template class wasn't used! Failed to load or applying intercepting template class with name \"%s\"",
                            clazzName),
                            e);
                    }
                }
            }
        }
        return serviceClass;
    }

    /** {@inheritDoc} */
    public OpenLService createService(ServiceDescription serviceDescription) throws RuleServiceInstantiationException {
        Collection<Module> modules = ruleServiceLoader.getModulesByServiceDescription(serviceDescription);

        OpenLService.OpenLServiceBuilder builder = new OpenLService.OpenLServiceBuilder();
        builder.setName(serviceDescription.getName())
            .setUrl(serviceDescription.getUrl())
            .setServiceClassName(serviceDescription.getServiceClassName())
            .setProvideRuntimeContext(serviceDescription.isProvideRuntimeContext())
            .setProvideVariations(serviceDescription.isProvideVariations())
            .setUseRuleServiceRuntimeContext(serviceDescription.isUseRuleServiceRuntimeContext())
            .addModules(modules);

        OpenLService openLService = builder.build();

        try {
            initService(getDependencyManager(serviceDescription), openLService);
        } catch (Exception e) {
            throw new RuleServiceInstantiationException(String.format("Failed to initialiaze OpenL service \"%s\"",
                openLService.getName()), e);
        }
        return openLService;
    }

    public RuleServiceLoader getRuleServiceLoader() {
        return ruleServiceLoader;
    }

    public void setRuleServiceLoader(RuleServiceLoader ruleServiceLoader) {
        if (ruleServiceLoader == null) {
            throw new IllegalArgumentException("rulesLoader arg can't be null");
        }
        this.ruleServiceLoader = ruleServiceLoader;
    }

    public IDependencyManager getExternalDependencyManager() {
        return externalDependencyManager;
    }

    public void setExternalDependencyManager(IDependencyManager externalDependencyManager) {
        this.externalDependencyManager = externalDependencyManager;
    }

    public RuleServiceInstantiationStrategyFactory getInstantiationStrategyFactory() {
        return instantiationStrategyFactory;
    }

    public void setInstantiationStrategyFactory(RuleServiceInstantiationStrategyFactory instantiationStrategyFactory) {
        if (instantiationStrategyFactory == null) {
            throw new IllegalArgumentException("instantiationStrategyFactory arg can't be null");
        }
        this.instantiationStrategyFactory = instantiationStrategyFactory;
    }

    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    public void setExternalParameters(Map<String, Object> externalParameters) {
        this.externalParameters = externalParameters;
    }

    private Map<DeploymentDescription, RuleServiceDeploymentRelatedDependencyManager> dependencyManagerMap = new HashMap<DeploymentDescription, RuleServiceDeploymentRelatedDependencyManager>();

    public void clear(DeploymentDescription deploymentDescription) {
        dependencyManagerMap.remove(deploymentDescription);
        CompiledDependencyCache.getInstance().removeAll(deploymentDescription);
    }

    private RuleServiceDeploymentRelatedDependencyManager getRuleServiceDependencyManager(ServiceDescription serviceDescription) {
        if (dependencyManagerMap.containsKey(serviceDescription.getDeployment())) {
            return dependencyManagerMap.get(serviceDescription.getDeployment());
        } else {
            boolean isLazy = false;
            if (instantiationStrategyFactory instanceof RuleServiceInstantiationStrategyFactoryImpl) {
                isLazy = ((RuleServiceInstantiationStrategyFactoryImpl) instantiationStrategyFactory).isLazy();
            }
            RuleServiceDeploymentRelatedDependencyManager dependencyManager = new RuleServiceDeploymentRelatedDependencyManager(serviceDescription.getDeployment(),
                getRuleServiceLoader(),
                isLazy);
            dependencyManagerMap.put(serviceDescription.getDeployment(), dependencyManager);
            return dependencyManager;
        }
    }

    private IDependencyManager getDependencyManager(ServiceDescription serviceDescription) {
        CompositeRulesProjectDependencyManager multiModuleDependencyManager = new CompositeRulesProjectDependencyManager();
        // multimodule is only available for execution(execution mode == true)
        multiModuleDependencyManager.setExecutionMode(true);

        if (externalDependencyManager != null) {
            multiModuleDependencyManager.addDependencyManager(externalDependencyManager);
        }

        RuleServiceDeploymentRelatedDependencyManager ruleServiceDependencyManager = getRuleServiceDependencyManager(serviceDescription);
        multiModuleDependencyManager.addDependencyManager(ruleServiceDependencyManager);

        multiModuleDependencyManager.setExternalParameters(externalParameters);

        return multiModuleDependencyManager;
    }
}
