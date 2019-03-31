package org.openl.rules.ruleservice.conf;

import java.util.Collection;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;

/**
 * Serves to determine all services to be deployed according to the data source current state.
 * 
 * @author PUdalau
 */
public interface ServiceConfigurer {
    /**
     * Compute all service to be deployed according to the projects in the data source state(accessed through the
     * loader).
     * 
     * @param loader Loader to access projects.
     * @return List of {@link ServiceDescription} to deployed.
     */
    Collection<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader ruleServiceLoader);
}
