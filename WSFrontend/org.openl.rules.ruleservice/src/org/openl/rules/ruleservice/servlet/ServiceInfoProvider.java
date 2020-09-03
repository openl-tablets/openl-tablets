package org.openl.rules.ruleservice.servlet;

import java.util.Collection;
import java.util.jar.Manifest;

public interface ServiceInfoProvider {

    /**
     * Returns a list of errors for the unsuccessfully deployed service.
     */
    Collection<String> getServiceErrors(String serviceName);

    /**
     * Returns MANIFEST.MF description for the given service.
     */
    Manifest getManifest(String serviceName);

    /**
     * Returns a list of available methods for the successfully deployed service.
     */
    Collection<MethodDescriptor> getServiceMethods(String serviceName);

    /**
     * Returns a list of deployed services.
     */
    Collection<ServiceInfo> getServicesInfo();

}
