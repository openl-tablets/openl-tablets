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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.common.ProjectException;
import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.project.impl.local.ProjectMetainfo;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.BranchStatus;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.BranchedProject;
import org.openl.rules.workspace.dtr.BranchedProject.BranchEntry;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.lw.LocalWorkspace;

/**
 * The workspace refresh keeps an opened copy linked when its repository is temporarily unavailable.
 * When the repository is removed from the configuration or the project is deleted, the copy is
 * silently closed regardless of local changes.
 *
 * @author Yury Molchan
 */
class UserWorkspaceRefreshTest {

    private static final String PROJECT = "P1";
    private static final String SECOND_PROJECT = "P2";
    private static final String DESIGN_PATH = "DESIGN/rules/" + PROJECT;

    @TempDir
    Path userDir;

    private MetainfoRegistry registry;
    private LocalRepository localRepository;
    private LocalWorkspace localWorkspace;
    private DesignTimeRepository designTimeRepository;
    private LockEngine projectsLockEngine;
    private UserWorkspaceImpl userWorkspace;

    @BeforeEach
    void init() {
        registry = MetainfoRegistry.open(userDir);
        localRepository = new LocalRepository(userDir, registry);
        localRepository.setId("design");
        localRepository.initialize();

        designTimeRepository = mock(DesignTimeRepository.class);
        lenient().when(designTimeRepository.getRulesLocation()).thenReturn("DESIGN/rules/");
        projectsLockEngine = mock(LockEngine.class);
        lenient().when(projectsLockEngine.getLockInfo(anyString(), any(), anyString()))
                .thenReturn(LockInfo.NO_LOCK);

        localWorkspace = mock(LocalWorkspace.class);
        lenient().when(localWorkspace.getLocation()).thenReturn(userDir.toFile());
        lenient().when(localWorkspace.getRepository(anyString())).thenReturn(localRepository);
        lenient().when(localWorkspace.getMetainfoRegistry()).thenReturn(registry);
        lenient().when(localWorkspace.getProjects()).thenAnswer(invocation -> workspaceProjects());
        lenient().when(localWorkspace.getProjectForPath(anyString(), any()))
                .thenAnswer(invocation -> workspaceProjects().stream()
                        .filter(project -> DESIGN_PATH.equals(invocation.getArgument(1)))
                        .findFirst()
                        .orElse(null));
        lenient().when(localWorkspace.getProjectForName(anyString(), anyString()))
                .thenAnswer(invocation -> workspaceProjects().stream()
                        .filter(project -> project.getBusinessName().equalsIgnoreCase(invocation.getArgument(1)))
                        .findFirst()
                        .orElse(null));

        userWorkspace = new UserWorkspaceImpl(new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe")),
                localWorkspace,
                designTimeRepository,
                projectsLockEngine);
    }

    @Test
    void unavailableRepositoryKeepsTheLink() throws IOException {
        var designRepository = mockEmptyDesign();
        // The repository cannot answer: an outage or an invalid URL is not a deletion.
        when(designRepository.check(anyString())).thenThrow(new IOException("The repository is unreachable."));
        seedOpenedCopy("design", null, false);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        var project = projects.getFirst();
        assertFalse(project.isLocalOnly(), "The copy must stay linked to the unavailable repository.");
        assertTrue(project.isOpened());
        assertEquals("design", registry.get(PROJECT).repositoryId(), "The record must not be rewritten.");
        assertTrue(Files.exists(userDir.resolve(PROJECT)), "The unchanged copy must not be closed.");
    }

    @Test
    void unchangedCopyOfProjectDeletedUpstreamIsClosed() throws IOException {
        // The repository answers, and the project is gone: a genuine deletion, not an outage.
        mockEmptyDesign();
        seedOpenedCopy("design", null, false);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertTrue(projects.isEmpty(), "The unchanged copy of the deleted project must be closed.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The unchanged copy must be deleted.");
        assertNull(registry.get(PROJECT), "The record must be removed together with the copy.");
    }

