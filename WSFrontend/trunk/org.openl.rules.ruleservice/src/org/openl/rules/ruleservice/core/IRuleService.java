package org.openl.rules.ruleservice.core;

import java.util.List;

public interface IRuleService {

    OpenLService deploy(ServiceDescription serviceDescription) throws ServiceDeployException;

    OpenLService redeploy(ServiceDescription serviceDescription) throws ServiceDeployException;

    OpenLService undeploy(String serviceName) throws ServiceDeployException;

    List<OpenLService> getRunningServices();

    OpenLService findServiceByName(String name);
}