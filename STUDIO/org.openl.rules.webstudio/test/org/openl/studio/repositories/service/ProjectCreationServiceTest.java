package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
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
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.tags.service.TagAssignmentValidator;

class ProjectCreationServiceTest {

    private AclProjectsHelper aclProjectsHelper;
    private RepositoryAclServiceProvider aclServiceProvider;
    private Comments comments;
    private TagAssignmentValidator tagAssignmentValidator;
    private ProjectCreationService service;

    @BeforeEach
    void setUp() {
        aclProjectsHelper = mock(AclProjectsHelper.class);
        aclServiceProvider = mock(RepositoryAclServiceProvider.class);
        comments = mock(Comments.class);
        tagAssignmentValidator = mock(TagAssignmentValidator.class);
        service = new ProjectCreationService(aclProjectsHelper, aclServiceProvider,
                tagAssignmentValidator, mock(PathFilter.class), mock(ZipCharsetDetector.class), "");
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
    void create_from_files_is_denied_without_create_permission() {
        List<ProjectFile> files = List.of();
        assertThrows(ForbiddenException.class, () -> service.createFromFiles("design", "Project", null,
                files, "comment", "rules/Models.xlsx", "rules/Algorithms.xlsx", "Models", "Algorithms", null));
    }

    @Test
    void upload_local_projects_is_denied_without_create_permission() {
        var names = List.of("Local");
        assertThrows(ForbiddenException.class,
                () -> service.uploadLocalProjects("design", names, null, "comment"));
    }

    @Test
    void upload_local_projects_rolls_back_previous_publishes_when_batch_fails() throws ProjectException {
        when(aclProjectsHelper.hasCreateProjectPermission("design")).thenReturn(true);
        var workspace = mock(UserWorkspace.class);
        var user = mock(WorkspaceUser.class);
        when(workspace.getUser()).thenReturn(user);
        var acl = mock(RepositoryAclService.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);
        when(comments.createProject("First")).thenReturn("Create First");
        when(comments.createProject("Second")).thenReturn("Create Second");
        var targetRepository = mock(Repository.class);
        when(targetRepository.getId()).thenReturn("design");
        when(targetRepository.supports()).thenReturn(new FeaturesBuilder(targetRepository).build());

        var first = publishedProject("First");
        when(workspace.uploadLocalProject(targetRepository, "First", "target/", "Create First")).thenReturn(first);
        when(workspace.uploadLocalProject(targetRepository, "Second", "target/", "Create Second"))
                .thenThrow(new ProjectException("failed"));

        service = serviceWithWorkspace(workspace);

        var names = List.of("First", "Second");
        assertThrows(ConflictException.class,
                () -> service.uploadLocalProjects(targetRepository, names, "target/", null));

        verify(first).delete(user, "Rollback project upload.");
        verify(acl).deleteAcl(first);
        verify(workspace, never()).refresh();
    }

    @Test
    void upload_local_projects_does_not_roll_back_a_finalized_write_when_indexing_fails() throws Exception {
        when(aclProjectsHelper.hasCreateProjectPermission("design")).thenReturn(true);
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getBranch()).thenReturn("feature");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.refreshBranch("design", "feature"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("failed")));
        var workspace = mock(UserWorkspace.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        var user = mock(WorkspaceUser.class);
        when(workspace.getUser()).thenReturn(user);
        var acl = mock(RepositoryAclService.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);
        var project = publishedProject("First");
        when(workspace.uploadLocalProject(repository, "First", "", "comment")).thenReturn(project);

        assertThrows(ConflictException.class,
                () -> serviceWithWorkspace(workspace).uploadLocalProjects(repository, List.of("First"), null, "comment"));

        verify(acl).createAcl(project, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true);
        verify(tagAssignmentValidator).applicable(Map.of());
        verify(project, never()).delete(any(), any());
        verify(acl, never()).deleteAcl(project);
        verify(workspace, never()).refresh();
    }

    @Test
    void copy_project_is_denied_without_create_permission() {
        assertThrows(ForbiddenException.class,
                () -> service.copyProject("design", "Copy", null, "design", "Source", "comment", null));
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
        when(source.getDesignRepository()).thenReturn(repository);

        var workspace = mock(UserWorkspace.class);
        when(workspace.getProjectsByName("Source")).thenReturn(List.of(source));
        service = serviceWithWorkspace(workspace);
        var targetRepository = mock(Repository.class);
        when(targetRepository.getId()).thenReturn("design");
        when(targetRepository.supports()).thenReturn(new FeaturesBuilder(targetRepository).build());

        assertThrows(NotFoundException.class, () -> service.copyProject(targetRepository, "Copy", null, "design",
                "Source", "comment", "does-not-exist"));
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
        when(source.getDesignRepository()).thenReturn(repository);

        var workspace = mock(UserWorkspace.class);
        when(workspace.getProjectsByName("Source")).thenReturn(List.of(source));
        service = serviceWithWorkspace(workspace);
        var targetRepository = mock(Repository.class);
        when(targetRepository.getId()).thenReturn("design");
        when(targetRepository.supports()).thenReturn(new FeaturesBuilder(targetRepository).build());

        // The target repository is not configured here, so the copy fails right after the source is read.
        assertThrows(RuntimeException.class, () -> service.copyProject(targetRepository, "Copy", null, "design",
                "Source", "comment", "5"));

        verify(repository).checkHistory("DESIGN/Source", "5");
    }