    @Test
    void modifiedCopyOfProjectDeletedUpstreamIsClosed() throws IOException {
        // The deletion is equivalent to a revoked access: local changes do not keep the copy alive.
        mockEmptyDesign();
        seedOpenedCopy("design", null, true);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertTrue(projects.isEmpty(), "The modified copy of the deleted project must be closed.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The copied data must not outlive the project.");
        assertNull(registry.get(PROJECT), "The record must be removed together with the copy.");
    }

    @Test
    void modifiedCopyOfUnavailableRepositoryStaysOpened() throws IOException {
        var designRepository = mockEmptyDesign();
        when(designRepository.check(anyString())).thenThrow(new IOException("The repository is unreachable."));
        seedOpenedCopy("design", null, true);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        var project = projects.getFirst();
        assertTrue(project.isOpened(), "The copy must stay opened: an outage is not a deletion.");
        assertTrue(project.isModified());
        assertTrue(Files.exists(userDir.resolve(PROJECT)), "The local changes must not be deleted.");
    }

    @Test
    void unchangedCopyOfArchivedProjectIsClosed() throws IOException {
        var designRepository = mockEmptyDesign();
        // The repository keeps the archived project as a deletion marker instead of a missing path.
        var archived = new FileData();
        archived.setName(DESIGN_PATH);
        archived.setVersion("rev-2");
        archived.setDeleted(true);
        when(designRepository.check(DESIGN_PATH)).thenReturn(archived);
        seedOpenedCopy("design", null, false);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertTrue(projects.isEmpty(), "The unchanged copy of the archived project must be closed.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The unchanged copy must be deleted.");
    }

    @Test
    void unchangedCopyExistingOnlyInSecondaryBranchStaysVisible() throws IOException {
        mockDesignWithProjectInBranch("feature");
        seedOpenedCopy("design", "feature", false);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertEquals("feature", projects.getFirst().getBranch());
        assertTrue(projects.getFirst().isOpened());
        assertTrue(Files.exists(userDir.resolve(PROJECT)));
        assertNotNull(registry.get(PROJECT));
    }

    @Test
    void modifiedCopyExistingOnlyInSecondaryBranchStaysVisible() throws IOException {
        mockDesignWithProjectInBranch("feature");
        seedOpenedCopy("design", "feature", true);
        LockInfo lockInfo = mock(LockInfo.class);
        when(lockInfo.isLocked()).thenReturn(true);
        when(lockInfo.getLockedBy()).thenReturn("jdoe");
        when(projectsLockEngine.getLockInfo("design", "feature", DESIGN_PATH)).thenReturn(lockInfo);
        var history = userDir.resolve(".history").resolve(PROJECT).resolve("module");
        Files.createDirectories(history);
        Files.writeString(history.resolve("edit"), "unsaved");

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertEquals("feature", projects.getFirst().getBranch());
        assertTrue(projects.getFirst().isModified());
        assertTrue(Files.exists(userDir.resolve(PROJECT)));
        assertTrue(Files.exists(userDir.resolve(".history").resolve(PROJECT)));
    }

    @Test
    void lockFailureDoesNotKeepOrphanCopy() throws IOException {
        mockEmptyDesign();
        seedOpenedCopy("design", null, true);
        when(projectsLockEngine.getLockInfo("design", null, DESIGN_PATH))
                .thenThrow(new IllegalStateException("The lock storage is unavailable."));

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertTrue(projects.isEmpty());
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The lock failure must not prevent cleanup.");
        assertNull(registry.get(PROJECT));
    }

