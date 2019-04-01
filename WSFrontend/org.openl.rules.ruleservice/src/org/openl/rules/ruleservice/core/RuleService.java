package org.openl.rules.ruleservice.core;

import java.util.Collection;

/**
 * Top level service for management OpenL services. It is used for deploy/undeploy/redeploy OpenL services. Builds
 * OpenLServices from ServiceDescriptors and uses a publisher for exposing services.
 *
 * @author Marat Kamalov
 *
 */
public interface RuleService {
    /**
     * Deploys a service.
     *
     * @param serviceDescription service description
     * @throws RuleServiceDeployException occurs if deploy process fails
     */
    void deploy(ServiceDescription serviceDescription) throws RuleServiceDeployException;

    /**
     * Redeploys a service.
     *
     * @param serviceDescription service description
     * @throws RuleServiceUndeployException
     * @throws RuleServiceDeployException
     */
    void redeploy(ServiceDescription serviceDescription) throws RuleServiceDeployException,
                                                         RuleServiceUndeployException;

    /**
     * Undeploys a service by name.
     *
     * @param serviceName service name
     * @throws RuleServiceDeployException throws exceptions if the service with specified name is't deployed or undeploy
     *             process fails.
     */
    void undeploy(String serviceName) throws RuleServiceUndeployException;

    /**
     * Returns a collection of deployed OpenL services.
     *
     * @return a collection of OpenL services
     */
    Collection<? extends OpenLService> getServices();

    /**
     * Finds and returns deployed OpenL service by name. Returns null if service with specified name isn't deployed.
     *
     * @param serviceName service name
     * @return founded OpenL service
     */
    OpenLService getServiceByName(String serviceName);

}