package org.openl.rules.workspace.uw.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.DummyLockEngine;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.project.impl.local.ProjectMetainfo;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;

/**
 * The workspace refresh must never rewrite the repository link of an opened copy: an unavailable
 * repository or a deleted upstream is not a reason to turn the copy into a LOCAL project.
 *
 * @author Yury Molchan
 */
class UserWorkspaceRefreshTest {

    private static final String PROJECT = "P1";
    private static final String DESIGN_PATH = "DESIGN/rules/" + PROJECT;

    @TempDir
    Path userDir;

    private MetainfoRegistry registry;
    private LocalRepository localRepository;
    private DesignTimeRepository designTimeRepository;
    private UserWorkspaceImpl userWorkspace;

    @BeforeEach
    void init() {
        registry = MetainfoRegistry.open(userDir);
        localRepository = new LocalRepository(userDir, registry);
        localRepository.setId("design");
        localRepository.initialize();

        designTimeRepository = mock(DesignTimeRepository.class);
        lenient().when(designTimeRepository.getRulesLocation()).thenReturn("DESIGN/rules/");

        LocalWorkspace localWorkspace = mock(LocalWorkspace.class);
        lenient().when(localWorkspace.getRepository(anyString())).thenReturn(localRepository);
        lenient().when(localWorkspace.getMetainfoRegistry()).thenReturn(registry);
        lenient().when(localWorkspace.getProjects()).thenAnswer(invocation -> workspaceProjects());
        lenient().when(localWorkspace.getProjectForPath(anyString(), any()))
                .thenAnswer(invocation -> workspaceProjects().stream()
                        .filter(project -> DESIGN_PATH.equals(invocation.getArgument(1)))
                        .findFirst()
                        .orElse(null));

        userWorkspace = new UserWorkspaceImpl(new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe")),
                localWorkspace,
                designTimeRepository,
                new DummyLockEngine());
    }

    @Test
    void unavailableRepositoryKeepsTheLink() throws IOException {
        Repository designRepository = mock(Repository.class);
        lenient().when(designRepository.getId()).thenReturn("design");
        lenient().when(designRepository.supports()).thenReturn(new FeaturesBuilder(designRepository).build());
        when(designTimeRepository.getRepository("design")).thenReturn(designRepository);
        // The repository does not serve its projects: an outage or an invalid URL.
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of());
        seedOpenedCopy("design", null, false);

