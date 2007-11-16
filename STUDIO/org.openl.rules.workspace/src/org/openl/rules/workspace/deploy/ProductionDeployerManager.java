package org.openl.rules.workspace.deploy;

import org.openl.rules.workspace.WorkspaceUser;

public interface ProductionDeployerManager {
    ProductionDeployer getDeployer(WorkspaceUser user) throws DeploymentException;
}
