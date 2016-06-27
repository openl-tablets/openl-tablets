package org.openl.rules.ruleservice.loader;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.project.abstraction.Deployment;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FileSystemDataSourceTest {
    private FileSystemDataSource dataSource;

    private static String FILE_SYSTEM_DATA_SOURCE_DIRECTORY = "target/filesystemdatasource";

    @Before
    public void setDataSource() {
        dataSource = new FileSystemDataSource(FILE_SYSTEM_DATA_SOURCE_DIRECTORY);
    }

    @Test
    public void testJcrDataSource() {
        assertNotNull(dataSource);
    }

    @Test
    public void testGetDeployments() {
        Collection<Deployment> deployments = dataSource.getDeployments();
        assertTrue(deployments.size() == 1);
    }

    @Test
    public void testGetDeployment() {
        Collection<Deployment> deployments = dataSource.getDeployments();
        assertTrue(deployments.size() == 1);
        Deployment tmp = deployments.iterator().next();
        Deployment deployment = dataSource.getDeployment(tmp.getDeploymentName(), tmp.getCommonVersion());
        assertNotNull(deployment);
        assertEquals(tmp.getName(), deployment.getDeploymentName());
        assertEquals(tmp.getCommonVersion(), deployment.getCommonVersion());
    }

    @Test
    public void testAddListener() {
        assertEquals(0, dataSource.listeners.size());
        DataSourceListener dataSourceListener = new DataSourceListener() {
            public void onDeploymentAdded() {
            }
        };
        dataSource.addListener(dataSourceListener);
        assertEquals(1, dataSource.listeners.size());
    }

    @Test
    public void testRemoveListener() {
        assertEquals(0, dataSource.listeners.size());
        DataSourceListener dataSourceListener = new DataSourceListener() {
            public void onDeploymentAdded() {
            }
        };
        dataSource.addListener(dataSourceListener);
        assertEquals(1, dataSource.listeners.size());
        dataSource.removeListener(dataSourceListener);
        assertEquals(0, dataSource.listeners.size());
    }
}
