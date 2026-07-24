package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
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
    private ProjectCreationService service;

    @BeforeEach
    void setUp() {
        aclProjectsHelper = mock(AclProjectsHelper.class);
        aclServiceProvider = mock(RepositoryAclServiceProvider.class);
        comments = mock(Comments.class);
        service = new ProjectCreationService(aclProjectsHelper, aclServiceProvider,
                mock(TagAssignmentValidator.class), mock(PathFilter.class), mock(ZipCharsetDetector.class), "");
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

        var first = publishedProject("First");
        when(workspace.uploadLocalProject("design", "First", "target/", "Create First")).thenReturn(first);
        when(workspace.uploadLocalProject("design", "Second", "target/", "Create Second"))
                .thenThrow(new ProjectException("failed"));

        service = serviceWithWorkspace(workspace);

        var names = List.of("First", "Second");
        assertThrows(ConflictException.class,
                () -> service.uploadLocalProjects("design", names, "target/", null));

        verify(first).delete(user, "Rollback project upload.");
        verify(acl).deleteAcl(first);
        verify(workspace).refresh();
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
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setVersions(true).build());
        when(repository.check("DESIGN/Source")).thenReturn(fileData("9"));
        // A repository numbers revisions its own way and rejects a value it cannot read.
        when(repository.checkHistory("DESIGN/Source", "does-not-exist"))
                .thenThrow(new NumberFormatException("For input string: \"does-not-exist\""));
        var source = mock(RulesProject.class);
        when(source.getRepository()).thenReturn(repository);
        when(source.getFolderPath()).thenReturn("DESIGN/Source");

        var workspace = mock(UserWorkspace.class);
        when(workspace.getProject("design", "Source", true)).thenReturn(source);
        service = serviceWithWorkspace(workspace);

        assertThrows(NotFoundException.class, () -> service.copyProject("design", "Copy", null, "design",
                "Source", "comment", "does-not-exist"));
    }

    @Test
    void copy_project_reads_the_source_at_the_requested_revision() throws Exception {
        when(aclProjectsHelper.hasCreateProjectPermission("design")).thenReturn(true);
        var acl = mock(RepositoryAclService.class);
        when(acl.isGranted(any(RulesProject.class), anyList())).thenReturn(true);
        when(aclServiceProvider.getDesignRepoAclService()).thenReturn(acl);

        var repository = mock(Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setVersions(true).build());
        when(repository.check("DESIGN/Source")).thenReturn(fileData("9"));
        when(repository.checkHistory("DESIGN/Source", "5")).thenReturn(fileData("5"));
        var source = mock(RulesProject.class);
        when(source.getRepository()).thenReturn(repository);
        when(source.getFolderPath()).thenReturn("DESIGN/Source");

        var workspace = mock(UserWorkspace.class);
        when(workspace.getProject("design", "Source", true)).thenReturn(source);
        service = serviceWithWorkspace(workspace);

        // The target repository is not configured here, so the copy fails right after the source is read.
        assertThrows(RuntimeException.class, () -> service.copyProject("design", "Copy", null, "design",
                "Source", "comment", "5"));

        verify(repository).checkHistory("DESIGN/Source", "5");
    }

    @Test
    void refreshes_the_workspace_after_a_design_change() {
        var workspace = mock(UserWorkspace.class);
        service = serviceWithWorkspace(workspace);

        service.refreshWorkspaceAfterDesignChange();

        verify(workspace).refresh();
    }

    private ProjectCreationService serviceWithWorkspace(UserWorkspace workspace) {
        return new TestProjectCreationService(aclProjectsHelper, aclServiceProvider,
                mock(TagAssignmentValidator.class), mock(PathFilter.class), mock(ZipCharsetDetector.class), "",
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
    }
}
