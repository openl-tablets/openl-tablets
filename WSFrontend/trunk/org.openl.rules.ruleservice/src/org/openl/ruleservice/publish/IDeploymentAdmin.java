package org.openl.ruleservice.publish;

import java.util.List;

import org.openl.ruleservice.OpenLService;
import org.openl.ruleservice.ServiceDeployException;

/**
 * Deployment admin controls the way how the services will be exposed.
 * 
 * @author PUdalau
 */
public interface IDeploymentAdmin {

    /**
     * Deploys the specified service.
     * 
     * @param service Service to deploy.
     * @return
     * @throws ServiceDeployException
     */
    OpenLService deploy(OpenLService service) throws ServiceDeployException;

    /**
     * Undeploys currently running service.
     * 
     * @param serviceName Name of the service to undeploy.
     * @return Undeployed service.
     * @throws ServiceDeployException
     */
    OpenLService undeploy(String serviceName) throws ServiceDeployException;

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
