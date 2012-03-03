package org.openl.rules.ruleservice.publish;

import java.util.List;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceRedeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;

/**
 * Deployment admin controls the way how the services will be exposed.
 * 
 * @author PUdalau
 */
public interface RuleServicePublisher {

    /**
     * Deploys the specified service.
     * 
     * @param service Service to deploy.
     * @return
     * @throws RuleServiceDeployException
     */
    OpenLService deploy(OpenLService service) throws RuleServiceDeployException;

    /**
     * Undeploys currently running service.
     * 
     * @param serviceName Name of the service to undeploy.
     * @return Undeployed service.
     * @throws RuleServiceDeployException
     */
    OpenLService undeploy(String serviceName) throws RuleServiceUndeployException;
    
    /**
     * Undeploys currently running service.
     * 
     * @param serviceName Name of the service to undeploy.
     * @return Undeployed service.
     * @throws RuleServiceDeployException
     */
    OpenLService redeploy(OpenLService service) throws RuleServiceRedeployException;

    /**
     * Provides info about all currently running services.
     * 
     * @return List of running services.
     */
    List<OpenLService> getRunningServices();

    /**
     * Searches for the service from currently running with the specified name.
     * 
     * @param name Name of the service to find.
     * @return Service with the specified name.
     */
    OpenLService findServiceByName(String name);
}
