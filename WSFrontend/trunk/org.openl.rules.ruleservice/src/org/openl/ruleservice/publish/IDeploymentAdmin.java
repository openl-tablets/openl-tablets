package org.openl.ruleservice.publish;

import java.util.List;

import org.openl.ruleservice.OpenLService;
import org.openl.ruleservice.ServiceDeployException;

public interface IDeploymentAdmin {
    OpenLService deploy(OpenLService service) throws ServiceDeployException;
    
    List<OpenLService> getRunningServices();

    OpenLService undeploy(String serviceName) throws ServiceDeployException;
}
