package org.openl.rules.ruleservice.core;

import java.util.List;

/**
 * Top level service for management OpenL services. It is used for
 * deploy/undeploy/redeploy OpenL services. Builds OpenLServices from
 * ServiceDescriptors and uses a publisher for exposing services.
 * 
 * @author Marat Kamalov
 * 
 */
public interface RuleService {
    /**
     * Deploys a service
     * 
     * @param serviceDescription service description
     * @return deployed OpenL service
     * @throws RuleServiceDeployException occurs if deploy process fails
     */
    OpenLService deploy(ServiceDescription serviceDescription) throws RuleServiceDeployException;

    /**
     * Redeploys a service
     * 
     * @param serviceDescription service description
     * @return redeployed OpenL service
     * @throws RuleServiceRedeployException
     */
    OpenLService redeploy(ServiceDescription serviceDescription) throws RuleServiceRedeployException;

    /**
     * Undeploys a service by name
     * 
     * @param serviceName service name
     * @return undeployed OpenL service. Returns null if service with specified name isn't deployed
     * @throws RuleServiceDeployException throws exceptions if the service with
     *             specified name is't deployed or undeploy process fails.
     */
    OpenLService undeploy(String serviceName) throws RuleServiceUndeployException;

    /**
     * Returns a list of deployed OpenL services
     * 
     * @return a list of OpenL services
     */
    List<OpenLService> getRunningServices();

    /**
     * Finds and returns deployed OpenL service by name. Returns null if service with specified name isn't deployed.
     * 
     * @param serviceName service name
     * @return founded OpenL service
     */
    OpenLService findServiceByName(String serviceName);

}