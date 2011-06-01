package org.openl.ruleservice.publish;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.ruleservice.OpenLService;
import org.openl.ruleservice.ServiceDeployException;

public class RulesPublisher implements IRulesPublisher {
    private static final Log LOG = LogFactory.getLog(RulesPublisher.class);

    private IRulesInstantiationFactory instantiationFactory;
    private IDeploymentAdmin deploymentAdmin;
    private IDependencyManager dependencyManager;

    public List<OpenLService> getRunningServices() {
        return deploymentAdmin.getRunningServices();
    }

    protected void initService(OpenLService service) throws Exception {
        RulesInstantiationStrategy instantiationStrategy = instantiationFactory.getStrategy(service.getModules(),
                dependencyManager);
        service.setInstantiationStrategy(instantiationStrategy);
        Object serviceBean = instantiationStrategy.instantiate(ReloadType.NO);
        service.setServiceBean(serviceBean);
        resolveInerface(service, instantiationStrategy);
    }

    private void resolveInerface(OpenLService service, RulesInstantiationStrategy instantiationStrategy)
            throws ClassNotFoundException {
        String serviceClassName = service.getServiceClassName();
        Class<?> serviceClass = null;
        if (serviceClassName != null) {
            ClassLoader serviceClassLoader = instantiationStrategy.getServiceClass().getClassLoader();
            try {
                serviceClass = serviceClassLoader.loadClass(serviceClassName);
            } catch (ClassNotFoundException e) {
                LOG.warn(String.format("Failed to load service class with name \"%s\"", serviceClassName));
                serviceClass = null;
            }
        }
        if (serviceClass == null) {
            serviceClass = instantiationStrategy.getServiceClass();
        }
        service.setServiceClass(serviceClass);
    }

    public OpenLService deploy(OpenLService service) throws ServiceDeployException {
        try {
            initService(service);
        } catch (Exception e) {
            throw new ServiceDeployException(String.format("Failed to initialiaze service \"%s\"", service.getName()),
                    e);
        }
        return deploymentAdmin.deploy(service);
    }

    public OpenLService redeploy(OpenLService runningService, OpenLService newService) throws ServiceDeployException {
        // TODO smart redeploy without full recompiling
        undeploy(runningService.getName());
        return deploy(newService);
    }

    public OpenLService undeploy(String serviceName) throws ServiceDeployException {
        return deploymentAdmin.undeploy(serviceName);
    }

    public IRulesInstantiationFactory getInstantiationFactory() {
        return instantiationFactory;
    }

    public void setInstantiationFactory(IRulesInstantiationFactory instantiationFactory) {
        this.instantiationFactory = instantiationFactory;
    }

    public IDeploymentAdmin getDeploymentAdmin() {
        return deploymentAdmin;
    }

    public void setDeploymentAdmin(IDeploymentAdmin deploymentAdmin) {
        this.deploymentAdmin = deploymentAdmin;
    }

    public IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public void setDependencyManager(IDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
    }
}
