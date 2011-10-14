package org.openl.rules.ruleservice.management;

import java.util.List;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.IRulesLoader;

/**
 * Serves to determine all services to be deployed according to the data source
 * current state.
 * 
 * @author PUdalau
 */
public interface IServiceConfigurer {
    /**
     * Compute all service to be deployed according to the projects in the data
     * source state(accessed through the loader)
     * 
     * @param loader Loader to access projects.
     * @return List of {@link ServiceDescription} to deployed.
     */
    List<ServiceDescription> getServicesToBeDeployed(IRulesLoader loader);
}
