package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.ruleservice.Constants.*;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.ruleservice.loader.IDataSource;
import org.openl.rules.ruleservice.loader.JcrDataSource;

public class JcrDataSourceTest {

    private static IDataSource dataSource;

    @BeforeClass
    public static void setDataSource() {
        dataSource = new JcrDataSource();
    }

    @Test
    public void testJcrDataSource() {
        assertNotNull(dataSource);
    }

    @Test
    public void testGetDeployments() {
        List<Deployment> deployments = dataSource.getDeployments();
        assertTrue(deployments.size() > 0);
    }

    @Test
    public void testGetDeployment() {
        CommonVersion commonVersion = new CommonVersionImpl(VERSION);
        Deployment deployment = dataSource.getDeployment(DEPLOYMENT_NAME, commonVersion);
        assertNotNull(deployment);
        assertEquals(DEPLOYMENT_NAME, deployment.getDeploymentName());
        assertEquals(VERSION, deployment.getCommonVersion().getVersionName());
    }
}
