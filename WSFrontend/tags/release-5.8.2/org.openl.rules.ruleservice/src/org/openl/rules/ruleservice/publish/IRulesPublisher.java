package org.openl.rules.ruleservice.publish;

import java.util.List;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.ServiceDeployException;

public interface IRulesPublisher {
    
    IDeploymentAdmin getDeploymentAdmin();

    List<OpenLService> getRunningServices();

    OpenLService findServiceByName(String serviceName);

    OpenLService deploy(OpenLService service) throws ServiceDeployException;

    OpenLService redeploy(OpenLService runningService, OpenLService newService) throws ServiceDeployException;

    OpenLService undeploy(String serviceName) throws ServiceDeployException;
}
