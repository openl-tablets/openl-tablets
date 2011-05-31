package org.openl.ruleservice.publish;

import java.util.List;

import org.openl.ruleservice.OpenLService;

public interface IDeploymentAdmin {
    OpenLService deploy(OpenLService service);
    
    List<OpenLService> getRunningServices();

    OpenLService undeploy(String serviceName);
}
