package org.openl.rules.ruleservice.publish;

import java.util.Collection;
import java.util.List;
import java.util.jar.Manifest;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.servlet.MethodDescriptor;
import org.openl.rules.ruleservice.servlet.ServiceInfo;

public interface RuleServiceManager {

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
     * @param serviceName Name of the service to undeploy.
     * @throws RuleServiceDeployException
     */
    void undeploy(String serviceName) throws RuleServiceUndeployException;

    /**
     * Provides info about all currently running services.
     *
     * @return List of running services.
     */
    Collection<OpenLService> getServices();

    /**
     * Searches for the service from currently running with the specified name or null if service with specified name
     * wasn't deployed.
     *
     * @param name Name of the service to find.
     * @return Service with the specified name or null if service with specified name wasn't deployed.
     */
    OpenLService getServiceByName(String name);

    Collection<ServiceInfo> getServicesInfo();

    Collection<MethodDescriptor> getServiceMethods(String serviceName);

    List<String> getServiceErrors(String serviceName);

    Manifest getManifest(String serviceName);
}
