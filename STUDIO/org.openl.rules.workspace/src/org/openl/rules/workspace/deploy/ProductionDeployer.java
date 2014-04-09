package org.openl.rules.workspace.deploy;

import java.util.Collection;

import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.WorkspaceUser;

/**
 * Interface for deployers to production repositories.
 * Each deployer works with a specified repository
 */
public interface ProductionDeployer {
    /**
     * Deploys a collection of <code>Project</code>s to the production
     * repository with given ID. A concrete deployer may choose what to do if
     * there is a deployment with given <i>id</i> already exists: overwrite it,
     * throw an exception, etc.
     *
     * @param deploymentProject deploy configuration
     * @param projects projects to deploy
     * @param user deploying user
     * @return <code>id</code> parameter
     * @throws DeploymentException if any deployment error occures
     */
    DeployID deploy(ADeploymentProject deploymentProject, Collection<AProject> projects, WorkspaceUser user) throws DeploymentException;

    /**
     * Checks if deploymentConfiguration is already deployed to this production
     * repository.
     * 
     * @param deployConfiguration deploy configuration for project
     *            trying to deploy
     * @return true if deploymentConfiguration with its id already exists in
     *         production repository
     * @throws RRepositoryException if cannot get info from repository for some
     *             reason
     */
    public boolean hasDeploymentProject(ADeploymentProject deployConfiguration) throws RRepositoryException;
    
    /**
     * Destroy deployer and release associated resources
     * @throws RRepositoryException if exception during deploying is occured
     */
    public void destroy() throws RRepositoryException;
}
