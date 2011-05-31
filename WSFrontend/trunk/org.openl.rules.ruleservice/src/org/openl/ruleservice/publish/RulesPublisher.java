package org.openl.ruleservice.publish;

import java.util.List;

import org.openl.ruleservice.OpenLService;

public class RulesPublisher implements IRulesPublisher {
    private IRulesInstantiationFactory instantiationFactory;
    private IDeploymentAdmin deploymentAdmin;

    public IDeploymentAdmin getDeploymentAdmin() {
        return deploymentAdmin;
    }

    public List<OpenLService> getRunningServices() {
        // TODO Auto-generated method stub
        return null;
    }

    public OpenLService deploy(OpenLService service) {
        // TODO Auto-generated method stub
        return null;
    }

    public OpenLService redeploy(OpenLService runningService, OpenLService newService) {
        // TODO Auto-generated method stub
        return null;
    }

    public OpenLService undeploy(String serviceName) {
        // TODO Auto-generated method stub
        return null;
    }

}
