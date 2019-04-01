package org.openl.rules.ruleservice.loader;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.project.abstraction.Deployment;

public class FileSystemDataSourceTest {
    private FileSystemDataSource dataSource;

    private static String FILE_SYSTEM_DATA_SOURCE_DIRECTORY = "target/filesystemdatasource";

    @Before
    public void setDataSource() {
        dataSource = new FileSystemDataSource(new File(FILE_SYSTEM_DATA_SOURCE_DIRECTORY));
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
}
