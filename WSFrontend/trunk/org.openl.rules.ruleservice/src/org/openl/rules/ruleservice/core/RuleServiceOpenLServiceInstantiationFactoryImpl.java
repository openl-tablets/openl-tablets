package org.openl.rules.ruleservice.core;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesServiceEnhancer;
import org.openl.rules.project.instantiation.variation.VariationsEnhancer;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.interceptors.ServiceInvocationAdvice;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
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

    private IDependencyManager dependencyManager;
    
    private Map<String, Object> externalParameters;

    private void initService(OpenLService service) throws RulesInstantiationException, ClassNotFoundException {
        if (service == null) {
            throw new IllegalArgumentException("service argument can't be null");
        }

        RulesInstantiationStrategy instantiationStrategy = instantiationStrategyFactory.getStrategy(service.getModules(),
                dependencyManager);
        instantiationStrategy.setExternalParameters(externalParameters);
        
        if (service.isProvideVariations()) {
            instantiationStrategy = new VariationsEnhancer(instantiationStrategy);
        }
        if (service.isProvideRuntimeContext()) {
            instantiationStrategy = new RulesServiceEnhancer(instantiationStrategy);
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
        factory.addAdvice(new ServiceInvocationAdvice(serviceBean, serviceClass));
        if (serviceClass.isInterface()) {
            factory.addInterface(serviceClass);
            if (!service.isProvideRuntimeContext()) {
                factory.addInterface(IEngineWrapper.class);
            }
        } else {
            // deprecated approach with wrapper: service class is not interface
            factory.setTarget(serviceBean);
            if (!Proxy.isProxyClass(serviceBean.getClass())) {
                factory.setProxyTargetClass(true);
            } else {
                factory.setProxyTargetClass(false);
            }
        }
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(instantiationStrategy.getClassLoader());
        Object proxyServiceBean = null;
        try {
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
        if (serviceClassName != null) {
            ClassLoader serviceClassLoader = instantiationStrategy.getClassLoader();
            try {
                serviceClass = serviceClassLoader.loadClass(serviceClassName);
                instantiationStrategy.setServiceClass(RuleServiceInstantiationFactoryHelper.getInterfaceForInstantiationStrategy(instantiationStrategy,
                    serviceClass));
            } catch (Exception e) {
                e.printStackTrace();
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Failed to load service class with name \"%s\"", serviceClassName));
                }
                serviceClass = instantiationStrategy.getInstanceClass();
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info(String.format("Service class is undefined of service '%s'. Generated interface will be used.",
                        service.getName()));
            }
            serviceClass = instantiationStrategy.getInstanceClass();
        }
        service.setServiceClass(serviceClass);
    }

    /** {@inheritDoc} */
    public OpenLService createService(ServiceDescription serviceDescription)
            throws RuleServiceInstantiationException {
        Collection<Module> modules = ruleServiceLoader.getModulesByServiceDescription(serviceDescription);
        OpenLService openLService = new OpenLService(serviceDescription.getName(),
            serviceDescription.getUrl(),
            serviceDescription.getServiceClassName(),
            serviceDescription.isProvideRuntimeContext(),
            serviceDescription.isProvideVariations(),
            modules);
        try {
            initService(openLService);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to initialiaze service " + openLService.getName(), e);
            }
            throw new RuleServiceInstantiationException(String.format(
                    "Failed to initialiaze OpenL service \"%s\"", openLService.getName()), e);
        }

        if (log.isInfoEnabled()) {
            String.format("Deploying service with name=\"%s\"...", openLService.getName());
        }
        return openLService;
    }

    /* for internal tests */public OpenLService createOpenLService(String serviceName, String url,
            String serviceClassName, boolean isProvideRuntimeContext, Collection<Module> modules)
            throws RuleServiceInstantiationException {
        return createOpenLService(serviceName, url, serviceClassName, isProvideRuntimeContext, false, modules);
    }

    /* for internal tests */public OpenLService createOpenLService(String serviceName, String url,
            String serviceClassName, boolean isProvideRuntimeContext, boolean isProvideVariations, Collection<Module> modules)
            throws RuleServiceInstantiationException {
        OpenLService openLService = new OpenLService(serviceName, url, serviceClassName, isProvideRuntimeContext,
                isProvideVariations, modules);
        try {
            initService(openLService);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to initialiaze service " + openLService.getName(), e);
            }
            throw new RuleServiceInstantiationException(String.format(
                    "Failed to initialiaze OpenL service \"%s\"", openLService.getName()), e);
        }

        if (log.isInfoEnabled()) {
            String.format("Deploying service with name=\"%s\"...", openLService.getName());
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

    public IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public void setDependencyManager(IDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
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

    
}
