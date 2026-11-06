package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.ProjectUploader;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.ProjectIds;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryProjects;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.service.ProjectAuditor;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.tags.service.TagAssignmentValidator;

class ProjectCreationServiceTest {

    private final List<AuditEvent> events = new ArrayList<>();

    private AclProjectsHelper aclProjectsHelper;
    private RepositoryAclServiceProvider aclServiceProvider;
    private RepositoryAclService folderAcl;
    private TagAssignmentValidator tagAssignmentValidator;
    private ProjectCreationService service;

    @BeforeEach
    void setUp() {
        aclProjectsHelper = mock(AclProjectsHelper.class);
        aclServiceProvider = mock(RepositoryAclServiceProvider.class);
        tagAssignmentValidator = mock(TagAssignmentValidator.class);
        service = new ProjectCreationService(aclProjectsHelper, aclServiceProvider,
                tagAssignmentValidator, mock(PathFilter.class), mock(ZipCharsetDetector.class), "",
                new ProjectAuditor(event -> events.add((AuditEvent) event)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void an_archive_upload_grants_the_creator_role_and_records_the_creation() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("jdoe", "secret"));
        var acl = creatorAcl();
        var project = uploadedProject();

        service.finishArchiveUpload(project, false, ProjectCreationService.FolderAcl.KEPT);

        verify(acl).createAcl(project, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true);
        assertEquals(List.of(AuditAction.PROJECT_CREATED, AuditAction.ROLE_GRANTED),
                events.stream().map(AuditEvent::action).toList());
        assertEquals("archive", events.getFirst().details().get("source"));
        assertEquals("jdoe", events.getLast().subject());
    }

    @Test
    void an_archive_over_a_project_records_a_save_and_keeps_its_access_rights() {
        var acl = creatorAcl();
        var project = uploadedProject();
        when(acl.hasAcl(project)).thenReturn(true);

        service.finishArchiveUpload(project, true, ProjectCreationService.FolderAcl.KEPT);

        verify(acl, never()).createAcl(any(AProject.class), anyList(), anyBoolean());
        assertEquals(List.of(AuditAction.PROJECT_SAVED), events.stream().map(AuditEvent::action).toList());
        assertEquals("archive", events.getFirst().details().get("source"));
    }

    @Test
    void an_archive_in_the_folder_of_a_removed_project_replaces_the_access_it_left() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("jdoe", "secret"));
        var acl = creatorAcl();
        var project = uploadedProject();
        when(acl.hasAcl(project)).thenReturn(true);
        var leftBehind = new ProjectCreationService.FolderAcl(true,
                Map.of(new PrincipalSid("user16938"), List.of(AclRole.MANAGER.getCumulativePermission())));

        service.finishArchiveUpload(project, false, leftBehind);

