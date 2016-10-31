package org.openl.rules.workspace.production.client;

import junit.framework.TestCase;

import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.TestHelper;
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

public class JcrRulesClientTestCase extends TestCase {
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
        AProject project = new AProject(repository, PROJECT_NAME);
        AProjectFolder folder1 = project.addFolder(FOLDER1);
        folder1.addResource(FILE1_1, new ByteArrayInputStream(new byte[10]));
        folder1.addResource(FILE1_2, new ByteArrayInputStream(new byte[20]));
        project.addFolder(FOLDER2);
        return project;
    }

    private AProject makeProject2() throws ProjectException {
        AProject project = new AProject(repository, PROJECT_NAME2);
        AProjectFolder folder1 = project.addFolder(FOLDER1);
        folder1.addResource(FILE1_2, new ByteArrayInputStream(new byte[42]));
        return project;
    }

    @Override
    protected void setUp() throws Exception {
        ensureTestFolderExistsAndClear();
        
        productionRepositoryFactoryProxy = new ProductionRepositoryFactoryProxy();
        repository = new MockRepository();

        instance = new JcrRulesClient(productionRepositoryFactoryProxy, ProductionRepositoryFactoryProxy.DEFAULT_REPOSITORY_PROPERTIES_FILE);

        project = makeProject();
    }

    @Override
    protected void tearDown() throws Exception {
        productionRepositoryFactoryProxy.destroy();
        deleteTestFolder();
    }

    public void testFetchProject() throws Exception {
        JcrProductionDeployer deployer = getDeployer();
        DeployID id = deployer.deploy(new ADeploymentProject(null, repository, "deployment project", null),
                Collections.singletonList(project), getWorkspaceUser());

        File destDir = new File(TestHelper.FOLDER_TEST, "download");
        instance.fetchDeployment(id, destDir);

        File file1_2 = new File(destDir, PROJECT_NAME + "/" + FOLDER1 + "/" + FILE1_2);
        assertEquals(20L, file1_2.length());
    }

    public void testFetchRedeployedProject() throws Exception {
        JcrProductionDeployer deployer = getDeployer();
        deployer.deploy(new ADeploymentProject(null, repository, "deployment project", null),
                Collections.singletonList(makeProject2()), getWorkspaceUser());

        File destDir = new File(TestHelper.FOLDER_TEST);
        TestHelper.clearDirectory(destDir);

        try {
            deployer.deploy(new ADeploymentProject(null, repository, "deployment project", null),
                    Collections.singletonList(makeProject2()), getWorkspaceUser());
            fail("exception expected");
        } catch (DeploymentException e) {
            // ok
        }
    }
}
