package org.openl.rules.workspace.deploy.impl;

import java.util.HashMap;

import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.deploy.ProductionDeployerManager;
import org.openl.rules.workspace.deploy.impl.jcr.JcrProductionDeployer;

/**
 * This class is responsible for creating <code>ProductionDeployer</code>
 * instances for given users.
 */
public class ProductionDeployerManagerImpl implements ProductionDeployerManager {
    /**
     * User name -> Production deployer.
     */
    private final HashMap<String, ProductionDeployer> deployers = new HashMap<String, ProductionDeployer>();

    public ProductionDeployerManagerImpl() {

    }

    public synchronized ProductionDeployer getDeployer(WorkspaceUser user) throws DeploymentException {
        ProductionDeployer productionDeployer = deployers.get(user.getUserId());
        if (productionDeployer == null) {
            deployers.put(user.getUserId(), productionDeployer = new JcrProductionDeployer(user));
        }

        return productionDeployer;
    }
}
