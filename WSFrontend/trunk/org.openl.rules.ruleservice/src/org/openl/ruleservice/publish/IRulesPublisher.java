package org.openl.ruleservice.publish;

import java.util.List;

import org.openl.ruleservice.OpenLService;

public interface IRulesPublisher {
    IDeploymentAdmin getDeploymentAdmin();

    List<OpenLService> getRunningServices();
    OpenLService deploy(OpenLService service);
    OpenLService redeploy(OpenLService runningService, OpenLService newService);
    OpenLService undeploy(String serviceName);
}
