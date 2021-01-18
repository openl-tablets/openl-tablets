package org.openl.rules.ruleservice.publish;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
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
     * @throws RuleServiceDeployException
     */
    void deploy(OpenLService service) throws RuleServiceDeployException;

    /**
     * Undeploys currently running service.
     *
     * @param service Service to undeploy.
     * @throws RuleServiceDeployException
     */
    void undeploy(OpenLService service) throws RuleServiceUndeployException;

    /**
     * Searches for the service from currently running with the specified name or null if service with specified name
     * wasn't deployed.
     *
     * @param deployPath deploy path of the service to find.
     * @return Service with the specified name or null if service with specified name wasn't deployed.
     */
    OpenLService getServiceByDeploy(String deployPath);

    /**
     * Returns a url for the given service, if it is published, otherwise returns null.
     */
    String getUrl(OpenLService service);
}