        verify(acl).createAcl(project, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true);
        // The roles the removed project left are revoked, as a writer of the access list announces what it replaces.
        assertEquals(List.of(AuditAction.PROJECT_CREATED, AuditAction.ROLE_REVOKED, AuditAction.ROLE_GRANTED),
                events.stream().map(AuditEvent::action).toList());
        assertEquals("user16938", events.get(1).subject());
    }

    @Test
    void a_role_that_cannot_be_granted_fails_the_creation() {
        creatorAcl();
        var acl = aclServiceProvider.getDesignRepoAclService();
        var project = uploadedProject();
        when(acl.createAcl(project, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true)).thenReturn(false);
        var replaced = new ProjectCreationService.FolderAcl(true, Map.of());

        var e = assertThrows(ConflictException.class, () -> service.finishArchiveUpload(project, false, replaced));

        assertEquals("openl.error.409.project.create.failed.message", e.getErrorCode());
    }

    @Test
    void the_access_of_a_folder_no_branch_holds_is_replaced_with_the_roles_it_holds_revoked() {
        var repository = folderRepository(new RepositoryProjects(List.of(projectAt("/DESIGN/rules/Other")), true));
        var leftBehind = Map.<Sid, List<Permission>>of(new PrincipalSid("jdoe"), List.of(BasePermission.READ));
        when(folderAcl.listPermissions("design", "DESIGN/rules/AclLeak")).thenReturn(leftBehind);

        assertEquals(new ProjectCreationService.FolderAcl(true, leftBehind),
                service.folderAcl(repository, "DESIGN/rules/AclLeak"));
    }

    @Test
    void the_access_of_a_folder_another_branch_holds_stays() {
        var repository = folderRepository(new RepositoryProjects(List.of(projectAt("/DESIGN/rules/AclLeak")), true));

        assertSame(ProjectCreationService.FolderAcl.KEPT, service.folderAcl(repository, "DESIGN/rules/AclLeak"));
    }

    @Test
    void the_access_of_a_folder_holding_a_project_below_it_stays() {
        var repository = folderRepository(
                new RepositoryProjects(List.of(projectAt("/DESIGN/rules/AclLeak/Inner")), true));

        assertSame(ProjectCreationService.FolderAcl.KEPT, service.folderAcl(repository, "DESIGN/rules/AclLeak"));
    }

    @Test
    void the_access_of_a_project_the_partial_index_knows_stays() {
        // An archive uploaded over a project while a branch fails to be read: the project is known all the same.
        var repository = folderRepository(new RepositoryProjects(List.of(projectAt("/DESIGN/rules/AclLeak")), false));

        assertSame(ProjectCreationService.FolderAcl.KEPT, service.folderAcl(repository, "DESIGN/rules/AclLeak"));
    }

    @Test
    void a_folder_with_access_entries_is_refused_while_the_repository_is_not_indexed_whole() {
        var repository = folderRepository(RepositoryProjects.UNKNOWN);
        when(folderAcl.listPermissions("design", "DESIGN/rules/AclLeak"))
                .thenReturn(Map.of(new PrincipalSid("jdoe"), List.of(BasePermission.READ)));

        var e = assertThrows(ConflictException.class,
                () -> service.folderAcl(repository, "DESIGN/rules/AclLeak"));
        assertEquals("openl.error.409.project.create.indexing.message", e.getErrorCode());
    }

    @Test
    void a_folder_without_access_entries_is_written_while_the_repository_is_not_indexed_whole() {
        var repository = folderRepository(RepositoryProjects.UNKNOWN);

        assertEquals(new ProjectCreationService.FolderAcl(true, Map.of()),
                service.folderAcl(repository, "DESIGN/rules/AclLeak"));
    }

    /** The access lists of the design repositories, granting the role of a creator. */
    private RepositoryAclService creatorAcl() {
        var acl = mock(RepositoryAclService.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);
        when(acl.createAcl(any(AProject.class), anyList(), anyBoolean())).thenReturn(true);
        return acl;
    }

    /**
     * A repository whose index lists the given projects across its branches.
     */
    private Repository folderRepository(RepositoryProjects projects) {
        folderAcl = mock(RepositoryAclService.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(folderAcl);
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.getProjectsInAllBranches("design")).thenReturn(projects);
        var workspace = mock(UserWorkspace.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        service = serviceWithWorkspace(workspace);
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        return repository;
    }

    private static AProject projectAt(String folder) {
        var data = new FileData();
        data.setName(folder);
        var project = mock(AProject.class);
        when(project.getFileData()).thenReturn(data);
        return project;
    }

    private static AProject uploadedProject() {
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        var project = mock(AProject.class);
        when(project.getRepository()).thenReturn(repository);
        when(project.getBusinessName()).thenReturn("Pricing");
        return project;
    }

    @Test
    void lists_predefined_templates_without_error() {
        // The custom resolver is not initialised outside a Spring context; listing must not fail.
        assertNotNull(service.listTemplates());
    }

    @Test
    void create_from_template_is_denied_without_create_permission() {
        assertThrows(ForbiddenException.class, () -> service.createFromTemplate("design", "Project", null,
                "predefined", "examples", "Example", "comment", null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void a_creation_from_files_is_audited_though_the_index_does_not_publish_it() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("jdoe", "secret"));
        when(aclProjectsHelper.hasCreateProjectPermission("design")).thenReturn(true);
        creatorAcl();
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.getRulesLocation()).thenReturn("DESIGN/rules/");
        when(designTimeRepository.getProjectsInAllBranches("design"))
                .thenReturn(new RepositoryProjects(List.of(), true));
        var workspace = mock(UserWorkspace.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        service = serviceWithWorkspace(workspace);
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        var project = mock(RulesProject.class);
        when(project.getRepository()).thenReturn(repository);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getBusinessName()).thenReturn("Pricing");
        List<ProjectFile> files = List.of();

        try (var ignored = mockConstruction(ProjectUploader.class, (uploader, context) ->
                when(uploader.uploadProject()).thenAnswer(invocation -> {
                    ((Consumer<RulesProject>) context.arguments().get(13)).accept(project);
                    throw new ConflictException("project.indexing.incomplete.message");
                }))) {
            assertThrows(ConflictException.class, () -> service.createFromFiles(repository, "Pricing", null, files,
                    "comment", "rules/Models.xlsx", "rules/Algorithms.xlsx", "Models", "Algorithms", Map.of()));
        }

        // The project and its access are made before the wait for the index, so the log tells of them.
        assertEquals(List.of(AuditAction.PROJECT_CREATED, AuditAction.ROLE_GRANTED),
                events.stream().map(AuditEvent::action).toList());
    }

    @Test
    void create_from_files_is_denied_without_create_permission() {
        List<ProjectFile> files = List.of();
        assertThrows(ForbiddenException.class, () -> service.createFromFiles("design", "Project", null,
                files, "comment", "rules/Models.xlsx", "rules/Algorithms.xlsx", "Models", "Algorithms", null));
    }

    @Test
    void copy_project_is_denied_without_create_permission() {
        var targetRepository = mock(Repository.class);
        when(targetRepository.getId()).thenReturn("design");
        var source = mock(RulesProject.class);
        assertThrows(ForbiddenException.class,
                () -> service.copyProject(targetRepository, "Copy", null, source, "comment", null));
    }


    @Test
    void copy_project_rejects_a_revision_the_source_has_no_state_at() throws Exception {
        when(aclProjectsHelper.hasCreateProjectPermission("design")).thenReturn(true);
        var acl = mock(RepositoryAclService.class);
        when(acl.isGranted(any(RulesProject.class), anyList())).thenReturn(true);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);

        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setVersions(true).build());
        when(repository.check("DESIGN/Source")).thenReturn(fileData("9"));
        // A repository numbers revisions its own way and rejects a value it cannot read.
        when(repository.checkHistory("DESIGN/Source", "does-not-exist"))
                .thenThrow(new NumberFormatException("For input string: \"does-not-exist\""));
        var source = mock(RulesProject.class);
        when(source.getRepository()).thenReturn(repository);
        when(source.getFolderPath()).thenReturn("DESIGN/Source");

        var workspace = mock(UserWorkspace.class);
        service = serviceWithWorkspace(workspace);
        var targetRepository = mock(Repository.class);
        when(targetRepository.getId()).thenReturn("design");
        when(targetRepository.supports()).thenReturn(new FeaturesBuilder(targetRepository).build());

        assertThrows(NotFoundException.class, () -> service.copyProject(targetRepository, "Copy", null, source,
                "comment", "does-not-exist"));
    }

    @Test
    void copy_project_reads_the_source_at_the_requested_revision() throws Exception {
        when(aclProjectsHelper.hasCreateProjectPermission("design")).thenReturn(true);
        var acl = mock(RepositoryAclService.class);
        when(acl.isGranted(any(RulesProject.class), anyList())).thenReturn(true);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);

        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setVersions(true).build());
        when(repository.check("DESIGN/Source")).thenReturn(fileData("9"));
        when(repository.checkHistory("DESIGN/Source", "5")).thenReturn(fileData("5"));
        var source = mock(RulesProject.class);
        when(source.getRepository()).thenReturn(repository);
        when(source.getFolderPath()).thenReturn("DESIGN/Source");

        var workspace = mock(UserWorkspace.class);
        service = serviceWithWorkspace(workspace);
        var targetRepository = mock(Repository.class);
        when(targetRepository.getId()).thenReturn("design");
        when(targetRepository.supports()).thenReturn(new FeaturesBuilder(targetRepository).build());

        // The target repository is not configured here, so the copy fails right after the source is read.
        assertThrows(RuntimeException.class, () -> service.copyProject(targetRepository, "Copy", null, source,
                "comment", "5"));

        verify(repository).checkHistory("DESIGN/Source", "5");
    }

    @Test
    void apply_status_opens_a_created_project_when_open_is_requested() throws Exception {
        var project = mock(RulesProject.class);
        when(project.isOpened()).thenReturn(false);
        var workspace = mock(UserWorkspace.class);
        when(workspace.getProject("design", "Alpha")).thenReturn(project);
        when(workspace.isOpenedOtherProject(project)).thenReturn(false);
        service = serviceWithWorkspace(workspace);

        // OPENED arrives as VIEWING through the status converter.
        service.applyStatusAfterCreate("design", "Alpha", ProjectStatus.VIEWING);

        verify(project).open();
        verify(workspace).refresh();
    }

    @Test
    void apply_status_opens_an_archive_project_the_workspace_does_not_list_yet() throws Exception {
        // An uploaded archive lands straight in the design repository: the workspace lookup fails, and
        // the project is assembled from its design state instead — like the legacy creator builds it.
        var workspace = mock(UserWorkspace.class);
        when(workspace.getProject("design", "Alpha"))
                .thenThrow(new ProjectException("Cannot find project 'Alpha'."));
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        var designProject = mock(AProject.class);
        when(designTimeRepository.getProject("design", "Alpha")).thenReturn(designProject);

        var project = mock(RulesProject.class);
        when(project.isOpened()).thenReturn(false);
        var testService = new TestProjectCreationService(aclProjectsHelper, aclServiceProvider,
                mock(TagAssignmentValidator.class), mock(PathFilter.class), mock(ZipCharsetDetector.class), "",
                new ProjectAuditor(event -> events.add((AuditEvent) event)), workspace);
        testService.designWorkspaceProject = project;

        testService.applyStatusAfterCreate("design", "Alpha", ProjectStatus.VIEWING);

        // The stale design-project cache is invalidated before the design lookup.
        verify(designTimeRepository).refresh();
        verify(project).open();
        verify(workspace).refresh();
    }

    @Test
    void apply_status_resolves_the_created_project_in_its_target_branch() throws Exception {
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getBranch()).thenReturn("feature");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        var workspace = mock(UserWorkspace.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        var designProject = mock(AProject.class);
        when(designTimeRepository.getProject("design", "Alpha", "feature")).thenReturn(designProject);
        var project = mock(RulesProject.class);
        var testService = new TestProjectCreationService(aclProjectsHelper, aclServiceProvider,
                mock(TagAssignmentValidator.class), mock(PathFilter.class), mock(ZipCharsetDetector.class), "",
                new ProjectAuditor(event -> events.add((AuditEvent) event)), workspace);
        testService.designWorkspaceProject = project;

        testService.applyStatusAfterCreate(repository, "Alpha", ProjectStatus.VIEWING);

        verify(designTimeRepository).getProject("design", "Alpha", "feature");
        verify(workspace, never()).getProject("design", "Alpha");
        verify(project).open();
    }

    @Test
    void apply_status_resolves_a_mapped_project_by_its_business_name() throws Exception {
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getBranch()).thenReturn("feature");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        var workspace = mock(UserWorkspace.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        when(designTimeRepository.getProject("design", "Alpha", "feature"))
                .thenThrow(new ProjectException("Not found"));
        var indexedProject = mock(AProject.class);
        when(indexedProject.getBusinessName()).thenReturn("Alpha");
        when(indexedProject.getName()).thenReturn("Alpha:hash");
        doReturn(List.of(indexedProject)).when(designTimeRepository).getProjects("design");
        var designProject = mock(AProject.class);
        when(designTimeRepository.getProject("design", "Alpha:hash", "feature")).thenReturn(designProject);
        var project = mock(RulesProject.class);
        var testService = new TestProjectCreationService(aclProjectsHelper, aclServiceProvider,
                mock(TagAssignmentValidator.class), mock(PathFilter.class), mock(ZipCharsetDetector.class), "",
                new ProjectAuditor(event -> events.add((AuditEvent) event)), workspace);
        testService.designWorkspaceProject = project;

        testService.applyStatusAfterCreate(repository, "Alpha", ProjectStatus.VIEWING);

        verify(designTimeRepository).getProject("design", "Alpha:hash", "feature");
        verify(project).open();
    }

    @Test
    void apply_status_closes_a_created_project_when_close_is_requested() throws Exception {
        var project = mock(RulesProject.class);
        when(project.isOpened()).thenReturn(true);
        var workspace = mock(UserWorkspace.class);
        when(workspace.getProject("design", "Alpha")).thenReturn(project);
        service = serviceWithWorkspace(workspace);

        service.applyStatusAfterCreate("design", "Alpha", ProjectStatus.CLOSED);

        verify(project).close();
        verify(workspace).refresh();
    }

    @Test
    void apply_status_leaves_an_already_open_project_alone() throws Exception {
        var project = mock(RulesProject.class);
        when(project.isOpened()).thenReturn(true);
        var workspace = mock(UserWorkspace.class);
        when(workspace.getProject("design", "Alpha")).thenReturn(project);
        service = serviceWithWorkspace(workspace);

        service.applyStatusAfterCreate("design", "Alpha", ProjectStatus.VIEWING);

        verify(project, never()).open();
        verify(workspace, never()).refresh();
    }

    @Test
    void apply_status_skips_opening_while_another_copy_of_the_project_is_open() throws Exception {
        var project = mock(RulesProject.class);
        when(project.isOpened()).thenReturn(false);
        var workspace = mock(UserWorkspace.class);
        when(workspace.getProject("design", "Alpha")).thenReturn(project);
        when(workspace.isOpenedOtherProject(project)).thenReturn(true);
        service = serviceWithWorkspace(workspace);

        service.applyStatusAfterCreate("design", "Alpha", ProjectStatus.VIEWING);

        verify(project, never()).open();
    }

    @Test
    void apply_status_never_fails_the_create_when_opening_fails() throws Exception {
        var project = mock(RulesProject.class);
        when(project.isOpened()).thenReturn(false);
        var workspace = mock(UserWorkspace.class);
        when(workspace.getProject("design", "Alpha")).thenReturn(project);
        when(workspace.isOpenedOtherProject(project)).thenReturn(false);
        doThrow(new ProjectException("disk full")).when(project).open();
        service = serviceWithWorkspace(workspace);

        // The project was created; a failed open is logged, never turned into a create error.
        assertDoesNotThrow(() -> service.applyStatusAfterCreate("design", "Alpha", ProjectStatus.VIEWING));
    }

    @Test
    void assembles_the_workspace_view_of_a_design_project_from_its_design_state() {
        var user = mock(WorkspaceUser.class);
        var localWorkspace = mock(LocalWorkspace.class);
        var localRepository = mock(LocalRepository.class);
        var lockEngine = mock(LockEngine.class);
        var workspace = mock(UserWorkspace.class);
        when(workspace.getUser()).thenReturn(user);
        when(workspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(localWorkspace.getRepository("design")).thenReturn(localRepository);
        when(workspace.getProjectsLockEngine()).thenReturn(lockEngine);

        var designRepository = mock(Repository.class);
        var designData = fileData("3");
        designData.setName("DESIGN/Alpha");
        var designProject = mock(AProject.class);
        when(designProject.getRepository()).thenReturn(designRepository);
        when(designProject.getFileData()).thenReturn(designData);

        var assembled = serviceWithWorkspace(workspace).newWorkspaceProject(workspace, "design", designProject);

        // The view reads from the design state — like the legacy creator's freshly created project.
        assertSame(designRepository, assembled.getDesignRepository());
        assertSame(designData, assembled.getFileData());
    }

    @Test
    void apply_status_leaves_the_project_alone_when_no_status_is_requested() {
        var workspace = mock(UserWorkspace.class);
        service = serviceWithWorkspace(workspace);

        service.applyStatusAfterCreate("design", "Alpha", null);

        verifyNoInteractions(workspace);
    }

    @Test
    void refreshes_the_workspace_after_a_design_change() {
        var workspace = mock(UserWorkspace.class);
        service = serviceWithWorkspace(workspace);

        service.refreshWorkspaceAfterDesignChange();

        verify(workspace).refresh();
    }

    @Test
    void workspace_refresh_failure_does_not_fail_a_finalized_design_change() {
        var workspace = mock(UserWorkspace.class);
        doThrow(new IllegalStateException("stale branch selection")).when(workspace).refresh();
        service = serviceWithWorkspace(workspace);

        assertDoesNotThrow(service::refreshWorkspaceAfterDesignChange);
    }

    @Test
    void waits_until_a_branch_write_is_published() {
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getBranch()).thenReturn("feature");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.refreshBranch("design", "feature"))
                .thenReturn(CompletableFuture.completedFuture(null));
        var workspace = mock(UserWorkspace.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);

        serviceWithWorkspace(workspace).awaitProjectVisibility(repository);

        verify(designTimeRepository).refreshBranch("design", "feature");
    }

    @Test
    void reports_a_failed_branch_publication() {
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getBranch()).thenReturn("feature");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.refreshBranch("design", "feature"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("failed")));
        var workspace = mock(UserWorkspace.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);

        service = serviceWithWorkspace(workspace);
        assertThrows(ConflictException.class,
                () -> service.awaitProjectVisibility(repository));
    }

    @Test
    void the_id_of_a_project_written_to_a_repository_with_folders_is_the_id_of_its_folder() {
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        var written = new FileData();
        written.setName("DESIGN/rules/Rating");
        written.addAdditionalData(new FileMappingData("DESIGN/rules/Rating", "Products/Rating"));

        assertEquals(ProjectIds.of("design", "Products/Rating"), service.idOfWritten(repository, written));
    }

    @Test
    void the_id_of_a_project_written_to_a_flat_repository_is_the_id_of_the_key_it_is_stored_under() {
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.getRulesLocation()).thenReturn("DESIGN/rules/");
        var workspace = mock(UserWorkspace.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        service = serviceWithWorkspace(workspace);
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        var written = new FileData();
        written.setName("DESIGN/rules/Rating");

        assertEquals(ProjectIds.of("design", "DESIGN/rules/Rating"), service.idOfWritten(repository, written));
    }

    private ProjectCreationService serviceWithWorkspace(UserWorkspace workspace) {
        return new TestProjectCreationService(aclProjectsHelper, aclServiceProvider,
                tagAssignmentValidator, mock(PathFilter.class), mock(ZipCharsetDetector.class), "",
                new ProjectAuditor(event -> events.add((AuditEvent) event)),
                workspace);
    }

    private static FileData fileData(String version) {
        var data = new FileData();
        data.setName("DESIGN/Source");
        data.setVersion(version);
        return data;
    }

    private static class TestProjectCreationService extends ProjectCreationService {

        private final UserWorkspace workspace;
        /** When set, stands in for the workspace view assembled from a design project. */
        private RulesProject designWorkspaceProject;

        TestProjectCreationService(AclProjectsHelper aclProjectsHelper,
                                   RepositoryAclServiceProvider aclServiceProvider,
                                   TagAssignmentValidator tagAssignmentValidator,
                                   PathFilter zipFilter,
                                   ZipCharsetDetector zipCharsetDetector,
                                   String openlHome,
                                   ProjectAuditor auditor,
                                   UserWorkspace workspace) {
            super(aclProjectsHelper, aclServiceProvider, tagAssignmentValidator, zipFilter, zipCharsetDetector,
                    openlHome, auditor);
            this.workspace = workspace;
        }

        @Override
        public UserWorkspace getUserWorkspace() {
            return workspace;
        }

        @Override
        protected RulesProject newWorkspaceProject(UserWorkspace workspace, String repositoryId, AProject designProject) {
            return designWorkspaceProject != null
                    ? designWorkspaceProject
                    : super.newWorkspaceProject(workspace, repositoryId, designProject);
        }
    }
}
