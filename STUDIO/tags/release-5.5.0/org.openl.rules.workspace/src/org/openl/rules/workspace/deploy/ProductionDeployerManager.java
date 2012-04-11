package org.openl.rules.workspace.deploy;

import org.openl.rules.workspace.WorkspaceUser;

/**
 * Creates <code>ProductionDeployer</code> instances for given user.
 * Implementations will possibly keep a cache of the instances per user.
 */
public interface ProductionDeployerManager {
    ProductionDeployer getDeployer(WorkspaceUser user) throws DeploymentException;
}
