package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.impl.PropertyImpl;
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
        localProject.save(TestHelper.getWorkspaceUser());

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

        localProject.save(TestHelper.getWorkspaceUser());
        localProject.save(TestHelper.getWorkspaceUser());
    }

    public void testPropertyFolderIgnored() throws ProjectException, WorkspaceException {
        localProject.save(TestHelper.getWorkspaceUser());
        AProject project = getFreshWorkspace().getProject(PROJECT_NAME);
        try {
            project.getArtefact(FolderHelper.PROPERTIES_FOLDER);
            fail("Exception not thrown");
        } catch (ProjectException e) {
            // ok
        }
    }

    private static LocalWorkspaceImpl getFreshWorkspace() throws WorkspaceException {
        LocalWorkspaceManagerImpl workspaceManager = new LocalWorkspaceManagerImpl();
        workspaceManager.setWorkspaceHome(TestHelper.FOLDER_TEST);
        workspaceManager.setLocalWorkspaceFileFilter(new FileFilter() {
            @Override public boolean accept(File file) {
                return !".studioProps".equals(file.getName());
            }
        });
        return workspaceManager.createWorkspace(TestHelper.getWorkspaceUser());
    }

}
