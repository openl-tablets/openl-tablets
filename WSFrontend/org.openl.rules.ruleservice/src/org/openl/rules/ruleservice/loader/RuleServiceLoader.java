package org.openl.rules.ruleservice.loader;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.ModuleDescription;

import java.util.Collection;

/**
 * Wrapper on data source that gives access to data source and resolves the
 * OpenL projects/modules inside the projects. Contains own storage for all
 * projects that is used in services.
 *
 * @author Marat Kamalov
 */
public interface RuleServiceLoader {

    /**
     * Adds a lister to data source.
     *
     * @param dataSourceListener
     */
    void addListener(DataSourceListener dataSourceListener);

    /**
     * Removes a listener from data source.
     *
     * @param dataSourceListener
     */
    void removeListener(DataSourceListener dataSourceListener);

    /**
     * Returns deployments
     *
     * @return list of deployments.
     */
    Collection<Deployment> getDeployments();

    /**
     * @param deploymentName
     * @param deploymentVersion
     * @param projectName
     * @return
     */
    Collection<Module> resolveModulesForProject(String deploymentName,
            CommonVersion deploymentVersion,
            String projectName);

    /**
     * @param deploymentName
     * @param deploymentVersion
     * @param modulesToLoad
     * @return
     */
    Collection<Module> getModulesByServiceDescription(String deploymentName,
            CommonVersion deploymentVersion,
            Collection<ModuleDescription> modulesToLoad);
}
