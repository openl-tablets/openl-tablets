package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;

public class LatestDeploymentsToLoadManagerTest {
    @Test
    public void testComputeLatestVersions() {
        Collection<DeploymentInfo> deployments = new ArrayList<DeploymentInfo>();
        deployments.add(DeploymentInfo.valueOf("deployment1#1.1"));
        deployments.add(DeploymentInfo.valueOf("deployment2#2.2"));
        deployments.add(DeploymentInfo.valueOf("deployment3#3.3"));
        deployments.add(DeploymentInfo.valueOf("deployment3#3.2"));
        Map<String, CommonVersion> versionMap = new HashMap<String, CommonVersion>();
        versionMap.put("deployment1", new CommonVersionImpl("1.1"));
        versionMap.put("deployment2", new CommonVersionImpl("2.2"));
        versionMap.put("deployment3", new CommonVersionImpl("3.3"));
        assertEquals(versionMap, LatestDeploymentsToLoadManager.computeLatestVersions(deployments));
        versionMap.put("deployment3", new CommonVersionImpl("3.4"));
        versionMap.put("deployment1", new CommonVersionImpl("1.8"));
        deployments.add(DeploymentInfo.valueOf("deployment3#3.4"));
        deployments.add(DeploymentInfo.valueOf("deployment1#1.8"));
        assertEquals(versionMap, LatestDeploymentsToLoadManager.computeLatestVersions(deployments));
        deployments.add(DeploymentInfo.valueOf("deployment1#1.999"));
        assertFalse(versionMap.equals(LatestDeploymentsToLoadManager.computeLatestVersions(deployments)));
    }
}