    @Test
    void copy_project_resolves_a_mapped_source_by_business_name() throws Exception {
        when(aclProjectsHelper.hasCreateProjectPermission("target")).thenReturn(true);
        var acl = mock(RepositoryAclService.class);
        when(acl.isGranted(any(RulesProject.class), anyList())).thenReturn(true);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);

        var sourceRepository = mock(Repository.class);
        when(sourceRepository.getId()).thenReturn("source");
        when(sourceRepository.supports()).thenReturn(new FeaturesBuilder(sourceRepository).setVersions(true).build());
        var latestSourceData = fileData("9");
        latestSourceData.setName("DESIGN/Source:hash");
        when(sourceRepository.check("DESIGN/Source:hash")).thenReturn(latestSourceData);
        var sourceData = fileData("5");
        sourceData.setName("DESIGN/Source:hash");
        when(sourceRepository.checkHistory("DESIGN/Source:hash", "5")).thenReturn(sourceData);
        var source = mock(RulesProject.class);
        when(source.getRepository()).thenReturn(sourceRepository);
        when(source.getDesignRepository()).thenReturn(sourceRepository);
        when(source.getFolderPath()).thenReturn("DESIGN/Source:hash");

        var workspace = mock(UserWorkspace.class);
        when(workspace.getProjectsByName("Source")).thenReturn(List.of(source));
        service = serviceWithWorkspace(workspace);
        var targetRepository = mock(Repository.class);
        when(targetRepository.getId()).thenReturn("target");
        when(targetRepository.supports()).thenReturn(new FeaturesBuilder(targetRepository).build());

        // The target repository is not configured here, so the copy fails after resolving and reading the source.
        assertThrows(RuntimeException.class, () -> service.copyProject(targetRepository, "Copy", null, "source",
                "Source", "comment", "5"));

        verify(sourceRepository).checkHistory("DESIGN/Source:hash", "5");
    }

    @Test
    void copy_project_rejects_an_ambiguous_business_name_in_the_source_repository() {
        when(aclProjectsHelper.hasCreateProjectPermission("target")).thenReturn(true);
        var acl = mock(RepositoryAclService.class);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);
        var sourceRepository = mock(Repository.class);
        when(sourceRepository.getId()).thenReturn("source");
        var first = mock(RulesProject.class);
        var second = mock(RulesProject.class);
        when(first.getDesignRepository()).thenReturn(sourceRepository);
        when(second.getDesignRepository()).thenReturn(sourceRepository);
        var workspace = mock(UserWorkspace.class);
        when(workspace.getProjectsByName("Source")).thenReturn(List.of(first, second));
        service = serviceWithWorkspace(workspace);
        var targetRepository = mock(Repository.class);
        when(targetRepository.getId()).thenReturn("target");

        assertThrows(ConflictException.class, () -> service.copyProject(targetRepository, "Copy", null, "source",
                "Source", "comment", null));

        verifyNoInteractions(acl);
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
                workspace, comments);
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
                workspace, comments);
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
                workspace, comments);
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

        assertThrows(ConflictException.class,
                () -> serviceWithWorkspace(workspace).awaitProjectVisibility(repository));
    }

    private ProjectCreationService serviceWithWorkspace(UserWorkspace workspace) {
        return new TestProjectCreationService(aclProjectsHelper, aclServiceProvider,
                tagAssignmentValidator, mock(PathFilter.class), mock(ZipCharsetDetector.class), "",
                workspace, comments);
    }

    private static FileData fileData(String version) {
        var data = new FileData();
        data.setName("DESIGN/Source");
        data.setVersion(version);
        return data;
    }

    private static RulesProject publishedProject(String name) {
        var project = mock(RulesProject.class);
        when(project.getName()).thenReturn(name);
        when(project.getDesignTags()).thenReturn(Map.of());
        when(project.getLocalTags()).thenReturn(Map.of());
        return project;
    }

    private static class TestProjectCreationService extends ProjectCreationService {

        private final UserWorkspace workspace;
        private final Comments comments;
        /** When set, stands in for the workspace view assembled from a design project. */
        private RulesProject designWorkspaceProject;

        TestProjectCreationService(AclProjectsHelper aclProjectsHelper,
                                   RepositoryAclServiceProvider aclServiceProvider,
                                   TagAssignmentValidator tagAssignmentValidator,
                                   PathFilter zipFilter,
                                   ZipCharsetDetector zipCharsetDetector,
                                   String openlHome,
                                   UserWorkspace workspace,
                                   Comments comments) {
            super(aclProjectsHelper, aclServiceProvider, tagAssignmentValidator, zipFilter, zipCharsetDetector, openlHome);
            this.workspace = workspace;
            this.comments = comments;
        }

        @Override
        public UserWorkspace getUserWorkspace() {
            return workspace;
        }

        @Override
        protected Comments getCommentsService(String repoId) {
            return comments;
        }

        @Override
        protected RulesProject newWorkspaceProject(UserWorkspace workspace, String repositoryId, AProject designProject) {
            return designWorkspaceProject != null
                    ? designWorkspaceProject
                    : super.newWorkspaceProject(workspace, repositoryId, designProject);
        }
    }
}
