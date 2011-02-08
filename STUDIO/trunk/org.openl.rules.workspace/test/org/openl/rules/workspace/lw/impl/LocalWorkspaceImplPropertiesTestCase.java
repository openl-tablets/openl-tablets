package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.openl.rules.common.ProjectDependency;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.impl.ProjectDependencyImpl;
import org.openl.rules.common.impl.PropertyImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.impl.local.LocalArtefactAPI;
import org.openl.rules.workspace.TestHelper;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.mock.MockFolder;

import junit.framework.TestCase;

/**
 * Tests correct working of <code>LocalProjectImpl</code> with properties.
 */
public class LocalWorkspaceImplPropertiesTestCase extends TestCase {
    private AProject localProject;
    private AProjectFolder folder1;
    private AProjectResource folder1File1;
    private AProjectResource folder1File2;
    private AProjectFolder folder2;
    private final String PROJECT_NAME = "sample";

    private static File getLocationForLocalArtefact(AProjectArtefact artefact) {
        if (artefact.getAPI() instanceof LocalArtefactAPI) {
            return ((LocalArtefactAPI) artefact.getAPI()).getSource();
        } else {
            throw new RuntimeException("It is not local artefact");
        }
    }

    private static File getPropertiesFileForFolderArtefact(AProjectFolder folder) {
        File propertiesFolder = new File(getLocationForLocalArtefact(folder.getProject()),
                FolderHelper.PROPERTIES_FOLDER);
        return new File(propertiesFolder, folder.getArtefactPath().withoutFirstSegment().getStringValue()
                + (File.separator + FolderHelper.FOLDER_PROPERTIES_FILE));
    }

    // TODO: fix

    public void testFake() {
    }

    @Override
    protected void setUp() throws Exception {
        TestHelper.ensureTestFolderExistsAndClear();

        LocalWorkspaceImpl workspace = getFreshWorkspace();

        localProject = workspace.addProject(new AProject(new MockFolder("sample")));
        // new RepositoryProjectVersionImpl(1, 0, 0, new
        // RepositoryVersionInfoImpl(new Date(), "test"))

        folder1 = localProject.addFolder("folder1");
        folder2 = localProject.addFolder("folder2");
        File folder1File = new File(getLocationForLocalArtefact(localProject), folder1.getName());
        TestHelper.createEmptyFile(folder1File, "file1");
        TestHelper.createEmptyFile(folder1File, "file2");
        folder1.refresh();
        folder1File1 = (AProjectResource) folder1.getArtefact("file1");
        folder1File2 = (AProjectResource) folder1.getArtefact("file2");
    }

    public void testProperties() throws ProjectException, IOException, PropertyException, WorkspaceException {
        Date now = new Date();
        folder1.addProperty(new PropertyImpl("p1", "paranoia"));
        folder1.addProperty(new PropertyImpl("p2", now));
        folder2.addProperty(new PropertyImpl("p1", "xyz"));

        folder1File1.addProperty(new PropertyImpl("p3", "file1"));
        folder1File1.addProperty(new PropertyImpl("p4", now));
        folder1File2.addProperty(new PropertyImpl("p5", "file2"));
        folder1File2.addProperty(new PropertyImpl("p6", now));
        localProject.checkIn(TestHelper.getWorkspaceUser());

        assertTrue("Properties file was not created for project root folder",
                getPropertiesFileForFolderArtefact(localProject).exists());
        assertTrue("Properties file was not created for folder1", getPropertiesFileForFolderArtefact(folder1).exists());
        assertTrue("Properties file was not created for folder2", getPropertiesFileForFolderArtefact(folder2).exists());

        AProject project = getFreshWorkspace().getProject(PROJECT_NAME);
        AProjectFolder folder1New = (AProjectFolder) project.getArtefact("folder1");
        AProjectFolder folder2New = (AProjectFolder) project.getArtefact("folder2");
        assertEquals("property not restored", "paranoia", folder1New.getProperty("p1").getValue());
        assertEquals("property not restored", now, folder1New.getProperty("p2").getValue());
        assertEquals("property not restored", "xyz", folder2New.getProperty("p1").getValue());
        assertEquals("property not restored", "file1", folder1New.getArtefact("file1").getProperty("p3").getValue());
        assertEquals("property not restored", now, folder1New.getArtefact("file1").getProperty("p4").getValue());
        assertEquals("property not restored", "file2", folder1New.getArtefact("file2").getProperty("p5").getValue());
        assertEquals("property not restored", now, folder1New.getArtefact("file2").getProperty("p6").getValue());
    }

