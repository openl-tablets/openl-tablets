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
import java.util.List;

public class JcrProductionDeployerTestCase extends TestCase {
    /**
     * <code>JcrProductionDeployer</code> instance to be used in tests.
     */
    private JcrProductionDeployer instance;
    private static final String PROJECT1_NAME = "project1";
    private static final String PROJECT2_NAME = "project2";
    private static final String PROJECT3_NAME = "project3";
    private static final Date EFFECTIVE_DATE = new Date();
    private static final Date EXPIRATION_DATE = new Date(EFFECTIVE_DATE.getTime() + 2*60*60*1000);
    private static final String LOB = "management";
    
    private static final String ATTRIBUTE1 = "attribute1";
    private static final String ATTRIBUTE2 = "attribute2";
    private static final String ATTRIBUTE3 = "attribute3";
    private static final String ATTRIBUTE4 = "attribute4";
    private static final String ATTRIBUTE5 = "attribute5";
    private static final Date ATTRIBUTE6 = new Date(6);
    private static final Date ATTRIBUTE7 = new Date(7);
    private static final Date ATTRIBUTE8 = new Date(8);
    private static final Date ATTRIBUTE9 = new Date(9);
    private static final Date ATTRIBUTE10 = new Date(10);
    private static final Double ATTRIBUTE11 = 11d;
    private static final Double ATTRIBUTE12 = 12d;
    private static final Double ATTRIBUTE13 = 13d;
    private static final Double ATTRIBUTE14 = 14d;
    private static final Double ATTRIBUTE15 = 15d;

    private Project project1;
    private Project project2;
    private Project project3;

    private List<Project> projects;
    
    private static final String FOLDER1 = "folder1";
    private static final String FILE1_1 = "file1_1";
    private static final String FILE1_2 = "file1_2";
    private static final String FOLDER2 = "folder2";


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

    private Project makeProject() {
        return (Project)
                new MockProject(PROJECT1_NAME).
                        addFolder(FOLDER1)
                            .addFile(FILE1_1).up()
                            .addFile(FILE1_2).up()
                        .up()
                            .addFolder(FOLDER2)
                        .up();
    }

    private Project makeProject2() {
        return (Project)
                new MockProject(PROJECT2_NAME).
                        addFolder(FOLDER1)._setEffectiveDate(EFFECTIVE_DATE)
                            .addFile(FILE1_1).setInputStream(new ByteArrayInputStream(new byte[15]))._setExpirationDate(EXPIRATION_DATE).up()
                            .addFile(FILE1_2)._setLineOfBusiness(LOB).up()
                        .up()
                            .addFolder(FOLDER2)
                        .up();
    }
    
    private Project makeProject3() {
        return (Project)
                new MockProject(PROJECT3_NAME).
                        addFolder(FOLDER1)
                            .addFile(FILE1_1)._setAttribute1(ATTRIBUTE1)
                                ._setAttribute2(ATTRIBUTE2)
                                ._setAttribute3(ATTRIBUTE3)
                                ._setAttribute4(ATTRIBUTE4)
                                ._setAttribute5(ATTRIBUTE5)
                                ._setAttribute6(ATTRIBUTE6)
                                ._setAttribute7(ATTRIBUTE7)
                                ._setAttribute8(ATTRIBUTE8)
                                ._setAttribute9(ATTRIBUTE9)
                                ._setAttribute10(ATTRIBUTE10)
                                ._setAttribute11(ATTRIBUTE11)
                                ._setAttribute12(ATTRIBUTE12)
                                ._setAttribute13(ATTRIBUTE13)
                                ._setAttribute14(ATTRIBUTE14)
                                ._setAttribute15(ATTRIBUTE15)
                            .up()
                        .up();
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

        RFile theFile1 = (RFile)getEntityByName(folder1.getFiles(),FILE1_1);

        assertNotNull(theFile1);
        assertEquals(15, theFile1.getContent().available());
        assertEquals(EXPIRATION_DATE, theFile1.getExpirationDate());

        RFile theFile2 = (RFile)getEntityByName(folder1.getFiles(),FILE1_2);
        assertNotNull(theFile2);
        assertEquals(LOB, theFile2.getLineOfBusiness());
        assertNull(theFile2.getEffectiveDate());
        assertNull(theFile2.getExpirationDate());
        
        RProject project3 = deployment.getProject(PROJECT3_NAME);

        folder1 = (RFolder) getEntityByName(project3.getRootFolder().getFolders(), FOLDER1);

        assertNotNull(folder1);
    
        theFile1 = (RFile)getEntityByName(folder1.getFiles(),FILE1_1);

        assertNotNull(theFile1);
        
        assertEquals(ATTRIBUTE1, theFile1.getAttribute1());
        assertEquals(ATTRIBUTE2, theFile1.getAttribute2());
        assertEquals(ATTRIBUTE3, theFile1.getAttribute3());
        assertEquals(ATTRIBUTE4, theFile1.getAttribute4());
        assertEquals(ATTRIBUTE5, theFile1.getAttribute5());
        assertEquals(ATTRIBUTE6, theFile1.getAttribute6());
        assertEquals(ATTRIBUTE7, theFile1.getAttribute7());
        assertEquals(ATTRIBUTE8, theFile1.getAttribute8());
        assertEquals(ATTRIBUTE9, theFile1.getAttribute9());
        assertEquals(ATTRIBUTE10, theFile1.getAttribute10());
        assertEquals(ATTRIBUTE11, theFile1.getAttribute11());
        assertEquals(ATTRIBUTE12, theFile1.getAttribute12());
        assertEquals(ATTRIBUTE13, theFile1.getAttribute13());
        assertEquals(ATTRIBUTE14, theFile1.getAttribute14());
        assertEquals(ATTRIBUTE15, theFile1.getAttribute15());
    }

    private static REntity getEntityByName(Iterable<? extends REntity> collection, String name) {
        for (REntity rEntity : collection)
            if (rEntity.getName().equals(name))
                return rEntity;
        return null;
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
