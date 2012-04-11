//package org.openl.rules.workspace.production.client;
//
//import junit.framework.TestCase;
//import org.openl.rules.workspace.TestHelper;
//import static org.openl.rules.workspace.TestHelper.ensureTestFolderExistsAndClear;
//import static org.openl.rules.workspace.TestHelper.getWorkspaceUser;
//import static org.openl.rules.workspace.TestHelper.deleteTestFolder;
//import org.openl.rules.workspace.deploy.DeployID;
//import org.openl.rules.workspace.deploy.DeploymentException;
//import org.openl.rules.workspace.deploy.impl.jcr.JcrProductionDeployer;
//import org.openl.rules.workspace.mock.MockProject;
//import org.openl.rules.project.abstraction.AProject;
//import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
//
//import java.io.ByteArrayInputStream;
//import java.io.File;
//import java.util.Collections;
//
//public class JcrRulesClientTestCase extends TestCase {
//    private static final String FOLDER1 = "folder1";
//    private static final String FILE1_1 = "file1_1";
//
//    private static final String FILE1_2 = "file1_2";
//    private static final String FOLDER2 = "folder2";
//    private static final String PROJECT_NAME = "project";
//    private static final String PROJECT_NAME2 = "project2";
//    private JcrRulesClient instance;
//    private AProject project;
//
//    private JcrProductionDeployer getDeployer() throws DeploymentException {
//        return new JcrProductionDeployer(getWorkspaceUser());
//    }
//
//    private AProject makeProject() {
//        return (AProject) new MockProject(PROJECT_NAME).addFolder(FOLDER1).addFile(FILE1_1).setInputStream(
//                new ByteArrayInputStream(new byte[10])).up().addFile(FILE1_2).setInputStream(
//                new ByteArrayInputStream(new byte[20])).up().up().addFolder(FOLDER2).up();
//    }
//
//    private AProject makeProject2() {
//        return (AProject) new MockProject(PROJECT_NAME2).addFolder(FOLDER1).addFile(FILE1_2).setInputStream(
//                new ByteArrayInputStream(new byte[42])).up().up();
//    }
//
//    @Override
//    protected void setUp() throws Exception {
//        ensureTestFolderExistsAndClear();
//
//        instance = new JcrRulesClient();
//
//        project = makeProject();
//    }
//
//    @Override
//    protected void tearDown() throws Exception {
//        ProductionRepositoryFactoryProxy.release();
//        deleteTestFolder();
//    }
//
//    public void testFetchProject() throws Exception {
//        JcrProductionDeployer deployer = getDeployer();
//        DeployID id = deployer.deploy(Collections.singletonList(project));
//
//        File destDir = new File(TestHelper.FOLDER_TEST, "download");
//        instance.fetchDeployment(id, destDir);
//
//        File file1_2 = new File(destDir, PROJECT_NAME + "/" + FOLDER1 + "/" + FILE1_2);
//        assertEquals(20L, file1_2.length());
//    }
//
//    public void testFetchRedeployedProject() throws Exception {
//        JcrProductionDeployer deployer = getDeployer();
//        DeployID id = deployer.deploy(Collections.singletonList(makeProject2()));
//
//        File destDir = new File(TestHelper.FOLDER_TEST);
//        TestHelper.clearDirectory(destDir);
//
//        try {
//            deployer.deploy(id, Collections.singletonList(makeProject2()));
//            fail("exception expected");
//        } catch (DeploymentException e) {
//            // ok
//        }
//    }
//}
