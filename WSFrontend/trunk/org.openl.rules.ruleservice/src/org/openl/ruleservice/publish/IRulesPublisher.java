package org.openl.ruleservice.publish;

import java.util.List;

import org.openl.ruleservice.OpenLService;
import org.openl.ruleservice.ServiceDeployException;

public interface IRulesPublisher {
    
    IDeploymentAdmin getDeploymentAdmin();

    List<OpenLService> getRunningServices();

    OpenLService findServiceByName(String name);

    OpenLService deploy(OpenLService service) throws ServiceDeployException;

    OpenLService redeploy(OpenLService runningService, OpenLService newService) throws ServiceDeployException;

    OpenLService undeploy(String serviceName) throws ServiceDeployException;
}
