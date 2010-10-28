package org.openl.rules.workspace.deploy.impl.jcr;

import junit.framework.TestCase;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.RProductionDeployment;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.exceptions.RRepositoryException;
import static org.openl.rules.workspace.TestHelper.ensureTestFolderExistsAndClear;
import static org.openl.rules.workspace.TestHelper.getWorkspaceUser;
import static org.openl.rules.workspace.TestHelper.deleteTestFolder;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.mock.MockProject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JcrProductionDeployerTestCase extends TestCase {
    private static final String PROJECT1_NAME = "project1";
    private static final String PROJECT2_NAME = "project2";
    private static final String PROJECT3_NAME = "project3";
    private static final Date EFFECTIVE_DATE = new Date();
    private static final Date EXPIRATION_DATE = new Date(EFFECTIVE_DATE.getTime() + 2 * 60 * 60 * 1000);
    private static final String LOB = "management";
    private static final String ATTRIBUTE = "attribute";

    private static final Object[] propData = { "attr1", "attr2", "attr3", "attr4", "attr5", new Date(6), new Date(7),
            new Date(8), new Date(9), new Date(10), 11D, 12D, 13D, 14D, 15D, };
    private static final String FOLDER1 = "folder1";
    private static final String FILE1_1 = "file1_1";

    private static final String FILE1_2 = "file1_2";
    private static final String FOLDER2 = "folder2";
    /**
     * <code>JcrProductionDeployer</code> instance to be used in tests.
     */
    private JcrProductionDeployer instance;

    private Map<String, Object> props;

    private Project project1;
    private Project project2;
    private Project project3;
    private List<Project> projects;

    private static REntity getEntityByName(Iterable<? extends REntity> collection, String name) {
        for (REntity rEntity : collection) {
            if (rEntity.getName().equals(name)) {
                return rEntity;
            }
        }
        return null;
    }

    private Project makeProject() {
        return (Project) new MockProject(PROJECT1_NAME).addFolder(FOLDER1).addFile(FILE1_1).up().addFile(FILE1_2).up()
                .up().addFolder(FOLDER2).up();
    }

    private Project makeProject2() {
        return (Project) new MockProject(PROJECT2_NAME).addFolder(FOLDER1)._setEffectiveDate(EFFECTIVE_DATE).addFile(
                FILE1_1).setInputStream(new ByteArrayInputStream(new byte[15]))._setExpirationDate(EXPIRATION_DATE)
                .up().addFile(FILE1_2)._setLineOfBusiness(LOB).up().up().addFolder(FOLDER2).up();
    }

    private Project makeProject3() {
        props = new HashMap<String, Object>();
        for (int i = 0; i < propData.length; i++) {
            props.put(ATTRIBUTE + i + 1, propData[i]);
        }
        return (Project) new MockProject(PROJECT3_NAME).addFolder(FOLDER1).addFile(FILE1_1)._setProps(props).up().up();
    }

    @Override
    protected void setUp() throws Exception {
        ensureTestFolderExistsAndClear();

        instance = new JcrProductionDeployer(getWorkspaceUser());

        project1 = makeProject();
        project2 = makeProject2();

        project3 = makeProject3();

        projects = new ArrayList<Project>();
        projects.add(project1);
        projects.add(project2);
        projects.add(project3);
    }

    @Override
    protected void tearDown() throws Exception {
        ProductionRepositoryFactoryProxy.release();
        deleteTestFolder();
    }

    public void testDeploy() throws DeploymentException, RRepositoryException, IOException {
        DeployID id = instance.deploy(projects);

        ProductionRepositoryFactoryProxy.reset();

        RProductionRepository pr = ProductionRepositoryFactoryProxy.getRepositoryInstance();
        assertTrue(pr.hasDeployment(id.getName()));
        final Collection<String> names = pr.getDeploymentNames();
        assertTrue(names.contains(id.getName()));

        RProductionDeployment deployment = pr.getDeployment(id.getName());
        assertTrue(deployment.hasProject(PROJECT1_NAME));
        assertTrue(deployment.hasProject(PROJECT2_NAME));

        RProject rProject = deployment.getProject(PROJECT2_NAME);

        RFolder folder1 = (RFolder) getEntityByName(rProject.getRootFolder().getFolders(), FOLDER1);

        assertNotNull(folder1);
        assertEquals(EFFECTIVE_DATE, folder1.getEffectiveDate());
        assertNull(folder1.getExpirationDate());
        assertNull(folder1.getLineOfBusiness());

        RFile theFile1 = (RFile) getEntityByName(folder1.getFiles(), FILE1_1);

        assertNotNull(theFile1);
        assertEquals(15, theFile1.getContent().available());
        assertEquals(EXPIRATION_DATE, theFile1.getExpirationDate());

        RFile theFile2 = (RFile) getEntityByName(folder1.getFiles(), FILE1_2);
        assertNotNull(theFile2);
        assertEquals(LOB, theFile2.getLineOfBusiness());
        assertNull(theFile2.getEffectiveDate());
        assertNull(theFile2.getExpirationDate());

        RProject project3 = deployment.getProject(PROJECT3_NAME);

        folder1 = (RFolder) getEntityByName(project3.getRootFolder().getFolders(), FOLDER1);

        assertNotNull(folder1);

        theFile1 = (RFile) getEntityByName(folder1.getFiles(), FILE1_1);

        assertNotNull(theFile1);

        final Map<String, Object> fileProps = theFile1.getProps();

        if (props != null) {
            assertNotNull(fileProps);
            assertTrue(!fileProps.isEmpty());
            assertTrue(fileProps.size() == props.size());
            for (int i = 1; i <= propData.length; i++) {
                assertEquals(fileProps.get(ATTRIBUTE + i), props.get(ATTRIBUTE + i));
            }
        }
    }

    public void testDeploySameId() throws DeploymentException {
        List<Project> projects = Collections.singletonList(project1);
        DeployID id = instance.deploy(projects);
        try {
            instance.deploy(id, projects);
            fail("exception expected");
        } catch (DeploymentException e) {
            assertNull(e.getCause());
        }
    }

}
