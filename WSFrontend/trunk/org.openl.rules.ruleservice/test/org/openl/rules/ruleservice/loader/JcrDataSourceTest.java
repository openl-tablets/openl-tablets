package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.ruleservice.Constants.*;

import java.util.List;

import org.junit.Before;
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

    @Before
    public void removeAllDataSourceListeners() {
        dataSource.removeAllListeners();
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

    @Test
    public void testAddListener() {
        assertTrue(dataSource.getListeners().size() == 0);
        IDataSourceListener dataSourceListener = new IDataSourceListener() {
            public void onDeploymentAdded() {
            }
        };
        dataSource.addListener(dataSourceListener);
        assertTrue(dataSource.getListeners().size() == 1);
    }

    @Test
    public void testRemoveListener() {
        assertTrue(dataSource.getListeners().size() == 0);
        IDataSourceListener dataSourceListener = new IDataSourceListener() {
            public void onDeploymentAdded() {
            }
        };
        dataSource.addListener(dataSourceListener);
        assertTrue(dataSource.getListeners().size() == 1);
        dataSource.removeListener(dataSourceListener);
        assertTrue(dataSource.getListeners().size() == 0);
    }

    @Test
    public void testRemoveAllListeners() {
        assertTrue(dataSource.getListeners().size() == 0);
        IDataSourceListener dataSourceListener = new IDataSourceListener() {
            public void onDeploymentAdded() {
            }
        };
        dataSource.addListener(dataSourceListener);
        assertTrue(dataSource.getListeners().size() == 1);
        dataSource.removeAllListeners();
        assertTrue(dataSource.getListeners().size() == 0);
    }
}
