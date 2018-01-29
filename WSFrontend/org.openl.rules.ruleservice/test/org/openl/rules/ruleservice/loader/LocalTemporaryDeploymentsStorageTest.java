package org.openl.rules.ruleservice.loader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.project.abstraction.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "production-repository.factory = org.openl.rules.repository.file.FileRepository",
        "production-repository.uri = test-resources/openl-repository",
        "version-in-deployment-name = true"
})
@ContextConfiguration({ "classpath:openl-ruleservice-property-placeholder.xml",
        "classpath:openl-ruleservice-datasource-beans.xml" })
public class LocalTemporaryDeploymentsStorageTest {

    @Autowired
    @Qualifier("productionRepositoryDataSource")
    private DataSource dataSource;

    private Deployment deployment;

    @Before
    public void getDeployment() {
        Collection<Deployment> deployments = dataSource.getDeployments();
        assertTrue(!deployments.isEmpty());
        deployment = deployments.iterator().next();
    }

    @Test
    public void testContainsDeloymentAndLoadDeployment() {
        LocalTemporaryDeploymentsStorage storage = new LocalTemporaryDeploymentsStorage("target/openl-deploy");
        assertFalse(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
    }

    @Test
    public void testLoadDeployment() {
        LocalTemporaryDeploymentsStorage storage = new LocalTemporaryDeploymentsStorage("target/openl-deploy");
        assertNull(storage.getDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        assertFalse(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
    }

    @Test
    public void testGetDeployment() {
        LocalTemporaryDeploymentsStorage storage = new LocalTemporaryDeploymentsStorage("target/openl-deploy");
        assertNull(storage.getDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        assertFalse(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        storage.loadDeployment(deployment);
        assertTrue(storage.containsDeployment(deployment.getDeploymentName(), deployment.getCommonVersion()));
        Deployment deployment1 = storage.getDeployment(deployment.getDeploymentName(), deployment.getCommonVersion());
        assertNotNull(deployment1);
        Deployment deployment2 = storage.getDeployment(deployment.getDeploymentName(), deployment.getCommonVersion());
        assertTrue(deployment1 == deployment2);
    }

}
