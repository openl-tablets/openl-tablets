package org.openl.rules.workspace.deploy.impl.jcr;

import static org.openl.rules.workspace.TestHelper.deleteTestFolder;
import static org.openl.rules.workspace.TestHelper.ensureTestFolderExistsAndClear;
import static org.openl.rules.workspace.TestHelper.getWorkspaceUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.PropertyException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.mock.MockFolder;
import org.openl.rules.workspace.mock.MockResource;

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
    private ProductionRepositoryFactoryProxy productionRepositoryFactoryProxy;
    private JcrProductionDeployer instance;

    private Map<String, Object> props;

    private AProject project1;
    private AProject project2;
    private AProject project3;
    private List<AProject> projects;

    private AProject makeProject() throws ProjectException {
        AProject project = new AProject(new MockFolder(PROJECT1_NAME));
        AProjectFolder folder1 = project.addFolder(FOLDER1);
        folder1.addResource(FILE1_1, MockResource.NULL_STREAM);
        folder1.addResource(FILE1_2, MockResource.NULL_STREAM);
        project.addFolder(FOLDER2);
        return project;
    }

    private AProject makeProject2() throws PropertyException, ProjectException {
        AProject project = new AProject(new MockFolder(PROJECT2_NAME));
        AProjectFolder folder1 = project.addFolder(FOLDER1);
        folder1.addResource(FILE1_1, MockResource.NULL_STREAM);
        folder1.addResource(FILE1_2, MockResource.NULL_STREAM);
        project.addFolder(FOLDER2);
        return project;
    }

    private AProject makeProject3() throws ProjectException, PropertyException {
        props = new HashMap<String, Object>();
        for (int i = 0; i < propData.length; i++) {
            props.put(ATTRIBUTE + (i+1), propData[i]);
        }
        AProject project = new AProject(new MockFolder(PROJECT3_NAME));
        AProjectFolder folder1 = project.addFolder(FOLDER1);
        folder1.addResource(FILE1_1, MockResource.NULL_STREAM).setProps(props);
        return project;
    }

    @Override
    protected void setUp() throws Exception {
        ensureTestFolderExistsAndClear();

        productionRepositoryFactoryProxy = new ProductionRepositoryFactoryProxy();

        instance = new JcrProductionDeployer(productionRepositoryFactoryProxy, ProductionRepositoryFactoryProxy.DEFAULT_REPOSITORY_PROPERTIES_FILE);

        project1 = makeProject();
        project2 = makeProject2();

        project3 = makeProject3();

        projects = new ArrayList<AProject>();
        projects.add(project1);
        projects.add(project2);
        projects.add(project3);
    }

    @Override
    protected void tearDown() throws Exception {
        productionRepositoryFactoryProxy.destroy();
        deleteTestFolder();
    }

    public void testDeploy() throws IOException, ProjectException, PropertyException {
        ADeploymentProject deploymentProject = new ADeploymentProject(new MockFolder("deployment project"), null);
        DeployID id = instance.deploy(deploymentProject, projects, getWorkspaceUser());

        productionRepositoryFactoryProxy.destroy();

        RProductionRepository pr = productionRepositoryFactoryProxy.getRepositoryInstance(ProductionRepositoryFactoryProxy.DEFAULT_REPOSITORY_PROPERTIES_FILE);
        assertTrue(pr.hasDeploymentProject(id.getName()));
        final Collection<String> names = pr.getDeploymentProjectNames();
        assertTrue(names.contains(id.getName()));

        FolderAPI deployment = pr.getDeploymentProject(id.getName());
        assertTrue(deployment.hasArtefact(PROJECT1_NAME));
        assertTrue(deployment.hasArtefact(PROJECT2_NAME));
        assertTrue(deployment.hasArtefact(PROJECT3_NAME));

        AProject project = new AProject((FolderAPI)deployment.getArtefact(PROJECT2_NAME));

        AProjectFolder folder1 = (AProjectFolder) project.getArtefact(FOLDER1);

        assertNotNull(folder1);

        AProjectResource theFile1 = (AProjectResource)folder1.getArtefact(FILE1_1);

        assertNotNull(theFile1);

        AProjectResource theFile2 = (AProjectResource)folder1.getArtefact(FILE1_2);
        assertNotNull(theFile2);

        AProject project3 = new AProject((FolderAPI)deployment.getArtefact(PROJECT3_NAME));

        folder1 = (AProjectFolder) project3.getArtefact(FOLDER1);

        assertNotNull(folder1);

        theFile1 = (AProjectResource) folder1.getArtefact(FILE1_1);

        assertNotNull(theFile1);
        
        final Map<String, Object> fileProps = theFile1.getProps();

        if (props != null) {
            assertNotNull(fileProps);
            assertTrue(!fileProps.isEmpty());
            assertEquals(fileProps.size(), props.size());
            for (int i = 1; i <= propData.length; i++) {
                assertEquals(fileProps.get(ATTRIBUTE + i), props.get(ATTRIBUTE + i));
            }
        }
    }

    public void testDeploySameId() throws DeploymentException {
        List<AProject> projects = Collections.singletonList(project1);
        instance.deploy(new ADeploymentProject(new MockFolder("deployment project"), null), projects, getWorkspaceUser());
        try {
            instance.deploy(new ADeploymentProject(new MockFolder("deployment project"), null), projects, getWorkspaceUser());
            fail("exception expected");
        } catch (DeploymentException e) {
            assertNull(e.getCause());
        }
    }

}
