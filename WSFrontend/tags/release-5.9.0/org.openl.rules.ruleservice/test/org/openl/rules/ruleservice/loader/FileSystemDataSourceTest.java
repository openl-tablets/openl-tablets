package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.project.abstraction.Deployment;

public class FileSystemDataSourceTest {
    private static IDataSource dataSource;

    private static String FILE_SYSTEM_DATA_SOURCE_DIRECTORY = "test-resources/filesystemdatasource";

    @BeforeClass
    public static void setDataSource() {
        dataSource = new FileSystemDataSource(FILE_SYSTEM_DATA_SOURCE_DIRECTORY);
    }

    @Before
    public void removeAllDataSourceListeners() {
        dataSource.removeAllListeners();
    }

    @Test
    public void testJcrDataSource() {
        assertNotNull(dataSource);
    }

    @Test
    public void testGetDeployments() {
        List<Deployment> deployments = dataSource.getDeployments();
        assertTrue(deployments.size() == 1);
    }

    @Test
    public void testGetDeployment() {
        List<Deployment> deployments = dataSource.getDeployments();
        assertTrue(deployments.size() == 1);
        Deployment tmp = deployments.get(0);
        Deployment deployment = dataSource.getDeployment(tmp.getDeploymentName(), tmp.getCommonVersion());
        assertNotNull(deployment);
        assertEquals(tmp.getName(), deployment.getDeploymentName());
        assertEquals(tmp.getCommonVersion(), deployment.getCommonVersion());
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
