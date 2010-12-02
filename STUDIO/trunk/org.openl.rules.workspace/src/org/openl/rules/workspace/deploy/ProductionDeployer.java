package org.openl.rules.workspace.deploy;

import org.openl.rules.project.abstraction.AProject;

import java.util.Collection;

/**
 * Interface for deployers to production repositories.
 */
public interface ProductionDeployer {
    /**
     * Deploys a collection of <code>Project</code>s to the production
     * repository. Generates unique ID for the deployment.
     *
     * @param projects projects to deploy
     * @return generated id for this deployment
     * @throws DeploymentException if any deployment error occures
     */
    DeployID deploy(Collection<AProject> projects) throws DeploymentException;

    /**
     * Deploys a collection of <code>Project</code>s to the production
     * repository with given ID. A concrete deployer may choose what to do if
     * there is a deployment with given <i>id</i> already exists: overwrite it,
     * throw an exception, etc.
     *
     * @param projects projects to deploy
     * @return <code>id</code> parameter
     * @throws DeploymentException if any deployment error occures
     */
    DeployID deploy(DeployID id, Collection<AProject> projects) throws DeploymentException;
}
