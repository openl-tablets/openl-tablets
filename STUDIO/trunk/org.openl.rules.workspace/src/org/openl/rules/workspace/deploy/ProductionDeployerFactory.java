package org.openl.rules.workspace.deploy;

public interface ProductionDeployerFactory {
    ProductionDeployer getDeployerInstance(String repositoryConfigName);
}
