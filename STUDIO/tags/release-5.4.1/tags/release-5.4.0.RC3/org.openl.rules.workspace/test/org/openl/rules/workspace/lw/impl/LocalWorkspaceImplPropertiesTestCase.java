package org.openl.rules.workspace.lw.impl;

import junit.framework.TestCase;
import org.openl.rules.workspace.lw.LocalProjectFolder;
import org.openl.rules.workspace.lw.LocalProjectResource;

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

    // TODO: fix

    public void testFake() {
    }
    /*
     * @Override protected void setUp() throws Exception {
     * TestHelper.ensureTestFolderExistsAndClear();
     *
     * LocalWorkspaceImpl workspace = getFreshWorkspace();
     *
     * localProject = (LocalProjectImpl) workspace.addProject(new
     * MockProject("sample"));
     *  // new RepositoryProjectVersionImpl(1, 0, 0, new //
     * RepositoryVersionInfoImpl(new Date(), "test"))
     *
     * folder1 = localProject.addFolder("folder1"); folder2 =
     * localProject.addFolder("folder2"); File folder1File = new
     * File(localProject.getLocation(), folder1.getName());
     * TestHelper.createEmptyFile(folder1File, "file1");
     * TestHelper.createEmptyFile(folder1File, "file2"); folder1.refresh();
     * folder1File1 = (LocalProjectResource) folder1.getArtefact("file1");
     * folder1File2 = (LocalProjectResource) folder1.getArtefact("file2"); }
     *
     * public void testProperties() throws ProjectException, IOException,
     * PropertyException, WorkspaceException { Date now = new Date();
     * folder1.addProperty(new PropertyImpl("p1", "paranoia"));
     * folder1.addProperty(new PropertyImpl("p2", now)); folder2.addProperty(new
     * PropertyImpl("p1", "xyz"));
     *
     * folder1File1.addProperty(new PropertyImpl("p3", "file1"));
     * folder1File1.addProperty(new PropertyImpl("p4", now));
     * folder1File2.addProperty(new PropertyImpl("p5", "file2"));
     * folder1File2.addProperty(new PropertyImpl("p6", now));
     * localProject.save();
     *
     * final String folderPropFolder = FolderHelper.PROPERTIES_FOLDER +
     * File.separator + FolderHelper.FOLDER_PROPERTIES_FOLDER;
     * assertTrue(folderPropFolder + " directory was not created in project root
     * folder", new File(localProject .getLocation(),
     * folderPropFolder).isDirectory()); assertTrue(folderPropFolder + "
     * directory was not created in folder1", new File(
     * ((LocalProjectFolderImpl) folder1).getLocation(),
     * folderPropFolder).isDirectory()); assertTrue(folderPropFolder + "
     * directory was not created in folder2", new File(
     * ((LocalProjectFolderImpl) folder2).getLocation(),
     * folderPropFolder).isDirectory());
     *
     * LocalProject project = getFreshWorkspace().getProject(PROJECT_NAME);
     * ProjectArtefact folder1New = project.getArtefact("folder1");
     * ProjectArtefact folder2New = project.getArtefact("folder2");
     * assertEquals("property not restored", "paranoia",
     * folder1New.getProperty("p1").getValue()); assertEquals("property not
     * restored", now, folder1New.getProperty("p2").getValue());
     * assertEquals("property not restored", "xyz",
     * folder2New.getProperty("p1").getValue()); assertEquals("property not
     * restored", "file1",
     * folder1New.getArtefact("file1").getProperty("p3").getValue());
     * assertEquals("property not restored", now,
     * folder1New.getArtefact("file1").getProperty("p4").getValue());
     * assertEquals("property not restored", "file2",
     * folder1New.getArtefact("file2").getProperty("p5").getValue());
     * assertEquals("property not restored", now,
     * folder1New.getArtefact("file2").getProperty("p6").getValue()); }
     *
     * public void testDoubleSave() throws ProjectException, PropertyException {
     * Date now = new Date(); folder1.addProperty(new PropertyImpl("p1",
     * "paranoia")); folder1.addProperty(new PropertyImpl("p2", now));
     * folder2.addProperty(new PropertyImpl("p1", "xyz"));
     *
     * localProject.save(); localProject.save(); }
     *
     * public void testPropertyFolderIgnored() throws ProjectException,
     * WorkspaceException { localProject.save(); LocalProject project =
     * getFreshWorkspace().getProject(PROJECT_NAME); try {
     * project.getArtefact(FolderHelper.PROPERTIES_FOLDER); fail("Exception not
     * thrown"); } catch (ProjectException e) { // ok } }
     *
     * public void testFixedDateProperties() throws ProjectException,
     * WorkspaceException { Date date1 = new Date(System.currentTimeMillis());
     * Date date2 = new Date(date1.getTime() + 1); Date date3 = new
     * Date(date2.getTime() + 1); Date date4 = new Date(date3.getTime() + 1);
     * Date date5 = new Date(date4.getTime() + 1); Date date6 = new
     * Date(date5.getTime() + 1);
     *
     * localProject.setEffectiveDate(date1);
     * localProject.setExpirationDate(date2); folder1.setEffectiveDate(date3);
     * folder1.setExpirationDate(date4); folder1File2.setEffectiveDate(date5);
     * folder1File2.setExpirationDate(date6); localProject.save();
     *
     * LocalProject project = getFreshWorkspace().getProject(PROJECT_NAME);
     * LocalProjectArtefact folder1 = project.getArtefact("folder1");
     * LocalProjectArtefact folder1File2 = folder1.getArtefact("file2");
     *
     * assertEquals("effective date for project was not persisted corectly",
     * date1, project.getEffectiveDate()); assertEquals("expiration date for
     * project was not persisted corectly", date2, project.getExpirationDate());
     * assertEquals("effective date for folder was not persisted corectly",
     * date3, folder1.getEffectiveDate()); assertEquals("expiration date for
     * folder was not persisted corectly", date4, folder1.getExpirationDate());
     * assertEquals("effective date for file was not persisted corectly", date5,
     * folder1File2.getEffectiveDate()); assertEquals("expiration date for file
     * was not persisted corectly", date6, folder1File2.getExpirationDate()); }
     *
     * public void testProjectDependency() throws WorkspaceException,
     * ProjectException { ProjectDependency[] dependencies = { new
     * ProjectDependencyImpl("project1", new RepositoryProjectVersionImpl(1, 0,
     * 0, null)), new ProjectDependencyImpl("project2", new
     * RepositoryProjectVersionImpl(2, 1, 0, null), new
     * RepositoryProjectVersionImpl(2, 2, 0, null)) };
     *
     * localProject.setDependencies(Arrays.asList(dependencies));
     * localProject.save();
     *
     * LocalProject project = getFreshWorkspace().getProject(PROJECT_NAME); List<ProjectDependency>
     * deps = new ArrayList<ProjectDependency>(project.getDependencies());
     * assertEquals("dependency collection size changed", dependencies.length,
     * deps.size()); for (int i = 0; i < dependencies.length; i++) {
     * assertEquals("dependency is incorrect in position " + i, dependencies[i],
     * deps.get(i)); } }
     *
     * public void testProjectDependencyNotNull() throws ProjectException,
     * WorkspaceException { localProject.save();
     *
     * LocalProject project = getFreshWorkspace().getProject(PROJECT_NAME);
     * assertNotNull("dependencies are null", project.getDependencies());
     * assertEquals("dependencies are not empty", 0,
     * project.getDependencies().size()); }
     *
     *
     * private static LocalWorkspaceImpl getFreshWorkspace() throws
     * WorkspaceException { Properties properties = new Properties();
     * properties.put(LocalWorkspaceManagerImpl.PROP_WS_LOCATION,
     * TestHelper.FOLDER_TEST);
     *
     * LocalWorkspaceManagerImpl workspaceManager = new
     * LocalWorkspaceManagerImpl(new SmartProps(properties));
     * workspaceManager.setLocalWorkspaceFileFilter(new NotFileFilter(new
     * NameFileFilter(".studioProps"))); return
     * workspaceManager.createWorkspace(TestHelper.getWorkspaceUser()); }
     */

}
