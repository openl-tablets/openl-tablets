package org.openl.rules.workspace.production.client;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.TestHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.openl.rules.workspace.TestHelper.ensureTestFolderExistsAndClear;
import static org.openl.rules.workspace.TestHelper.getWorkspaceUser;
import static org.openl.rules.workspace.TestHelper.deleteTestFolder;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.impl.jcr.JcrProductionDeployer;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.workspace.mock.MockRepository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;

public class JcrRulesClientTestCase {
    private static final String FOLDER1 = "folder1";
    private static final String FILE1_1 = "file1_1";

    private static final String FILE1_2 = "file1_2";
    private static final String FOLDER2 = "folder2";
    private static final String PROJECT_NAME = "project";
    private static final String PROJECT_NAME2 = "project2";
    private JcrRulesClient instance;
    private AProject project;
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;
    private Repository repository;

    private JcrProductionDeployer getDeployer() throws DeploymentException {
        return new JcrProductionDeployer(productionRepositoryFactoryProxy, ProductionRepositoryFactoryProxy.DEFAULT_REPOSITORY_PROPERTIES_FILE);
    }

    private AProject makeProject() throws ProjectException {
        AProject project = new AProject(repository, PROJECT_NAME, true);
        AProjectFolder folder1 = project.addFolder(FOLDER1);
        folder1.addResource(FILE1_1, new ByteArrayInputStream(new byte[10]));
        folder1.addResource(FILE1_2, new ByteArrayInputStream(new byte[20]));
        project.addFolder(FOLDER2);
        return project;
    }

    private AProject makeProject2() throws ProjectException {
        AProject project = new AProject(repository, PROJECT_NAME2, true);
        AProjectFolder folder1 = project.addFolder(FOLDER1);
        folder1.addResource(FILE1_2, new ByteArrayInputStream(new byte[42]));
        return project;
    }

    @Before
    public void setUp() throws Exception {
        ensureTestFolderExistsAndClear();
        
        productionRepositoryFactoryProxy = new ProductionRepositoryFactoryProxy();
        repository = new MockRepository();

        instance = new JcrRulesClient(productionRepositoryFactoryProxy, ProductionRepositoryFactoryProxy.DEFAULT_REPOSITORY_PROPERTIES_FILE);

        project = makeProject();
    }

    @After
    public void tearDown() throws Exception {
        productionRepositoryFactoryProxy.destroy();
        deleteTestFolder();
    }

    @Test
    public void testFetchProject() throws Exception {
        JcrProductionDeployer deployer = getDeployer();
        DeployID id = deployer.deploy(new ADeploymentProject(null, repository, "deployment project", null),
                Collections.singletonList(project), getWorkspaceUser());

        File destDir = new File(TestHelper.FOLDER_TEST, "download");
        instance.fetchDeployment(id, destDir);

        File file1_2 = new File(destDir, PROJECT_NAME + "/" + FOLDER1 + "/" + FILE1_2);
        assertEquals(20L, file1_2.length());
    }

    @Ignore("Not actual anymore. With new API same project can be deployed second time without changes. It won't throw an exception.")
    @Test
    public void testFetchRedeployedProject() throws Exception {
        JcrProductionDeployer deployer = getDeployer();
        AProject project2 = makeProject2();
        deployer.deploy(new ADeploymentProject(null, repository, "deployment project", null),
                Collections.singletonList(project2), getWorkspaceUser());

        File destDir = new File(TestHelper.FOLDER_TEST);
        TestHelper.clearDirectory(destDir);

        try {
            deployer.deploy(new ADeploymentProject(null, repository, "deployment project", null),
                    Collections.singletonList(project2), getWorkspaceUser());
            fail("exception expected");
        } catch (DeploymentException e) {
            // ok
        }
    }
}
