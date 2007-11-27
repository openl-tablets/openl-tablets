package org.openl.rules.workspace.deploy.impl.jcr;

import junit.framework.TestCase;
import org.openl.SmartProps;
import static org.openl.rules.workspace.TestHelper.*;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.mock.MockProject;
import org.openl.rules.workspace.TestHelper;

import java.io.File;
import java.util.Collections;
import java.util.Properties;
import java.util.List;

public class JcrProductionDeployerTestCase extends TestCase {
    /**
     * <code>JcrProductionDeployer</code> instance to be used in tests.
     */
    private JcrProductionDeployer instance;
    private Project project;
    private static final String FOLDER1 = "folder1";
    private static final String FILE1_1 = "file1_1";
    private static final String FILE1_2 = "file1_2";
    private static final String FOLDER2 = "folder2";


    @Override
    protected void setUp() throws Exception {
        ensureTestFolderExistsAndClear();

        Properties properties = new Properties();
        properties.put(JcrProductionDeployer.PROPNAME_ZIPFOLDER, FOLDER_TEST);

        instance = new JcrProductionDeployer(getWorkspaceUser(), new SmartProps(properties));

        project = makeProject();
    }

    private Project makeProject() {
        return (Project)
                new MockProject("project").
                        addFolder(FOLDER1)
                            .addFile(FILE1_1).up()
                            .addFile(FILE1_2).up()
                        .up()
                            .addFolder(FOLDER2)
                        .up();
    }

    public void testTempFolder() {
        assertTrue("temp folder for test user was not created", userWorkingFolder().exists());
        assertTrue("working folder created not where it was intended to", FolderHelper.isParent(new File(FOLDER_TEST), userWorkingFolder()));
    }

    public void testDownloadProjects() throws DeploymentException {
        instance.downloadProjects(Collections.singletonList(project));

        File projectFolder = new File(userWorkingFolder(), project.getName());
        assertTrue("project folder was not created", projectFolder.exists());
        File folder1 = new File(projectFolder, FOLDER1);
        assertTrue("folder was not created inside the project", folder1.exists());
        assertTrue("filel was not created", new File(folder1, FILE1_1).exists());
        assertTrue("filel was not created", new File(folder1, FILE1_2).exists());
        assertTrue("folder was not created inside the project", new File(projectFolder, FOLDER2).exists());
    }

    public void testDownloadProjects_TWICE() throws DeploymentException {
        testDownloadProjects();
        testDownloadProjects();
    }

    public void testDeployProjects() throws DeploymentException {
        instance.deploy(Collections.singletonList(project));

        File zipFile = new File(new File(TestHelper.FOLDER_TEST, TestHelper.getWorkspaceUser().getUserId()),
                JcrProductionDeployer.ZIP_FILE_NAME);
        assertTrue("zip archive was not created", zipFile.exists());
    }

    public void testDeployProjectsTheSameName() throws DeploymentException {
        List<Project> projects = Collections.singletonList(project);
        DeployID id = instance.deploy(projects);
        instance.deploy(id, projects);
    }

    
    private  File userWorkingFolder() {
        return instance.getWorkingFolder();
    }
}
