package org.openl.rules.ruleservice.loader;

import java.util.Collection;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.IDeployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolvingException;

/**
 * Wrapper on data source that gives access to data source and resolves the OpenL projects/modules inside the projects.
 * Contains own storage for all projects that is used in services.
 *
 * @author Marat Kamalov
 */
public interface RuleServiceLoader {

    /**
     * Adds a lister to data source.
     *
     * @param dataSourceListener
     */
    void setListener(DataSourceListener dataSourceListener);

    /**
     * Returns deployments
     *
     * @return list of deployments.
     */
    Collection<IDeployment> getDeployments();

    /**
     * @param deploymentName
     * @param deploymentVersion
     * @param projectName
     * @return
     */
    ProjectDescriptor resolveProject(String deploymentName, CommonVersion deploymentVersion, String projectName) throws ProjectResolvingException;

    /**
     * Gets a deployment from data source.
     *
     * @param deploymentName target deployment name
     * @param deploymentVersion target version
     * @return deployment
     */
    IDeployment getDeployment(String deploymentName, CommonVersion deploymentVersion);

    /**
     * Removes deploy path from folder path
     *
     * @param realFolderPath real path to folder
     * @return cleared folder path
     */
    String getLogicalProjectFolder(String realFolderPath);

    /**
     * Checks if rules service loader is ready (there are no connection issues etc.).
     */
    boolean isReady();
}
