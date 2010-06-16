package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;

public class RulesLoaderTest {
    

    @Test
    public void testParseDeloyments() {
        Collection<DeploymentInfo> deployments = new ArrayList<DeploymentInfo>();
        deployments.add(DeploymentInfo.valueOf("deployment1#1.1"));
        deployments.add(DeploymentInfo.valueOf("deployment2#2.2"));
        deployments.add(DeploymentInfo.valueOf("deployment3#3.3"));
        Collection<String> deploymentNames = new ArrayList<String>();
        deploymentNames.add("deployment1#1.1");
        deploymentNames.add("deployment2#2.2");
        deploymentNames.add("deployment3#3.3");
        assertEquals(deployments, JcrRulesLoader.parseDeloyments(deploymentNames));
        deploymentNames.add("deployment4#4.4");
        assertFalse(deployments.equals(JcrRulesLoader.parseDeloyments(deploymentNames)));
    }
}
