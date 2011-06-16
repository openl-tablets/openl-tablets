package org.openl.rules.ruleservice;

import java.util.List;

import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.loader.IRulesLoader;
import org.openl.rules.ruleservice.publish.IRulesPublisher;

public class RuleService {
    private IRulesLoader loader;
    private IRulesPublisher publisher;

    protected OpenLService createService(ServiceDescription serviceDescription) {
        List<Module> modules = loader.getModulesForService(serviceDescription);
        OpenLService newService = new OpenLService(serviceDescription.getName(), serviceDescription.getUrl(), modules,
                serviceDescription.getServiceClassName(), serviceDescription.isProvideRuntimeContext());
        return newService;
    }

    public OpenLService deploy(ServiceDescription serviceDescription) throws ServiceDeployException {
        return publisher.deploy(createService(serviceDescription));
    }

    public OpenLService redeploy(ServiceDescription serviceDescription) throws ServiceDeployException {
        OpenLService runningService = findServiceByName(serviceDescription.getName());
        return publisher.redeploy(runningService, createService(serviceDescription));
    }

    public OpenLService undeploy(String serviceName) throws ServiceDeployException {
        return publisher.undeploy(serviceName);
    }

    public List<OpenLService> getRunningServices() {
        return publisher.getRunningServices();
    }

    public OpenLService findServiceByName(String name) {
        return publisher.findServiceByName(name);
    }

    public IRulesLoader getLoader() {
        return loader;
    }

    public void setLoader(IRulesLoader loader) {
        this.loader = loader;
    }

    public IRulesPublisher getPublisher() {
        return publisher;
    }

    public void setPublisher(IRulesPublisher publisher) {
        this.publisher = publisher;
    }
}
