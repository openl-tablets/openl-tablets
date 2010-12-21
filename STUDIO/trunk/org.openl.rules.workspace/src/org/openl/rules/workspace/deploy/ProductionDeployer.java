package org.openl.rules.workspace.deploy;

import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;

import java.util.Collection;

/**
 * Interface for deployers to production repositories.
 */
public interface ProductionDeployer {
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
    DeployID deploy(ADeploymentProject deploymentProject, DeployID id, Collection<AProject> projects) throws DeploymentException;
}
