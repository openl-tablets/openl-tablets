package org.openl.rules.ruleservice.loader;

import java.util.Collection;

public interface DeploymentsToLoadManager {

    Collection<DeploymentInfo> getDeploymentsToLoad(Collection<DeploymentInfo> deployments);

    void resetLoadedDeploymentsCache();

}
