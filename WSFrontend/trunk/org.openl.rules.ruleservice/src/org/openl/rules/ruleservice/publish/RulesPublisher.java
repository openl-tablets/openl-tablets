package org.openl.rules.ruleservice.publish;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesServiceEnhancer;
import org.openl.rules.ruleservice.OpenLService;
import org.openl.rules.ruleservice.ServiceDeployException;

/**
 * Publisher
 * 
 * @author MKamalov
 * 
 */
public class RulesPublisher implements IRulesPublisher {
    private static final Log LOG = LogFactory.getLog(RulesPublisher.class);

    private IRulesInstantiationFactory instantiationFactory;
    private IDeploymentAdmin deploymentAdmin;
    private IDependencyManager dependencyManager;

    public List<OpenLService> getRunningServices() {
        return deploymentAdmin.getRunningServices();
    }

    public OpenLService findServiceByName(String serviceName) {
        if (serviceName == null)
            throw new IllegalArgumentException("serviceName argument can't be null");

        return deploymentAdmin.findServiceByName(serviceName);
    }

    protected void initService(OpenLService service) throws InstantiationException, ClassNotFoundException, IllegalAccessException {
        if (service == null)
            throw new IllegalArgumentException("service argument can't be null");

        RulesInstantiationStrategy instantiationStrategy = instantiationFactory.getStrategy(service.getModules(),
                dependencyManager);
        service.setInstantiationStrategy(instantiationStrategy);
        if (service.isProvideRuntimeContext()) {
            RulesServiceEnhancer enhancer = new RulesServiceEnhancer(instantiationStrategy);
            service.setEnhancer(enhancer);
        }
        instantiateServiceBean(service);
        resolveInterface(service);
    }

    @SuppressWarnings("deprecation")
    private void instantiateServiceBean(OpenLService service) throws InstantiationException, ClassNotFoundException, IllegalAccessException {
        Object serviceBean = null;
        if (service.isProvideRuntimeContext()) {
            serviceBean = service.getEnhancer().instantiate(ReloadType.NO);
        } else {
            serviceBean = service.getInstantiationStrategy().instantiate(ReloadType.NO);
        }
        service.setServiceBean(serviceBean);

    }

    private void resolveInterface(OpenLService service) throws InstantiationException, ClassNotFoundException{
        String serviceClassName = service.getServiceClassName();
        Class<?> generatedServiceClass = null;// created by engine factory
        if (service.isProvideRuntimeContext()) {
            generatedServiceClass = service.getEnhancer().getServiceClass();
        } else {
            generatedServiceClass = service.getInstantiationStrategy().getServiceClass();
        }
        Class<?> serviceClass = null;
        if (serviceClassName != null) {
            ClassLoader serviceClassLoader = generatedServiceClass.getClassLoader();
            try {
                serviceClass = serviceClassLoader.loadClass(serviceClassName);
            } catch (ClassNotFoundException e) {
                LOG.warn(String.format("Failed to load service class with name \"%s\"", serviceClassName));
                serviceClass = null;
            }
        }
        if (serviceClass == null) {
            serviceClass = generatedServiceClass;
        }
        service.setServiceClass(serviceClass);
    }

    public OpenLService deploy(OpenLService service) throws ServiceDeployException {
        if (service == null)
            throw new IllegalArgumentException("service argument can't be null");

        try {
            initService(service);
        } catch (Exception e) {
            throw new ServiceDeployException(String.format("Failed to initialiaze service \"%s\"", service.getName()),
                    e);
        }
        return deploymentAdmin.deploy(service);
    }

    public OpenLService redeploy(OpenLService runningService, OpenLService newService) throws ServiceDeployException {
        if (runningService == null)
            throw new IllegalArgumentException("runningService argument can't be null");
        if (newService == null)
            throw new IllegalArgumentException("newService argument can't be null");
        
        // TODO smart redeploy without full recompiling
        undeploy(runningService.getName());
        return deploy(newService);
    }

    public OpenLService undeploy(String serviceName) throws ServiceDeployException {
        if (serviceName == null)
            throw new IllegalArgumentException("serviceName argument can't be null");

        return deploymentAdmin.undeploy(serviceName);
    }

    public IRulesInstantiationFactory getInstantiationFactory() {
        return instantiationFactory;
    }

    public void setInstantiationFactory(IRulesInstantiationFactory instantiationFactory) {
        if (instantiationFactory == null)
            throw new IllegalArgumentException("instantiationFactory argument can't be null");

        this.instantiationFactory = instantiationFactory;
    }

    public IDeploymentAdmin getDeploymentAdmin() {
        return deploymentAdmin;
    }

    public void setDeploymentAdmin(IDeploymentAdmin deploymentAdmin) {
        if (deploymentAdmin == null)
            throw new IllegalArgumentException("deploymentAdmin argument can't be null");
        
        this.deploymentAdmin = deploymentAdmin;
    }

    public IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public void setDependencyManager(IDependencyManager dependencyManager) {
        if (dependencyManager == null)
            throw new IllegalArgumentException("dependencyManager argument can't be null");

        this.dependencyManager = dependencyManager;
    }
}
