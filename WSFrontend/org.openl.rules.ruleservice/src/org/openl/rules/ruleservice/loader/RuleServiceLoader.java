package org.openl.rules.ruleservice.loader;

import java.util.Collection;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.ServiceDescription;

/**
 * Wrapper on data source that gives access to data source and resolves the
 * OpenL projects/modules inside the projects. Contains own storage for all
 * projects that is used in services.
 * 
 * @author Marat Kamalov
 * 
 */
public interface RuleServiceLoader {
    /**
     * Returns data source.
     * 
     * @return data source
     */
    DataSource getDataSource();

    /**
     * Returns deployments
     * 
     * @return list of deployments.
     */
    Collection<Deployment> getDeployments();

    /**
     * 
     * @param deploymentName
     * @param deploymentVersion
     * @param projectName
     * @return
     */
    Collection<Module> resolveModulesForProject(String deploymentName, CommonVersion deploymentVersion, String projectName);

    /**
     * 
     * @param serviceDescription
     * @return
     */
    Collection<Module> getModulesByServiceDescription(ServiceDescription serviceDescription);
}