        List<RulesProject> projects = new ArrayList<>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        RulesProject project = projects.getFirst();
        assertFalse(project.isLocalOnly(), "The copy must stay linked to the unavailable repository.");
        assertTrue(project.isOpened());
        assertEquals("design", registry.get(PROJECT).repositoryId(), "The record must not be rewritten.");
    }

    @Test
    void removedRepositoryConvertsTheCopyToLocal() {
        // The repository is not configured anymore: the administrative detach is the only case
        // when the copy becomes a genuine local project.
        when(designTimeRepository.getRepository("design")).thenReturn(null);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of());
        seedOpenedCopy("design", null, true);

        List<RulesProject> projects = new ArrayList<>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertTrue(projects.getFirst().isLocalOnly());
        assertEquals("local", registry.get(PROJECT).repositoryId(),
                "The record must be relinked to the local repository.");
        assertTrue(registry.isDirty(PROJECT), "The local changes must survive the conversion.");
    }

    @Test
    void removedRepositoryRelinksTheVersionlessCopy() {
        // A record migrated from the legacy layout may lack the revision details. The administrative
        // detach must relink it all the same, otherwise the record hangs under the removed repository.
        when(designTimeRepository.getRepository("design")).thenReturn(null);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of());
        seedOpenedCopy("design", null, true, null);

        List<RulesProject> projects = new ArrayList<>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertTrue(projects.getFirst().isLocalOnly());
        assertEquals("local", registry.get(PROJECT).repositoryId(),
                "The record must be relinked to the local repository.");
        assertTrue(registry.isDirty(PROJECT), "The local changes must survive the relink.");
    }

    @Test
    void genuineLocalProjectStaysLocal() {
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of());
        seedOpenedCopy("local", null, false);

        List<RulesProject> projects = new ArrayList<>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertTrue(projects.getFirst().isLocalOnly());
        assertEquals("local", registry.get(PROJECT).repositoryId());
    }

    @Test
    void syncRenamesTheFolderTogetherWithItsRecord() throws IOException {
        mockMappedDesign();
        seedOpenedCopy("design", null, false);
        // The project was renamed on the design side: rules.xml carries the actual name.
        Files.writeString(userDir.resolve(PROJECT).resolve("rules.xml"),
                "<?xml version=\"1.0\"?><project><name>P1-renamed</name></project>");

        userWorkspace.syncProjects();
        userWorkspace.getProjects(true);

        assertNull(registry.get(PROJECT));
        assertNotNull(registry.get("P1-renamed"), "The record must be renamed together with the folder.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)));
        assertTrue(Files.isDirectory(userDir.resolve("P1-renamed")));
    }

    @Test
    void syncRejectsActualNameEscapingTheWorkspace() throws IOException {
        mockMappedDesign();
        seedOpenedCopy("design", null, false);
        // rules.xml is user-editable content, so the actual name cannot be trusted as a folder name.
        Files.writeString(userDir.resolve(PROJECT).resolve("rules.xml"),
                "<?xml version=\"1.0\"?><project><name>../evil</name></project>");

        userWorkspace.syncProjects();
        userWorkspace.getProjects(true);

        assertNotNull(registry.get(PROJECT), "The record must keep its name.");
        assertTrue(Files.isDirectory(userDir.resolve(PROJECT)), "The folder must not move.");
        assertFalse(Files.exists(userDir.getParent().resolve("evil")),
                "Nothing must escape the workspace root.");
    }

    private void mockMappedDesign() {
        Repository designRepository = mock(Repository.class);
        lenient().when(designRepository.getId()).thenReturn("design");
        lenient().when(designRepository.supports())
                .thenReturn(new FeaturesBuilder(designRepository).setMappedFolders(true).build());
        when(designTimeRepository.getRepository("design")).thenReturn(designRepository);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of());
    }

    @Test
    void modifiedCopyOfProjectDeletedInBranchStaysOpened() throws IOException {
        mockBranchedDesign(true);
        seedOpenedCopy("design", "dead", true);

        List<RulesProject> projects = new ArrayList<>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        RulesProject project = projects.getFirst();
        assertFalse(project.isLocalOnly(), "The modified copy must not turn into a LOCAL project.");
        assertTrue(project.isOpened(), "The modified copy must stay opened.");
        assertTrue(project.isModified());
        assertEquals("design", registry.get(PROJECT).repositoryId(), "The record must not be rewritten.");
        assertTrue(Files.exists(userDir.resolve(PROJECT)), "The local changes must not be deleted.");
    }

    @Test
    void unchangedCopyOfProjectDeletedInBranchIsClosed() throws IOException {
        mockBranchedDesign(true);
        seedOpenedCopy("design", "dead", false);

        List<RulesProject> projects = new ArrayList<>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertFalse(projects.getFirst().isOpened(), "The unchanged copy must be closed.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The unchanged copy must be deleted.");
    }

    @Test
    void modifiedCopyOfRemovedBranchStaysOpened() throws IOException {
        mockBranchedDesign(false);
        seedOpenedCopy("design", "dead", true);

        List<RulesProject> projects = new ArrayList<>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        RulesProject project = projects.getFirst();
        assertFalse(project.isLocalOnly(), "The modified copy must not turn into a LOCAL project.");
        assertTrue(project.isOpened(), "The modified copy must stay opened.");
        assertTrue(project.isModified());
        assertEquals("design", registry.get(PROJECT).repositoryId(), "The record must not be rewritten.");
        assertTrue(Files.exists(userDir.resolve(PROJECT)), "The local changes must not be deleted.");
    }

    @Test
    void unchangedCopyOfRemovedBranchIsClosed() throws IOException {
        mockBranchedDesign(false);
        seedOpenedCopy("design", "dead", false);

        List<RulesProject> projects = new ArrayList<>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertFalse(projects.getFirst().isOpened(), "The unchanged copy must be closed.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The unchanged copy must be deleted.");
    }

    private void mockBranchedDesign(boolean branchExists) throws IOException {
        BranchRepository branched = mock(BranchRepository.class);
        lenient().when(branched.getId()).thenReturn("design");
        lenient().when(branched.supports())
                .thenReturn(new FeaturesBuilder(branched).setVersions(true).setBranches(true).build());
        lenient().when(branched.getBranch()).thenReturn("master");
        when(branched.branchExists("dead")).thenReturn(branchExists);

        if (branchExists) {
            BranchRepository dead = mock(BranchRepository.class);
            lenient().when(dead.getId()).thenReturn("design");
            lenient().when(dead.supports())
                    .thenReturn(new FeaturesBuilder(dead).setVersions(true).setBranches(true).build());
            // The project was deleted in this branch.
            when(dead.check(DESIGN_PATH)).thenReturn(null);
            when(branched.forBranch("dead")).thenReturn(dead);
        }

        when(designTimeRepository.getRepository("design")).thenReturn(branched);
        FileData designFileData = new FileData();
        designFileData.setName(DESIGN_PATH);
        designFileData.setVersion("rev-2");
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of(new AProject(branched, designFileData)));
    }

    private void seedOpenedCopy(String repositoryId, String branch, boolean modified) {
        seedOpenedCopy(repositoryId, branch, modified, "rev-1");
    }

    private void seedOpenedCopy(String repositoryId, String branch, boolean modified, String version) {
        try {
            Path projectDir = userDir.resolve(PROJECT);
            Files.createDirectories(projectDir);
            Path mainFile = projectDir.resolve("Main.xlsx");
            Files.writeString(mainFile, "content");
            // The baseline matches the written file, so an unchanged fixture is genuinely unchanged
            // and survives the dirty-state reconstruction of a restarted workspace.
            var baseline = new ProjectMetainfo.FileBaseline(null, Files.size(mainFile),
                    Files.getLastModifiedTime(mainFile).toMillis());
            // A record without a version has no revision details at all, like a record migrated
            // from the legacy layout.
            registry.save(PROJECT,
                    new ProjectMetainfo(repositoryId, DESIGN_PATH, branch, version, "jdoe",
                            version == null ? null : 1751980000000L, version == null ? null : 7L,
                            null, Map.of("/Main.xlsx", baseline)));
            if (modified) {
                registry.markDirty(PROJECT);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<AProject> workspaceProjects() {
        List<AProject> projects = new ArrayList<>();
        for (String name : registry.projects()) {
            FileData fileData = localRepository.getProjectState(name).getFileData();
            // Mirrors LocalWorkspaceImpl.loadProjects: a record without revision details serves
            // the project from the folder on disk.
            projects.add(fileData == null
                    ? new AProject(localRepository, name, localRepository.getProjectState(name).getProjectVersion())
                    : new AProject(localRepository, fileData));
        }
        return projects;
    }
}
