package org.openl.ruleservice.loader;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.project.abstraction.Deployment;

public class LocalTemporaryDeploymentsStorageTest {

    private static IDataSource dataSource;

    private Deployment deployment;

    @BeforeClass
    public static void setDataSource() {
        dataSource = new JcrDataSource();
    }

    @Before
    public void getDeployment() {
        List<Deployment> deployments = dataSource.getDeployments();
        assertTrue(deployments.size() > 0);
        deployment = deployments.get(0);
    }

    @Test
    public void testContainsDeloymentAndLoadDeployment() {
        LocalTemporaryDeploymentsStorage storage = new LocalTemporaryDeploymentsStorage();
        assertFalse(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
    }

    @Test
    public void testClearStorage() {
        LocalTemporaryDeploymentsStorage storage = new LocalTemporaryDeploymentsStorage();
        assertFalse(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.clear();
        assertFalse(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
    }

    @Test
    public void testRemoveDeployment() {
        LocalTemporaryDeploymentsStorage storage = new LocalTemporaryDeploymentsStorage();
        assertFalse(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.removeDeployment(deployment.getDeploymentName(), deployment.getCommonVersion());
        assertTrue(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
    }

    @Test
    public void testGetDeployment() {
        LocalTemporaryDeploymentsStorage storage = new LocalTemporaryDeploymentsStorage();
        assertNull(storage.getDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        assertFalse(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        assertNotNull(storage.getDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
    }

}
