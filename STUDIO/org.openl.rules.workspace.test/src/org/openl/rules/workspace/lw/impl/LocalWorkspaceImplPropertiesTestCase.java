package org.openl.rules.workspace.lw.impl;

import junit.framework.TestCase;
import org.openl.SmartProps;
import org.openl.rules.workspace.TestHelper;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.dtr.impl.RepositoryVersionInfoImpl;
import org.openl.rules.workspace.lw.LWTestHelper;
import org.openl.rules.workspace.lw.LocalProject;
import org.openl.rules.workspace.lw.LocalProjectFolder;
import org.openl.rules.workspace.lw.LocalProjectResource;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.PropertyTypeException;
import org.openl.rules.workspace.props.impl.PropertyImpl;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * Tests correct working of <code>LocalProjectImpl</code> with properties. 
 */
public class LocalWorkspaceImplPropertiesTestCase extends TestCase {
    private LocalProjectImpl localProject;
    private LocalProjectFolder folder1;
    private LocalProjectResource folder1File1;
    private LocalProjectResource folder1File2;
    private LocalProjectFolder folder2;

    private final String PROJECT_NAME = "sample";

    @Override
    protected void setUp() throws Exception {
        TestHelper.ensureTestFolderExistsAndClear();

        LocalWorkspaceImpl workspace = getFreshWorkspace();

        localProject = (LocalProjectImpl) workspace.addProject(
                new LocalProjectImpl(PROJECT_NAME, new ArtefactPathImpl("sample"),
                new File(TestHelper.FOLDER_TEST),
                new RepositoryProjectVersionImpl(1, 0, 0, new RepositoryVersionInfoImpl(new Date(), "test")),
                        workspace));

        folder1 = localProject.addFolder("folder1");
        folder2 = localProject.addFolder("folder2");
        File folder1File = new File(localProject.getLocation(), folder1.getName());
        TestHelper.createEmptyFile(folder1File, "file1");
        TestHelper.createEmptyFile(folder1File, "file2");
        folder1.refresh();
        folder1File1 = (LocalProjectResource) folder1.getArtefact("file1");
        folder1File2 = (LocalProjectResource) folder1.getArtefact("file2");
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
        localProject.save();

        final String folderPropFolder = LocalProjectImpl.PROPERTIES_FOLDER + File.separator + LocalProjectImpl.FOLDER_PROPERTIES_FOLDER;
        assertTrue(folderPropFolder + " directory was not created in project root folder",
                new File(localProject.getLocation(), folderPropFolder).isDirectory());
        assertTrue(folderPropFolder + " directory was not created in folder1",
                new File(((LocalProjectFolderImpl) folder1).getLocation(), folderPropFolder).isDirectory());
        assertTrue(folderPropFolder + " directory was not created in folder2",
                new File(((LocalProjectFolderImpl) folder2).getLocation(), folderPropFolder).isDirectory());

        
        LocalProject project = getFreshWorkspace().getProject(PROJECT_NAME);
        ProjectArtefact folder1New = project.getArtefact("folder1");
        ProjectArtefact folder2New = project.getArtefact("folder2");
        assertEquals("property not restored", "paranoia", folder1New.getProperty("p1").getValue());
        assertEquals("property not restored", now, folder1New.getProperty("p2").getValue());
        assertEquals("property not restored", "xyz", folder2New.getProperty("p1").getValue());
        assertEquals("property not restored", "file1", folder1New.getArtefact("file1").getProperty("p3").getValue());
        assertEquals("property not restored", now, folder1New.getArtefact("file1").getProperty("p4").getValue());
        assertEquals("property not restored", "file2", folder1New.getArtefact("file2").getProperty("p5").getValue());
        assertEquals("property not restored", now, folder1New.getArtefact("file2").getProperty("p6").getValue());
    }

    public void testDoubleSave() throws ProjectException, PropertyTypeException {
        Date now = new Date();
        folder1.addProperty(new PropertyImpl("p1", "paranoia"));
        folder1.addProperty(new PropertyImpl("p2", now));
        folder2.addProperty(new PropertyImpl("p1", "xyz"));

        localProject.save();
        localProject.save();
    }

    private static LocalWorkspaceImpl getFreshWorkspace() throws WorkspaceException {
        Properties properties = new Properties();
        properties.put(LocalWorkspaceManagerImpl.PROP_WS_LOCATION, TestHelper.FOLDER_TEST);

        return new LocalWorkspaceManagerImpl(new SmartProps(properties)).createWorkspace(LWTestHelper.getTestUser());
    }
}
