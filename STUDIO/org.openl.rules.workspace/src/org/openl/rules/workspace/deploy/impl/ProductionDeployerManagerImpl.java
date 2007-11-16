package org.openl.rules.workspace.deploy.impl;

import org.openl.SmartProps;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.deploy.ProductionDeployerManager;
import org.openl.rules.workspace.deploy.impl.jcr.JcrProductionDeployer;

import java.util.HashMap;

public class ProductionDeployerManagerImpl implements ProductionDeployerManager {
    public static final String PROPERTY_FILE = "deploy.properties";

    public final SmartProps properties;

    /**
     * User name -> Production deployer.
     */
    private final HashMap<String, ProductionDeployer> deployers = new HashMap<String, ProductionDeployer>();

    public ProductionDeployerManagerImpl() {
        this(new SmartProps(PROPERTY_FILE));
    }

    public ProductionDeployerManagerImpl(SmartProps props) {
        properties = props;
    }

    public synchronized ProductionDeployer getDeployer(WorkspaceUser user) throws DeploymentException {
        ProductionDeployer productionDeployer = deployers.get(user.getUserId());
        if (productionDeployer == null) {
            deployers.put(user.getUserId(), productionDeployer = new JcrProductionDeployer(user, properties));
        }

        return productionDeployer;
    }
}
