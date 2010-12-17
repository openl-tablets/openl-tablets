package org.openl.rules.ruleservice.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.common.CommonVersion;

public class LatestDeploymentsToLoadManager implements DeploymentsToLoadManager {
    private Map<String, String> deployment2Version = new HashMap<String, String>();
    
    public Collection<DeploymentInfo> getDeploymentsToLoad(Collection<DeploymentInfo> deployments) {
        Map<String, CommonVersion> latestVersionMap = computeLatestVersions(deployments);

        Collection<DeploymentInfo> deploymentsToLoad =  computeDeploymentsToLoad(deployments, latestVersionMap);
        
        for(DeploymentInfo deployment : deploymentsToLoad) {
            deployment2Version.put(deployment.getName(), deployment.getVersion().getVersionName());
        }
        
        return deploymentsToLoad;
    }
    
    public void resetLoadedDeploymentsCache() {
        deployment2Version = new HashMap<String, String>();
    }
    
    public static Map<String, CommonVersion> computeLatestVersions(Collection<DeploymentInfo> deployments) {
        Map<String, CommonVersion> versionMap = new HashMap<String, CommonVersion>();

        for (DeploymentInfo deployment : deployments) {
            CommonVersion version = versionMap.get(deployment.getName());

            if (version == null || deployment.getVersion().compareTo(version) > 0) {
                version = deployment.getVersion();
            }

            versionMap.put(deployment.getName(), version);
        }

        return versionMap;
    }

    private Collection<DeploymentInfo> computeDeploymentsToLoad(Collection<DeploymentInfo> deployments,
            Map<String, CommonVersion> latestVersionMap) {
        Collection<DeploymentInfo> deploymentsToLoad = new ArrayList<DeploymentInfo>();

        for (DeploymentInfo deployment : deployments) {
            // check whether we load only deployment with latest version
            if (latestVersionMap.get(deployment.getName()).equals(deployment.getVersion())) {
                final String version = deployment.getVersion().getVersionName();
                // check whether deployment with latest version has not been
                // loaded yet
                if (!version.equals(deployment2Version.get(deployment.getName()))) {
                    deploymentsToLoad.add(deployment);
                }
            }
        }

        return deploymentsToLoad;
    }

}