    public void testDoubleSave() throws ProjectException, PropertyException {
        Date now = new Date();
        folder1.addProperty(new PropertyImpl("p1", "paranoia"));
        folder1.addProperty(new PropertyImpl("p2", now));
        folder2.addProperty(new PropertyImpl("p1", "xyz"));

        localProject.checkIn(TestHelper.getWorkspaceUser());
        localProject.checkIn(TestHelper.getWorkspaceUser());
    }

    public void testPropertyFolderIgnored() throws ProjectException, WorkspaceException {
        localProject.checkIn(TestHelper.getWorkspaceUser());
        AProject project = getFreshWorkspace().getProject(PROJECT_NAME);
        try {
            project.getArtefact(FolderHelper.PROPERTIES_FOLDER);
            fail("Exception not thrown");
        } catch (ProjectException e) {
            // ok
        }
    }

    public void testFixedDateProperties() throws ProjectException, WorkspaceException, PropertyException {
        Date date1 = new Date(System.currentTimeMillis());
        Date date2 = new Date(date1.getTime() + 1);
        Date date3 = new Date(date2.getTime() + 1);
        Date date4 = new Date(date3.getTime() + 1);
        Date date5 = new Date(date4.getTime() + 1);
        Date date6 = new Date(date5.getTime() + 1);

        localProject.setEffectiveDate(date1);
        localProject.setExpirationDate(date2);
        folder1.setEffectiveDate(date3);
        folder1.setExpirationDate(date4);
        folder1File2.setEffectiveDate(date5);
        folder1File2.setExpirationDate(date6);
        localProject.checkIn(TestHelper.getWorkspaceUser());

        AProject project = getFreshWorkspace().getProject(PROJECT_NAME);
        AProjectFolder folder1 = (AProjectFolder) project.getArtefact("folder1");
        AProjectArtefact folder1File2 = folder1.getArtefact("file2");

        assertEquals("effective date for project was not persisted corectly", date1, project.getEffectiveDate());
        assertEquals("expiration date for project was not persisted corectly", date2, project.getExpirationDate());
        assertEquals("effective date for folder was not persisted corectly", date3, folder1.getEffectiveDate());
        assertEquals("expiration date for folder was not persisted corectly", date4, folder1.getExpirationDate());
        assertEquals("effective date for file was not persisted corectly", date5, folder1File2.getEffectiveDate());
        assertEquals("expiration date for file was not persisted corectly", date6, folder1File2.getExpirationDate());
    }

    public void testProjectDependency() throws WorkspaceException, ProjectException {
        ProjectDependency[] dependencies = {
                new ProjectDependencyImpl("project1", new RepositoryProjectVersionImpl(1, 0, 0, null)),
                new ProjectDependencyImpl("project2", new RepositoryProjectVersionImpl(2, 1, 0, null),
                        new RepositoryProjectVersionImpl(2, 2, 0, null)) };

        localProject.setDependencies(Arrays.asList(dependencies));
        localProject.checkIn(TestHelper.getWorkspaceUser());

        AProject project = getFreshWorkspace().getProject(PROJECT_NAME);
        List<ProjectDependency> deps = new ArrayList<ProjectDependency>(project.getDependencies());
        assertEquals("dependency collection size changed", dependencies.length, deps.size());
        for (int i = 0; i < dependencies.length; i++) {
            assertEquals("dependency is incorrect in position " + i, dependencies[i], deps.get(i));
        }
    }

    public void testProjectDependencyNotNull() throws ProjectException, WorkspaceException {
        localProject.checkIn(TestHelper.getWorkspaceUser());

        AProject project = getFreshWorkspace().getProject(PROJECT_NAME);
        assertNotNull("dependencies are null", project.getDependencies());
        assertEquals("dependencies are not empty", 0, project.getDependencies().size());
    }

    private static LocalWorkspaceImpl getFreshWorkspace() throws WorkspaceException {
        LocalWorkspaceManagerImpl workspaceManager = new LocalWorkspaceManagerImpl();
        workspaceManager.setWorkspacesRoot(TestHelper.FOLDER_TEST);
        workspaceManager.setLocalWorkspaceFileFilter(new NotFileFilter(new NameFileFilter(".studioProps")));
        return workspaceManager.createWorkspace(TestHelper.getWorkspaceUser());
    }

}