    /**
     * The main-branch listing misses the project, but a secondary branch still holds it.
     */
    private void mockDesignWithProjectInBranch(String branch) throws IOException {
        mockBranchedEmptyDesign();
        BranchRepository forBranch = mock(BranchRepository.class);
        lenient().when(forBranch.getId()).thenReturn("design");
        lenient().when(forBranch.getBranch()).thenReturn(branch);
        lenient().when(forBranch.getBaseBranch()).thenReturn("work");
        lenient().when(forBranch.supports())
                .thenReturn(new FeaturesBuilder(forBranch).setVersions(true).setBranches(true).build());
        var existing = new FileData();
        existing.setName(DESIGN_PATH);
        existing.setVersion("rev-1");
        var project = new AProject(forBranch, existing);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of(project));
        when(designTimeRepository.getBranchedProject("design", PROJECT))
                .thenReturn(java.util.Optional.of(branchedProject("work", Map.of(branch, project))));
    }

    @Test
    void unchangedCopyOfRemovedBranchMissingUpstreamIsClosed() throws IOException {
        var branched = mockBranchedEmptyDesign();
        when(branched.branchExists("dead")).thenReturn(false);
        seedOpenedCopy("design", "dead", false);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertTrue(projects.isEmpty(), "The unchanged copy of the removed branch must be closed.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The unchanged copy must be deleted.");
    }

    @Test
    void versionlessLinkedRecordMissingFromMainBranchIsClosed() throws IOException {
        // A record migrated from the legacy layout still has the repository path needed to identify
        // an orphan, even when it has no revision details.
        mockEmptyDesign();
        seedOpenedCopy("design", null, false, null);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertTrue(projects.isEmpty());
        assertNull(registry.get(PROJECT), "The versionless record must be removed.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The versionless copy must be deleted.");
    }

    private Repository mockEmptyDesign() throws IOException {
        Repository designRepository = mock(Repository.class);
        lenient().when(designRepository.getId()).thenReturn("design");
        lenient().when(designRepository.supports()).thenReturn(new FeaturesBuilder(designRepository).build());
        lenient().when(designRepository.check(anyString())).thenReturn(null);
        when(designTimeRepository.getRepository("design")).thenReturn(designRepository);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of());
        return designRepository;
    }

    private BranchRepository mockBranchedEmptyDesign() {
        BranchRepository branched = mock(BranchRepository.class);
        lenient().when(branched.getId()).thenReturn("design");
        lenient().when(branched.supports())
                .thenReturn(new FeaturesBuilder(branched).setVersions(true).setBranches(true).build());
        lenient().when(branched.getBranch()).thenReturn("work");
        when(designTimeRepository.getRepository("design")).thenReturn(branched);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of());
        return branched;
    }

    @Test
    void removedRepositoryEvictsAllLinkedCopies() throws IOException {
        when(designTimeRepository.getRepository("design")).thenReturn(null);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of());
        seedOpenedCopy("design", null, true);
        seedOpenedCopy(SECOND_PROJECT, "design", null, false, "rev-1");
        LockInfo lockInfo = mock(LockInfo.class);
        when(lockInfo.isLocked()).thenReturn(true);
        when(lockInfo.getLockedBy()).thenReturn("jdoe");
        when(projectsLockEngine.getLockInfo("design", null, DESIGN_PATH)).thenReturn(lockInfo);
        var history = userDir.resolve(".history").resolve(PROJECT).resolve("module");
        Files.createDirectories(history);
        Files.writeString(history.resolve("edit"), "unsaved");

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertTrue(projects.isEmpty(), "A removed repository must not leave linked copies in the workspace.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The modified copy must be deleted.");
        assertFalse(Files.exists(userDir.resolve(SECOND_PROJECT)), "The unchanged copy must be deleted.");
        assertFalse(Files.exists(userDir.resolve(".history").resolve(PROJECT)),
                "The local edit history must be deleted with the copy.");
        assertNull(registry.get(PROJECT), "The record must be removed together with the copy.");
        assertNull(registry.get(SECOND_PROJECT), "Every record linked to the repository must be removed.");
        verify(projectsLockEngine).unlock("design", null, DESIGN_PATH);
    }

    @Test
    void versionlessCopyOfRemovedRepositoryIsEvicted() {
        when(designTimeRepository.getRepository("design")).thenReturn(null);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of());
        seedOpenedCopy("design", null, false, null);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertTrue(projects.isEmpty(), "A removed repository must evict versionless linked copies too.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The versionless copy must be deleted.");
        assertNull(registry.get(PROJECT), "The versionless record must be removed.");
    }

    @Test
    void genuineLocalProjectStaysLocal() {
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of());
        seedOpenedCopy("local", null, false);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertTrue(projects.getFirst().isLocalOnly());
        assertEquals("local", registry.get(PROJECT).repositoryId());
    }

    @Test
    void closedBranchPreferenceSurvivesWorkspaceRecreation() throws IOException, ProjectException {
        var main = branchProjectRepository("main");
        var feature = branchProjectRepository("feature/rates");
        var mainData = new FileData();
        mainData.setName(DESIGN_PATH);
        mainData.setVersion("main-revision");
        var featureData = new FileData();
        featureData.setName(DESIGN_PATH);
        featureData.setVersion("feature-revision");
        when(feature.check(DESIGN_PATH)).thenReturn(featureData);
        var mainProject = new AProject(main, mainData);
        var featureProject = new AProject(feature, featureData);
        when(designTimeRepository.getRepository("design")).thenReturn(main);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of(mainProject));
        when(designTimeRepository.getBranchedProject("design", PROJECT))
                .thenReturn(java.util.Optional.of(
                        branchedProject("main", Map.of("main", mainProject, "feature/rates", featureProject))));

        var selected = userWorkspace.getProject("design", PROJECT);
        userWorkspace.setProjectBranch(selected, "feature/rates");
        assertEquals("feature/rates", selected.getBranch());

        var recreated = new UserWorkspaceImpl(
                new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe")),
                localWorkspace,
                designTimeRepository,
                projectsLockEngine);

        assertEquals("feature/rates", recreated.getProject("design", PROJECT).getBranch());
    }

    @Test
    void openedCopyUsesTheSelectedBranchesMappedPath() throws ProjectException {
        var main = mappedBranchProjectRepository("main", "physical/main/P1");
        var feature = mappedBranchProjectRepository("feature/rates", DESIGN_PATH);
        var mainData = new FileData();
        mainData.setName("DESIGN/P1");
        var featureData = new FileData();
        featureData.setName("DESIGN/P1");
        var mainProject = new AProject(main, mainData);
        var featureProject = new AProject(feature, featureData);
        when(designTimeRepository.getRepository("design")).thenReturn(main);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of(mainProject));
        when(designTimeRepository.getBranchedProject("design", PROJECT))
                .thenReturn(java.util.Optional.of(
                        branchedProject("main", Map.of("main", mainProject, "feature/rates", featureProject))));
        seedOpenedCopy("design", "feature/rates", false);

        var project = userWorkspace.getProject("design", PROJECT);

        assertEquals("feature/rates", project.getBranch());
        assertEquals(DESIGN_PATH, project.getRealPath());
    }

    @Test
    void openedCopyIsMatchedByEveryBranchPathBeforeItsLocalFolderName() throws ProjectException {
        var main = mappedBranchProjectRepository("main", "physical/main/P1");
        var feature = mappedBranchProjectRepository("feature/rates", DESIGN_PATH);
        var mainData = new FileData();
        mainData.setName("DESIGN/P1");
        var featureData = new FileData();
        featureData.setName("DESIGN/P1");
        var mainProject = new AProject(main, mainData);
        var featureProject = new AProject(feature, featureData);
        when(designTimeRepository.getRepository("design")).thenReturn(main);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of(mainProject));
        when(designTimeRepository.getBranchedProject("design", PROJECT))
                .thenReturn(java.util.Optional.of(
                        branchedProject("main", Map.of("main", mainProject, "feature/rates", featureProject))));
        when(localWorkspace.getProjectForName("design", PROJECT)).thenReturn(null);
        seedOpenedCopy("design", "feature/rates", false);

        var project = userWorkspace.getProject("design", PROJECT);

        assertEquals("feature/rates", project.getBranch());
        assertTrue(project.isOpened());
    }

    @Test
    void preferenceForMissingBranchIsDiscarded() throws ProjectException {
        var main = branchProjectRepository("main");
        var mainData = new FileData();
        mainData.setName(DESIGN_PATH);
        var mainProject = new AProject(main, mainData);
        when(designTimeRepository.getRepository("design")).thenReturn(main);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of(mainProject));
        when(designTimeRepository.getBranchedProject("design", PROJECT))
                .thenReturn(java.util.Optional.of(branchedProject("main", Map.of("main", mainProject))));
        ProjectBranchPreferenceStore.open(userDir).put("design", PROJECT, "removed");
        var recreated = new UserWorkspaceImpl(
                new WorkspaceUserImpl("jdoe", id -> new UserInfo("jdoe")),
                localWorkspace,
                designTimeRepository,
                projectsLockEngine);

        assertEquals("main", recreated.getProject("design", PROJECT).getBranch());
        assertTrue(ProjectBranchPreferenceStore.open(userDir).get("design", PROJECT).isEmpty());
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

    private void mockMappedDesign() throws IOException {
        Repository designRepository = mock(Repository.class);
        lenient().when(designRepository.getId()).thenReturn("design");
        lenient().when(designRepository.supports())
                .thenReturn(new FeaturesBuilder(designRepository).setMappedFolders(true).build());
        // The project still exists upstream: it was renamed, not deleted.
        var existing = new FileData();
        existing.setName(DESIGN_PATH);
        existing.setVersion("rev-1");
        lenient().when(designRepository.check(anyString())).thenReturn(existing);
        when(designTimeRepository.getRepository("design")).thenReturn(designRepository);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of());
    }

    @Test
    void modifiedCopyOfProjectDeletedInBranchIsClosed() throws IOException {
        // The deletion is equivalent to a revoked access: local changes do not keep the copy alive.
        mockBranchedDesign(true);
        seedOpenedCopy("design", "dead", true);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertFalse(projects.getFirst().isOpened(), "The modified copy must be closed.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The copied data must not outlive the project.");
        assertNull(registry.get(PROJECT), "The record must be removed together with the copy.");
    }

    @Test
    void unchangedCopyOfProjectDeletedInBranchIsClosed() throws IOException {
        mockBranchedDesign(true);
        seedOpenedCopy("design", "dead", false);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertFalse(projects.getFirst().isOpened(), "The unchanged copy must be closed.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The unchanged copy must be deleted.");
    }

    @Test
    void modifiedCopyOfRemovedBranchIsClosed() throws IOException {
        mockBranchedDesign(false);
        seedOpenedCopy("design", "dead", true);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertFalse(projects.getFirst().isOpened(), "The modified copy must be closed.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The copied data must not outlive the project.");
        assertNull(registry.get(PROJECT), "The record must be removed together with the copy.");
    }

    @Test
    void unchangedCopyOfRemovedBranchIsClosed() throws IOException {
        mockBranchedDesign(false);
        seedOpenedCopy("design", "dead", false);

        var projects = new ArrayList<RulesProject>(userWorkspace.getProjects(true));

        assertEquals(1, projects.size());
        assertFalse(projects.getFirst().isOpened(), "The unchanged copy must be closed.");
        assertFalse(Files.exists(userDir.resolve(PROJECT)), "The unchanged copy must be deleted.");
    }

    private void mockBranchedDesign(boolean branchExists) throws IOException {
        BranchRepository branched = branchProjectRepository("master");
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
        var designFileData = new FileData();
        designFileData.setName(DESIGN_PATH);
        designFileData.setVersion("rev-2");
        var project = new AProject(branched, designFileData);
        when(designTimeRepository.getProjects()).thenAnswer(invocation -> List.of(project));
        when(designTimeRepository.getBranchedProject("design", PROJECT))
                .thenReturn(java.util.Optional.of(branchedProject("master", Map.of("master", project))));
    }

    private static BranchRepository branchProjectRepository(String branch) {
        BranchRepository repository = mock(BranchRepository.class);
        lenient().when(repository.getId()).thenReturn("design");
        lenient().when(repository.supports())
                .thenReturn(new FeaturesBuilder(repository).setVersions(true).setBranches(true).build());
        lenient().when(repository.getBranch()).thenReturn(branch);
        lenient().when(repository.getBaseBranch()).thenReturn("main");
        return repository;
    }

    private static BranchRepository mappedBranchProjectRepository(String branch, String internalPath) {
        BranchRepository repository = mock(BranchRepository.class, withSettings().extraInterfaces(FolderMapper.class));
        lenient().when(repository.getId()).thenReturn("design");
        lenient().when(repository.supports()).thenReturn(new FeaturesBuilder(repository)
                .setVersions(true)
                .setBranches(true)
                .setMappedFolders(true)
                .build());
        lenient().when(repository.getBranch()).thenReturn(branch);
        lenient().when(repository.getBaseBranch()).thenReturn("main");
        var mapper = (FolderMapper) repository;
        lenient().when(mapper.getBusinessName("DESIGN/P1")).thenReturn("DESIGN/P1");
        lenient().when(mapper.getRealPath("DESIGN/P1")).thenReturn(internalPath);
        return repository;
    }

    private static BranchedProject branchedProject(String baseBranch, Map<String, AProject> projects) {
        var entries = new java.util.LinkedHashMap<String, BranchEntry>();
        projects.forEach((branch, project) -> entries.put(branch,
                new BranchEntry(project,
                        new BranchStatus(
                                new UserInfo("author"),
                                Instant.parse("2026-07-29T10:00:00Z"),
                                "message",
                                branch + "-revision"))));
        return BranchedProject.create(PROJECT, baseBranch, entries);
    }

    private void seedOpenedCopy(String repositoryId, String branch, boolean modified) {
        seedOpenedCopy(repositoryId, branch, modified, "rev-1");
    }

    private void seedOpenedCopy(String repositoryId, String branch, boolean modified, String version) {
        seedOpenedCopy(PROJECT, repositoryId, branch, modified, version);
    }

    private void seedOpenedCopy(String projectName,
                                String repositoryId,
                                String branch,
                                boolean modified,
                                String version) {
        try {
            var projectDir = userDir.resolve(projectName);
            Files.createDirectories(projectDir);
            var mainFile = projectDir.resolve("Main.xlsx");
            Files.writeString(mainFile, "content");
            // The baseline matches the written file, so an unchanged fixture is genuinely unchanged
            // and survives the dirty-state reconstruction of a restarted workspace.
            var baseline = new ProjectMetainfo.FileBaseline(null, Files.size(mainFile),
                    Files.getLastModifiedTime(mainFile).toMillis());
            // A record without a version has no revision details at all, like a record migrated
            // from the legacy layout.
            registry.save(projectName,
                    new ProjectMetainfo(repositoryId, "DESIGN/rules/" + projectName, branch, version, "jdoe",
                            version == null ? null : 1751980000000L, version == null ? null : 7L,
                            null, Map.of("/Main.xlsx", baseline)));
            if (modified) {
                registry.markDirty(projectName);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<AProject> workspaceProjects() {
        var projects = new ArrayList<AProject>();
        for (String name : registry.projects()) {
            var projectRepository = new LocalRepository(userDir, registry);
            ProjectMetainfo metainfo = Objects.requireNonNull(registry.get(name));
            projectRepository.setId(metainfo.repositoryId());
            projectRepository.initialize();
            var projectState = projectRepository.getProjectState(name);
            var fileData = projectState.getFileData();
            // Mirrors LocalWorkspaceImpl.loadProjects: a record without revision details serves
            // the project from the folder on disk.
            projects.add(fileData == null
                    ? new AProject(projectRepository, name, projectState.getProjectVersion())
                    : new AProject(projectRepository, fileData));
        }
        return projects;
    }
}
