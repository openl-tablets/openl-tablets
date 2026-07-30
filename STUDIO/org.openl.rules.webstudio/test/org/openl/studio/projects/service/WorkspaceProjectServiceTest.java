package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectException;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.SearchScope;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.BranchedProject;
import org.openl.rules.workspace.dtr.BranchedProjectIndexService.IndexHealth;
import org.openl.rules.workspace.dtr.BranchedProjectIndexService.IndexState;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.CreateBranchModel;
import org.openl.studio.projects.model.ModuleViewModel;
import org.openl.studio.projects.model.ProjectBranchInfo;
import org.openl.studio.projects.model.ProjectInclude;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.EditableTableView;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.service.history.ProjectHistoryService;
import org.openl.studio.projects.service.project.compile.ProjectHandle;
import org.openl.studio.projects.service.project.status.ProjectStatusMapper;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.projects.service.tables.TableCreatorService;
import org.openl.studio.projects.service.tables.read.RawTableReader;
import org.openl.studio.projects.service.tables.read.SummaryTableReader;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;
import org.openl.studio.projects.validator.NewBranchValidator;
import org.openl.studio.projects.validator.ProjectStateValidator;

class WorkspaceProjectServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void get_branches_marks_the_base_and_protected_ones_without_reading_commits() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var project = mock(RulesProject.class);
        var repository = mock(BranchRepository.class);
        var workspace = workspaceFor(project);

        when(project.isSupportsBranches()).thenReturn(true);
        when(project.getDesignRepository()).thenReturn(repository);
        when(repository.getId()).thenReturn("design");
        mockMembership(workspace, project, "main", "feature");
        var service = newService(acl, mock(ProtectedBranchBypassService.class), workspace);
        when(acl.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
        when(repository.getBaseBranch()).thenReturn("main");
        when(repository.isBranchProtected("main")).thenReturn(true);

        var result = service.getBranches(project);

        assertEquals(List.of("feature", "main"), result.stream().map(ProjectBranchInfo::name).toList());
        assertEquals(List.of(false, true), result.stream().map(ProjectBranchInfo::base).toList());
        assertEquals(List.of(false, true), result.stream().map(ProjectBranchInfo::protectedFlag).toList());
        verify(repository, never()).listBranches();
        verify(repository, never()).getBranchStatuses(any());
    }

    @Test
    void get_project_omits_modules_by_default() throws Exception {
        var project = projectWithSimpleExcelModule();
        var service = newService(mock(RepositoryAclService.class), mock(ProtectedBranchBypassService.class),
                workspaceFor(project));

        var result = service.getProject(project);

        // The descriptor is resolved only on request, so it is absent by default.
        assertNull(result.descriptor);
    }

    @Test
    void branch_filter_matches_every_actual_project_branch() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var project = projectWithSimpleExcelModule();
        when(project.isSupportsBranches()).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
        var workspace = workspaceFor(project);
        mockMembership(workspace, project, "main", "feature/rates");
        var service = newService(acl, mock(ProtectedBranchBypassService.class), workspace);

        var matching = service.getProjects(
                ProjectCriteriaQuery.builder().branch("rates").build(),
                Pageable.unpaged());
        var missing = service.getProjects(
                ProjectCriteriaQuery.builder().branch("other").build(),
                Pageable.unpaged());

        assertEquals(1, matching.getNumberOfElements());
        assertEquals(0, missing.getNumberOfElements());
    }

    @Test
    void get_project_reports_the_repository_it_is_stored_in() throws Exception {
        var project = projectWithSimpleExcelModule();
        var service = newService(mock(RepositoryAclService.class), mock(ProtectedBranchBypassService.class),
                workspaceFor(project));

        var result = service.getProject(project);

        // The legacy field keeps carrying the id alone.
        assertEquals("design", result.repository);
        assertNotNull(result.repositoryInfo);
        assertEquals("design", result.repositoryInfo.id());
        assertEquals("Design", result.repositoryInfo.name());
        assertNotNull(result.repositoryInfo.features());
    }

    @Test
    void get_project_reports_no_repository_for_a_local_only_project() throws Exception {
        var project = projectWithSimpleExcelModule();
        when(project.isLocalOnly()).thenReturn(true);
        var service = newService(mock(RepositoryAclService.class), mock(ProtectedBranchBypassService.class),
                workspaceFor(project));

        var result = service.getProject(project);

        assertNull(result.repositoryInfo);
    }

    @Test
    void create_branch_waits_until_its_project_membership_is_published() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        var workspace = workspaceFor();
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        var service = newService(acl, mock(ProtectedBranchBypassService.class), workspace);
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.supports())
                .thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        var project = mock(RulesProject.class);
        var artefact = mock(AProjectArtefact.class);
        fillProject(project, repository, "PricingProject", "PricingProject");
        when(project.isSupportsBranches()).thenReturn(true);
        when(project.getArtefacts()).thenReturn(List.of(artefact));
        when(acl.isGranted(
                artefact,
                List.of(BasePermission.WRITE, BasePermission.DELETE, BasePermission.CREATE))).thenReturn(true);
        when(designTimeRepository.refreshBranch("design", "feature/rates"))
                .thenReturn(CompletableFuture.completedFuture(null));
        var model = new CreateBranchModel();
        model.setBranch("feature/rates");
        model.setRevision("main");

        service.createBranch(project, model);

        verify(repository).createRepositoryBranch("feature/rates", "main");
        verify(designTimeRepository).refreshBranch("design", "feature/rates");
    }

    @Test
    void get_project_resolves_modules_from_workspace_folder_when_requested() throws Exception {
        var project = projectWithSimpleExcelModule();
        var service = newService(mock(RepositoryAclService.class), mock(ProtectedBranchBypassService.class),
                workspaceFor(project));

        var result = service.getProject(project, List.of(ProjectInclude.DESCRIPTOR));

        assertEquals(List.of("Pricing"), result.descriptor.modules().stream().map(ModuleViewModel::name).toList());
        assertEquals(List.of("Pricing.xlsx"), result.descriptor.modules().stream().map(ModuleViewModel::path).toList());
    }

    @Test
    void get_projects_omits_modules_by_default() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var project = projectWithSimpleExcelModule();
        when(acl.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
        var service = newService(acl, mock(ProtectedBranchBypassService.class), workspaceFor(project));

        var response = service.getProjects(ProjectCriteriaQuery.builder().build(), Pageable.unpaged());

        var result = response.getContent().iterator().next();
        assertNull(result.descriptor);
    }

    @Test
    void get_projects_reports_cross_branch_index_health() throws Exception {
        var workspace = workspaceFor();
        var repository = repository();
        var health = new IndexHealth(IndexState.INDEXING, Set.of(), null);
        when(workspace.getDesignTimeRepository().getRepositories()).thenReturn(List.of(repository));
        when(workspace.getDesignTimeRepository().getProjectIndexHealth("design")).thenReturn(Optional.of(health));
        var service = newService(
                mock(RepositoryAclService.class),
                mock(ProtectedBranchBypassService.class),
                workspace);

        var response = service.getProjects(ProjectCriteriaQuery.builder().build(), Pageable.unpaged());

        assertEquals(Map.of("design", health), response.getProjectIndexHealth());
    }

    @Test
    void get_projects_omits_ready_cross_branch_indexes() throws Exception {
        var workspace = workspaceFor();
        var repository = repository();
        var health = new IndexHealth(IndexState.READY, Set.of(), null);
        when(workspace.getDesignTimeRepository().getRepositories()).thenReturn(List.of(repository));
        when(workspace.getDesignTimeRepository().getProjectIndexHealth("design")).thenReturn(Optional.of(health));
        var service = newService(
                mock(RepositoryAclService.class),
                mock(ProtectedBranchBypassService.class),
                workspace);

        var response = service.getProjects(ProjectCriteriaQuery.builder().build(), Pageable.unpaged());

        assertEquals(Map.of(), response.getProjectIndexHealth());
    }

    @Test
    void get_projects_resolves_modules_for_list_response_when_requested() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var project = projectWithSimpleExcelModule();
        when(acl.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
        var service = newService(acl, mock(ProtectedBranchBypassService.class), workspaceFor(project));

        var response = service.getProjects(ProjectCriteriaQuery.builder()
                .include(ProjectInclude.DESCRIPTOR)
                .build(), Pageable.unpaged());

        var result = response.getContent().iterator().next();
        assertEquals(List.of("Pricing"), result.descriptor.modules().stream().map(ModuleViewModel::name).toList());
        assertEquals(List.of("Pricing.xlsx"), result.descriptor.modules().stream().map(ModuleViewModel::path).toList());
    }

    @Test
    void get_project_answers_a_declared_pattern_with_the_modules_it_matched() throws Exception {
        var project = closedProject("""
                <project>
                    <modules>
                        <module>
                            <name>Rules</name>
                            <rules-root path="rules/*.xlsx"/>
                        </module>
                    </modules>
                </project>
                """, "rules/Rating.xlsx");
        var service = newService(mock(RepositoryAclService.class), mock(ProtectedBranchBypassService.class));

        var result = service.getProject(project, List.of(ProjectInclude.DESCRIPTOR));

        // One declaration in rules.xml, one module in the answer, whatever the pattern matched.
        assertEquals(List.of("Rules"), result.descriptor.modules().stream().map(ModuleViewModel::name).toList());
        assertEquals(List.of("rules/*.xlsx"), result.descriptor.modules().stream().map(ModuleViewModel::path).toList());
        var matched = result.descriptor.modules().getFirst().modules();
        assertEquals(List.of("Rating"), matched.stream().map(ModuleViewModel::name).toList());
        assertEquals(List.of("rules/Rating.xlsx"), matched.stream().map(ModuleViewModel::path).toList());
        // The modules and sources come from the file, so neither is flagged as a default.
        assertFalse(result.descriptor.modulesDefault());
        assertTrue(result.descriptor.sourcesDefault());
    }

    @Test
    void get_project_uses_default_module_patterns_when_workspace_folder_is_unavailable() throws Exception {
        var project = closedProject("""
                <project>
                    <name>DefaultModulesProject</name>
                </project>
                """, "rules/Pricing.xlsx", "tests/PricingTest.xlsx");
        var service = newService(mock(RepositoryAclService.class), mock(ProtectedBranchBypassService.class));

        var result = service.getProject(project, List.of(ProjectInclude.DESCRIPTOR));

        // A project that declares no module takes the two patterns every project has by default.
        assertEquals(List.of("rules/**/*.xlsx", "tests/**/*.xlsx"),
                result.descriptor.modules().stream().map(ModuleViewModel::path).toList());
        assertEquals(List.of("Pricing"),
                result.descriptor.modules().getFirst().modules().stream().map(ModuleViewModel::name).toList());
        assertEquals(List.of("PricingTest"),
                result.descriptor.modules().get(1).modules().stream().map(ModuleViewModel::name).toList());
        // The file declares neither modules nor sources, so both are the engine's defaults.
        assertTrue(result.descriptor.modulesDefault());
        assertEquals(List.of("groovy/", "lib/*.jar"), result.descriptor.sources());
        assertTrue(result.descriptor.sourcesDefault());
    }

    @Test
    void get_project_answers_a_pattern_only_with_the_files_it_matches() throws Exception {
        var project = closedProject("""
                <project>
                    <modules>
                        <module>
                            <rules-root path="*.xlsx"/>
                        </module>
                    </modules>
                </project>
                """, "Pricing.xlsx", "rules/Rating.xlsx");
        var service = newService(mock(RepositoryAclService.class), mock(ProtectedBranchBypassService.class));

        var result = service.getProject(project, List.of(ProjectInclude.DESCRIPTOR));

        assertEquals(List.of("*.xlsx"), result.descriptor.modules().stream().map(ModuleViewModel::path).toList());
        // The file in the folder below is out of the pattern's reach.
        assertEquals(List.of("Pricing.xlsx"),
                result.descriptor.modules().getFirst().modules().stream().map(ModuleViewModel::path).toList());
    }

    @Test
    void update_project_status_saves_modified_project_with_default_comment_when_save_requested_without_comment() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var webStudio = mock(WebStudio.class);
        var service = newService(acl, mock(ProtectedBranchBypassService.class), null, projectStateValidator, webStudio);
        var project = mock(RulesProject.class);
        var fileData = mock(FileData.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(project.isModified()).thenReturn(true);
        when(project.getFileData()).thenReturn(fileData);
        when(projectStateValidator.canSave(project)).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.WRITE))).thenReturn(true);

        service.updateProjectStatus(project, ProjectStatusUpdateModel.builder().save(true).build());

        verify(fileData).setComment("Save PricingProject");
        verify(webStudio).saveProject(project);
    }

    @Test
    void update_project_status_tells_the_websocket_layer_after_a_successful_save() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var webStudio = mock(WebStudio.class);
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var userWorkspace = mock(UserWorkspace.class);
        var user = mock(WorkspaceUser.class);
        when(userWorkspace.getUser()).thenReturn(user);
        when(user.getUserName()).thenReturn("jane");
        var service = newService(acl, mock(ProtectedBranchBypassService.class), userWorkspace, projectStateValidator,
                webStudio, mock(AclProjectsHelper.class), eventPublisher);
        var project = mock(RulesProject.class);
        var fileData = mock(FileData.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(project.isModified()).thenReturn(true);
        when(project.getFileData()).thenReturn(fileData);
        when(projectStateValidator.canSave(project)).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.WRITE))).thenReturn(true);

        service.updateProjectStatus(project, ProjectStatusUpdateModel.builder().save(true).build());

        var captor = forClass(ProjectStateChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("jane", captor.getValue().userName());
    }

    @Test
    void a_failing_notification_never_fails_the_action_it_reports() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var webStudio = mock(WebStudio.class);
        var eventPublisher = mock(ApplicationEventPublisher.class);
        doThrow(new IllegalStateException("broker down")).when(eventPublisher).publishEvent(any());
        var userWorkspace = mock(UserWorkspace.class);
        var user = mock(WorkspaceUser.class);
        when(userWorkspace.getUser()).thenReturn(user);
        when(user.getUserName()).thenReturn("jane");
        var service = newService(acl, mock(ProtectedBranchBypassService.class), userWorkspace, projectStateValidator,
                webStudio, mock(AclProjectsHelper.class), eventPublisher);
        var project = mock(RulesProject.class);
        var fileData = mock(FileData.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(project.isModified()).thenReturn(true);
        when(project.getFileData()).thenReturn(fileData);
        when(projectStateValidator.canSave(project)).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.WRITE))).thenReturn(true);

        // The notification is advisory: the save must complete even when publishing it blows up.
        service.updateProjectStatus(project, ProjectStatusUpdateModel.builder().save(true).build());

        verify(webStudio).saveProject(project);
    }

    @Test
    void update_project_status_validates_generated_default_comment() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var webStudio = mock(WebStudio.class);
        var service = newService(acl, mock(ProtectedBranchBypassService.class), null, projectStateValidator, webStudio);
        var project = mock(RulesProject.class);
        var fileData = mock(FileData.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(project.isModified()).thenReturn(true);
        when(projectStateValidator.canSave(project)).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.WRITE))).thenReturn(true);
        when(project.getFileData()).thenReturn(fileData);
        Props.setEnvironment(commentValidationEnvironment());

        var model = ProjectStatusUpdateModel.builder().save(true).build();
        var exception = assertThrows(BadRequestException.class, () -> service.updateProjectStatus(project, model));

        assertEquals("openl.error.400.repo.invalid.comment.message", exception.getErrorCode());
        verify(webStudio, never()).saveProject(project);
    }

    @Test
    void delete_project_does_not_generate_comment_when_missing() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var aclProjectsHelper = mock(AclProjectsHelper.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var userWorkspace = userWorkspaceWithNonDirectoryParent();
        var webStudio = mock(WebStudio.class);
        when(webStudio.getModel()).thenReturn(mock(ProjectModel.class));
        var service = newService(acl, mock(ProtectedBranchBypassService.class), userWorkspace, projectStateValidator,
                webStudio, aclProjectsHelper);
        var project = mock(RulesProject.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(aclProjectsHelper.hasPermission(project, BasePermission.DELETE)).thenReturn(true);
        when(projectStateValidator.canDelete(project)).thenReturn(true);

        service.delete(project, " ");

        verify(project).delete(null);
    }

    @Test
    void delete_project_uses_original_repository_after_project_permission_check() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var aclProjectsHelper = mock(AclProjectsHelper.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var userWorkspace = userWorkspaceWithNonDirectoryParent();
        var user = mock(WorkspaceUser.class);
        when(userWorkspace.getUser()).thenReturn(user);
        var webStudio = mock(WebStudio.class);
        when(webStudio.getModel()).thenReturn(mock(ProjectModel.class));
        var service = newService(acl, mock(ProtectedBranchBypassService.class), userWorkspace, projectStateValidator,
                webStudio, aclProjectsHelper);
        var originalRepository = repository();
        var decoratedRepository =
                mock(Repository.class, withSettings().extraInterfaces(RepositoryDelegate.class));
        when(((RepositoryDelegate) decoratedRepository).getOriginal()).thenReturn(originalRepository);
        when(decoratedRepository.getId()).thenReturn("design");
        when(decoratedRepository.getName()).thenReturn("Design");
        when(decoratedRepository.supports())
                .thenReturn(new FeaturesBuilder(decoratedRepository).build());
        var project = mock(RulesProject.class);
        var fileData = new FileData();
        fileData.setName("PricingProject");
        fillProject(project, decoratedRepository, "PricingProject", "PricingProject");
        when(project.getFileData()).thenReturn(fileData);
        when(acl.getPath(project)).thenReturn("PricingProject");
        when(aclProjectsHelper.hasPermission(project, BasePermission.DELETE)).thenReturn(true);
        when(projectStateValidator.canDelete(project)).thenReturn(true);

        service.delete(project, "Delete PricingProject");

        var deletedData = forClass(FileData.class);
        verify(originalRepository).delete(deletedData.capture());
        assertEquals("PricingProject", deletedData.getValue().getName());
        assertEquals("Delete PricingProject", deletedData.getValue().getComment());
        verify(decoratedRepository, never()).delete(any(FileData.class));
        verify(project, never()).delete(any());
    }

    @Test
    void delete_project_rejects_invalid_comment() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var aclProjectsHelper = mock(AclProjectsHelper.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var userWorkspace = userWorkspaceWithNonDirectoryParent();
        var webStudio = mock(WebStudio.class);
        var service = newService(acl, mock(ProtectedBranchBypassService.class), userWorkspace, projectStateValidator,
                webStudio, aclProjectsHelper);
        var project = mock(RulesProject.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(aclProjectsHelper.hasPermission(project, BasePermission.DELETE)).thenReturn(true);
        when(projectStateValidator.canDelete(project)).thenReturn(true);
        Props.setEnvironment(commentValidationEnvironment());

        var exception = assertThrows(BadRequestException.class, () -> service.delete(project, null));

        assertEquals("openl.error.400.repo.invalid.comment.message", exception.getErrorCode());
        verify(project, never()).delete(any());
    }

    @ParameterizedTest
    @CsvSource({
            "PricingProject, true",
            "PricingProject/module, true",
            "OtherProject, false"
    })
    void delete_project_cleans_acl_only_when_no_surviving_branch_uses_covered_path(String survivingAclPath,
                                                                                  boolean keepAcl) throws Exception {
        var acl = mock(RepositoryAclService.class);
        var aclProjectsHelper = mock(AclProjectsHelper.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var workspaceManager = mock(MultiUserWorkspaceManager.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        var userWorkspace = userWorkspaceWithNonDirectoryParent();
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        var webStudio = mock(WebStudio.class);
        when(webStudio.getModel()).thenReturn(mock(ProjectModel.class));
        var service = newDeleteService(
                acl,
                userWorkspace,
                projectStateValidator,
                webStudio,
                aclProjectsHelper,
                workspaceManager);
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getBranch()).thenReturn("feature/rates");
        when(repository.supports())
                .thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        var project = mock(RulesProject.class);
        fillProject(project, repository, "PricingProject", "PricingProject");
        when(project.getDesignProjectName()).thenReturn("PricingProject:8f31");
        when(project.getBranch()).thenReturn("feature/rates");
        when(aclProjectsHelper.hasPermission(project, BasePermission.DELETE)).thenReturn(true);
        when(projectStateValidator.canDelete(project)).thenReturn(true);
        when(designTimeRepository.refreshBranch("design", "feature/rates"))
                .thenReturn(CompletableFuture.completedFuture(null));
        var surviving = mock(BranchedProject.class);
        var survivingProject = mock(org.openl.rules.project.abstraction.AProject.class);
        var survivingEntry = mock(BranchedProject.BranchEntry.class);
        when(survivingEntry.project()).thenReturn(survivingProject);
        when(surviving.entries()).thenReturn(Map.of("main", survivingEntry));
        when(designTimeRepository.getBranchedProject("design", "PricingProject:8f31"))
                .thenReturn(Optional.of(surviving));
        when(acl.getPath(project)).thenReturn("PricingProject");
        when(acl.getPath(survivingProject)).thenReturn(survivingAclPath);

        service.delete(project, null);

        verify(designTimeRepository).refreshBranch("design", "feature/rates");
        verify(designTimeRepository).getBranchedProject("design", "PricingProject:8f31");
        if (keepAcl) {
            verify(acl, never()).deleteAcl("design", "PricingProject");
        } else {
            verify(acl).deleteAcl("design", "PricingProject");
        }
        verify(workspaceManager).refreshWorkspaces();
    }

    @Test
    void delete_project_removes_acl_after_the_last_branch_entry_disappears() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var aclProjectsHelper = mock(AclProjectsHelper.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        var userWorkspace = userWorkspaceWithNonDirectoryParent();
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        var webStudio = mock(WebStudio.class);
        when(webStudio.getModel()).thenReturn(mock(ProjectModel.class));
        var service = newDeleteService(
                acl,
                userWorkspace,
                projectStateValidator,
                webStudio,
                aclProjectsHelper,
                mock(MultiUserWorkspaceManager.class));
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getBranch()).thenReturn("feature/rates");
        when(repository.supports())
                .thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        var project = mock(RulesProject.class);
        fillProject(project, repository, "PricingProject", "PricingProject");
        when(project.getBranch()).thenReturn("feature/rates");
        when(aclProjectsHelper.hasPermission(project, BasePermission.DELETE)).thenReturn(true);
        when(projectStateValidator.canDelete(project)).thenReturn(true);
        when(designTimeRepository.refreshBranch("design", "feature/rates"))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(designTimeRepository.getBranchedProject("design", "PricingProject")).thenReturn(Optional.empty());
        when(acl.getPath(project)).thenReturn("PricingProject");

        service.delete(project, null);

        verify(acl).deleteAcl("design", "PricingProject");
    }

    @Test
    void delete_project_keeps_acl_and_reports_an_incomplete_failed_branch_refresh() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var aclProjectsHelper = mock(AclProjectsHelper.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var workspaceManager = mock(MultiUserWorkspaceManager.class);
        var designTimeRepository = mock(DesignTimeRepository.class);
        var userWorkspace = userWorkspaceWithNonDirectoryParent();
        when(userWorkspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        var webStudio = mock(WebStudio.class);
        when(webStudio.getModel()).thenReturn(mock(ProjectModel.class));
        var service = newDeleteService(
                acl,
                userWorkspace,
                projectStateValidator,
                webStudio,
                aclProjectsHelper,
                workspaceManager);
        var repository = mock(BranchRepository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getBranch()).thenReturn("feature/rates");
        when(repository.supports())
                .thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        var project = mock(RulesProject.class);
        fillProject(project, repository, "PricingProject", "PricingProject");
        when(project.getBranch()).thenReturn("feature/rates");
        when(aclProjectsHelper.hasPermission(project, BasePermission.DELETE)).thenReturn(true);
        when(projectStateValidator.canDelete(project)).thenReturn(true);
        when(designTimeRepository.refreshBranch("design", "feature/rates"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("index unavailable")));
        when(acl.getPath(project)).thenReturn("PricingProject");

        var exception = assertThrows(ConflictException.class, () -> service.delete(project, null));

        assertEquals("openl.error.409.project.delete.indexing.incomplete.message", exception.getErrorCode());
        verify(acl, never()).deleteAcl("design", "PricingProject");
        verify(workspaceManager).refreshWorkspaces();
    }

    @Test
    void update_project_status_rejects_closing_modified_project_without_force() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var service = newService(acl, mock(ProtectedBranchBypassService.class), null, projectStateValidator,
                mock(WebStudio.class));
        var project = mock(RulesProject.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(project.getStatus()).thenReturn(ProjectStatus.EDITING);
        when(project.isModified()).thenReturn(true);
        when(projectStateValidator.canClose(project)).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);

        var model = ProjectStatusUpdateModel.builder().status(ProjectStatus.CLOSED).build();
        var exception = assertThrows(ConflictException.class, () -> service.updateProjectStatus(project, model));

        assertEquals("openl.error.409.project.close.modified.message", exception.getErrorCode());
        verify(project, never()).close();
    }

    @Test
    void update_project_status_preserves_current_branch_without_request_branch() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var userWorkspace = mock(UserWorkspace.class);
        var webStudio = mock(WebStudio.class);
        var projectModel = mock(ProjectModel.class);
        var service = newService(acl, mock(ProtectedBranchBypassService.class), userWorkspace, projectStateValidator,
                webStudio);
        var project = mock(RulesProject.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(project.getStatus()).thenReturn(ProjectStatus.VIEWING);
        when(project.isOpened()).thenReturn(true);
        when(project.isSupportsBranches()).thenReturn(true);
        when(project.getBranch()).thenReturn("release", "master");
        when(project.getLastHistoryVersion()).thenReturn("revision");
        when(projectStateValidator.canClose(project)).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
        when(webStudio.getModel()).thenReturn(projectModel);

        try (var ignored = mockStatic(ProjectHistoryService.class)) {
            service.updateProjectStatus(project, ProjectStatusUpdateModel.builder()
                    .status(ProjectStatus.CLOSED)
                    .build());
        }

        verify(userWorkspace).setProjectBranch(project, "release");
        verify(project).close();
    }

    @Test
    void update_project_status_keeps_open_project_open_after_branch_switch() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var userWorkspace = mock(UserWorkspace.class);
        var webStudio = mock(WebStudio.class);
        var projectModel = mock(ProjectModel.class);
        var service = newService(acl, mock(ProtectedBranchBypassService.class), userWorkspace,
                mock(ProjectStateValidator.class), webStudio);
        var project = mock(RulesProject.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(project.isSupportsBranches()).thenReturn(true);
        when(project.getBranch()).thenReturn("main");
        when(project.isOpened()).thenReturn(true);
        when(project.getLastHistoryVersion()).thenReturn("revision");
        when(acl.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
        when(webStudio.getModel()).thenReturn(projectModel);
        when(userWorkspace.isOpenedOtherProject(project)).thenReturn(false);

        try (var ignored = mockStatic(ProjectHistoryService.class)) {
            service.updateProjectStatus(project, ProjectStatusUpdateModel.builder()
                    .branch("feature")
                    .build());
        }

        verify(project).releaseMyLock();
        verify(userWorkspace).setProjectBranch(project, "feature");
        verify(project).open();
    }

    @Test
    void update_project_status_rejects_opening_revision_for_modified_project_without_force() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var userWorkspace = mock(UserWorkspace.class);
        var service = newService(acl, mock(ProtectedBranchBypassService.class), userWorkspace, projectStateValidator,
                mock(WebStudio.class));
        var project = mock(RulesProject.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(project.isOpened()).thenReturn(true);
        when(project.isModified()).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
        when(userWorkspace.isOpenedOtherProject(any())).thenReturn(false);

        var model = ProjectStatusUpdateModel.builder()
                .status(ProjectStatus.VIEWING)
                .revision("rev-1")
                .build();
        var exception = assertThrows(ConflictException.class, () -> service.updateProjectStatus(project, model));

        assertEquals("openl.error.409.project.close.modified.message", exception.getErrorCode());
        verify(projectStateValidator, never()).canOpen(project);
        verify(project, never()).openVersion(any());
    }

    @Test
    void update_project_status_forces_opening_revision_for_modified_project() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var projectStateValidator = mock(ProjectStateValidator.class);
        var userWorkspace = mock(UserWorkspace.class);
        var webStudio = mock(WebStudio.class);
        when(webStudio.getModel()).thenReturn(mock(ProjectModel.class));
        var service = newService(acl, mock(ProtectedBranchBypassService.class), userWorkspace, projectStateValidator,
                webStudio);
        var project = mock(RulesProject.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(project.isOpened()).thenReturn(true);
        when(project.isModified()).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
        when(userWorkspace.isOpenedOtherProject(any())).thenReturn(false);

        service.updateProjectStatus(project, ProjectStatusUpdateModel.builder()
                .status(ProjectStatus.VIEWING)
                .revision("rev-1")
                .discardChanges(true)
                .build());

        verify(projectStateValidator, never()).canOpen(project);
        verify(project).releaseMyLock();
        verify(project).openVersion("rev-1");
    }

    @Test
    void update_project_status_rejects_switching_branch_for_modified_project_without_force() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var service = newService(acl, mock(ProtectedBranchBypassService.class));
        var project = mock(RulesProject.class);
        fillProject(project, repository(), "PricingProject", "PricingProject");
        when(project.isSupportsBranches()).thenReturn(true);
        when(project.getBranch()).thenReturn("main");
        when(project.isOpened()).thenReturn(true);
        when(project.isModified()).thenReturn(true);

        var model = ProjectStatusUpdateModel.builder().branch("feature").build();
        var exception = assertThrows(ConflictException.class, () -> service.updateProjectStatus(project, model));

        assertEquals("openl.error.409.project.close.modified.message", exception.getErrorCode());
        verify(project, never()).setBranch("feature");
    }

    @Test
    void create_new_table_creates_a_module_when_module_path_is_supplied() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var webStudio = mock(WebStudio.class);
        var tableCreatorService = mock(TableCreatorService.class);
        var descriptor = new ProjectDescriptor();
        descriptor.setName("PricingProject");
        var project = project(repository(), "PricingProject", "PricingProject");
        when(project.isOpened()).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.WRITE))).thenReturn(true);
        when(webStudio.getProjectByName("design", "PricingProject")).thenReturn(descriptor);
        when(webStudio.getCurrentProject()).thenReturn(project);
        var service = newService(
                acl,
                mock(ProtectedBranchBypassService.class),
                null,
                mock(ProjectStateValidator.class),
                webStudio,
                mock(AclProjectsHelper.class),
                tableCreatorService);
        var table = rawTable("NewTable");
        var request = new CreateNewTableRequest(
                "NewModule",
                "Rules",
                "rules/NewModule.xlsx",
                table);

        service.createNewTable(project, request);

        verify(project).tryLockOrThrow();
        verify(tableCreatorService).requireTableName("NewTable");
        verify(tableCreatorService).createModuleWithTable(project, descriptor, request, table);
    }

    @Test
    void create_new_table_locks_the_target_project_when_the_session_has_none_open() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var webStudio = mock(WebStudio.class);
        var tableCreatorService = mock(TableCreatorService.class);
        var descriptor = new ProjectDescriptor();
        descriptor.setName("PricingProject");
        var project = project(repository(), "PricingProject", "PricingProject");
        when(project.isOpened()).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.WRITE))).thenReturn(true);
        when(webStudio.getProjectByName("design", "PricingProject")).thenReturn(descriptor);
        // A project with no modules never opens, so the session keeps no current project.
        when(webStudio.getCurrentProject()).thenReturn(null);
        var service = newService(
                acl,
                mock(ProtectedBranchBypassService.class),
                null,
                mock(ProjectStateValidator.class),
                webStudio,
                mock(AclProjectsHelper.class),
                tableCreatorService);
        var table = rawTable("NewTable");
        var request = new CreateNewTableRequest(
                "NewModule",
                "Rules",
                "rules/NewModule.xlsx",
                table);

        service.createNewTable(project, request);

        verify(project).tryLockOrThrow();
        verify(tableCreatorService).createModuleWithTable(project, descriptor, request, table);
    }

    @Test
    void create_new_table_rejects_a_non_raw_new_module_request_as_bad_request() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var project = project(repository(), "PricingProject", "PricingProject");
        when(acl.isGranted(project, List.of(BasePermission.WRITE))).thenReturn(true);
        var tableCreatorService = mock(TableCreatorService.class);
        var service = newService(
                acl,
                mock(ProtectedBranchBypassService.class),
                null,
                mock(ProjectStateValidator.class),
                mock(WebStudio.class),
                mock(AclProjectsHelper.class),
                tableCreatorService);
        var request = new CreateNewTableRequest(
                "NewModule",
                "Rules",
                "rules/NewModule.xlsx",
                mock(EditableTableView.class));

        var exception = assertThrows(BadRequestException.class, () -> service.createNewTable(project, request));

        assertEquals("openl.error.400.table.new-module.raw-source.message", exception.getErrorCode());
        verify(project, never()).tryLockOrThrow();
        verify(tableCreatorService, never()).createModuleWithTable(any(), any(), any(), any());
    }

    @Test
    void tables_of_a_project_without_modules_are_none_rather_than_not_found() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var webStudio = mock(WebStudio.class);
        var descriptor = new ProjectDescriptor();
        descriptor.setName("PricingProject");
        var project = project(repository(), "PricingProject", "PricingProject");
        when(project.isOpened()).thenReturn(true);
        when(webStudio.getProjectByName("design", "PricingProject")).thenReturn(descriptor);
        var service = newService(
                acl,
                mock(ProtectedBranchBypassService.class),
                null,
                mock(ProjectStateValidator.class),
                webStudio,
                mock(AclProjectsHelper.class),
                mock(TableCreatorService.class));

        // Such a project is where the table creator gets its first module from, so it has to be able to ask.
        var tables = service.getTables(project, ProjectTableCriteriaQuery.builder().build(), Pageable.unpaged());

        assertTrue(tables.getContent().isEmpty());
        assertEquals(0L, tables.getTotal());
    }

    @Test
    void created_table_is_read_by_the_identifier_returned_by_the_writer() throws Exception {
        var summaryTableReader = mock(SummaryTableReader.class);
        var service = spy(newService(
                mock(RepositoryAclService.class),
                mock(ProtectedBranchBypassService.class),
                null,
                mock(ProjectStateValidator.class),
                mock(WebStudio.class),
                mock(AclProjectsHelper.class),
                mock(TableCreatorService.class),
                summaryTableReader));
        var project = mock(RulesProject.class);
        var projectModel = mock(ProjectModel.class);
        var handle = mock(ProjectHandle.class);
        var openLTable = mock(IOpenLTable.class);
        var expected = SummaryTableView.builder()
                .id("created-id")
                .tableType("RawSource")
                .kind("Constants")
                .name("Constants")
                .build();
        when(handle.awaitCompiled()).thenReturn(projectModel);
        when(projectModel.getTableById("created-id")).thenReturn(openLTable);
        when(summaryTableReader.read(openLTable)).thenReturn(expected);
        doReturn(handle).when(service).openProject(project, "Main");

        var created = service.getCreatedTable(project, "Main", "created-id", "Constants");

        assertEquals(expected, created);
        verify(projectModel).getTableById("created-id");
        verify(summaryTableReader).read(openLTable);
    }

    @Test
    void create_new_table_rejects_a_case_insensitive_duplicate_module_name() throws Exception {
        var acl = mock(RepositoryAclService.class);
        var webStudio = mock(WebStudio.class);
        var tableCreatorService = mock(TableCreatorService.class);
        var descriptor = new ProjectDescriptor();
        descriptor.setName("PricingProject");
        var module = new Module();
        module.setName("Pricing");
        module.setRulesRootPath("rules/Pricing.xlsx");
        descriptor.setModules(List.of(module));
        var project = project(repository(), "PricingProject", "PricingProject");
        when(project.isOpened()).thenReturn(true);
        when(acl.isGranted(project, List.of(BasePermission.WRITE))).thenReturn(true);
        when(webStudio.getProjectByName("design", "PricingProject")).thenReturn(descriptor);
        var service = newService(
                acl,
                mock(ProtectedBranchBypassService.class),
                null,
                mock(ProjectStateValidator.class),
                webStudio,
                mock(AclProjectsHelper.class),
                tableCreatorService);
        var request = new CreateNewTableRequest(
                "pricing",
                "Rules",
                "rules/Replacement.xlsx",
                rawTable("Replacement"));

        // Not locked when the request arrives, and locked by the request itself before the name is checked.
        when(project.isLockedByMe()).thenReturn(false, true);

        var exception = assertThrows(ConflictException.class, () -> service.createNewTable(project, request));

        assertEquals("openl.error.409.table.new-module.exists.message", exception.getErrorCode());
        verify(tableCreatorService, never()).createModuleWithTable(any(), any(), any(), any());
        // A duplicate name is ordinary input, and only an administrator can clear a lock left behind by one.
        verify(project).unlock();
    }

    @Test
    void created_table_is_found_when_its_name_is_longer_than_a_shortened_one() throws Exception {
        var summaryTableReader = mock(SummaryTableReader.class);
        var service = spy(newService(
                mock(RepositoryAclService.class),
                mock(ProtectedBranchBypassService.class),
                null,
                mock(ProjectStateValidator.class),
                mock(WebStudio.class),
                mock(AclProjectsHelper.class),
                mock(TableCreatorService.class),
                summaryTableReader));
        var project = mock(RulesProject.class);
        var projectModel = mock(ProjectModel.class);
        var handle = mock(ProjectHandle.class);
        var openLTable = mock(IOpenLTable.class);
        // Longer than the 57 characters a shortened display name keeps.
        var longName = "A".repeat(60);
        var expected = SummaryTableView.builder()
                .id("created-id")
                .tableType("RawSource")
                .kind("Datatype")
                .name(longName)
                .build();
        when(handle.awaitCompiled()).thenReturn(projectModel);
        when(projectModel.search(any(), eq(SearchScope.CURRENT_MODULE))).thenReturn(List.of(openLTable));
        when(summaryTableReader.read(openLTable)).thenReturn(expected);
        doReturn(handle).when(service).openProject(project, "Main");

        var created = service.getCreatedTable(project, "Main", null, longName);

        assertEquals(expected, created);
        // The search narrows by substring. Shortening the name appends a mark the table's own display name never
        // carries, so searching for the shortened form would find nothing and answer a successful create with no
        // table at all.
        var selector = forClass(Predicate.class);
        verify(projectModel).search(selector.capture(), eq(SearchScope.CURRENT_MODULE));
        assertTrue(selector.getValue().test(datatypeNode("Datatype " + longName)));
    }

    /** A syntax node the table selector can read a Datatype header from. */
    private static TableSyntaxNode datatypeNode(String header) {
        var node = mock(TableSyntaxNode.class);
        var headerNode = mock(HeaderSyntaxNode.class);
        when(node.getType()).thenReturn(XlsNodeTypes.XLS_DATATYPE.toString());
        when(node.getHeader()).thenReturn(headerNode);
        when(headerNode.getSourceString()).thenReturn(header);
        return node;
    }

    private static WorkspaceProjectService newService(RepositoryAclService acl,
                                                      ProtectedBranchBypassService bypassService) throws ProjectException {
        return newService(acl, bypassService, null);
    }

    private static WorkspaceProjectService newService(RepositoryAclService acl,
                                                      ProtectedBranchBypassService bypassService,
                                                      UserWorkspace userWorkspace) throws ProjectException {
        return newService(acl, bypassService, userWorkspace, mock(ProjectStateValidator.class), mock(WebStudio.class));
    }

    private static WorkspaceProjectService newService(RepositoryAclService acl,
                                                      ProtectedBranchBypassService bypassService,
                                                      UserWorkspace userWorkspace,
                                                      ProjectStateValidator projectStateValidator,
                                                      WebStudio webStudio) throws ProjectException {
        return newService(acl, bypassService, userWorkspace, projectStateValidator, webStudio,
                mock(AclProjectsHelper.class));
    }

    private static WorkspaceProjectService newService(RepositoryAclService acl,
                                                      ProtectedBranchBypassService bypassService,
                                                      UserWorkspace userWorkspace,
                                                      ProjectStateValidator projectStateValidator,
                                                      WebStudio webStudio,
                                                      AclProjectsHelper aclProjectsHelper) throws ProjectException {
        return newService(
                acl,
                bypassService,
                userWorkspace,
                projectStateValidator,
                webStudio,
                aclProjectsHelper,
                mock(TableCreatorService.class));
    }

    private static WorkspaceProjectService newService(RepositoryAclService acl,
                                                      ProtectedBranchBypassService bypassService,
                                                      UserWorkspace userWorkspace,
                                                      ProjectStateValidator projectStateValidator,
                                                      WebStudio webStudio,
                                                      AclProjectsHelper aclProjectsHelper,
                                                      ApplicationEventPublisher eventPublisher) throws ProjectException {
        return newService(
                acl,
                bypassService,
                userWorkspace,
                projectStateValidator,
                webStudio,
                aclProjectsHelper,
                mock(TableCreatorService.class),
                mock(SummaryTableReader.class),
                eventPublisher);
    }

    private static WorkspaceProjectService newService(RepositoryAclService acl,
                                                      ProtectedBranchBypassService bypassService,
                                                      UserWorkspace userWorkspace,
                                                      ProjectStateValidator projectStateValidator,
                                                      WebStudio webStudio,
                                                      AclProjectsHelper aclProjectsHelper,
                                                      TableCreatorService tableCreatorService) throws ProjectException {
        return newService(
                acl,
                bypassService,
                userWorkspace,
                projectStateValidator,
                webStudio,
                aclProjectsHelper,
                tableCreatorService,
                mock(SummaryTableReader.class),
                mock(ApplicationEventPublisher.class));
    }

    private static WorkspaceProjectService newService(RepositoryAclService acl,
                                                      ProtectedBranchBypassService bypassService,
                                                      UserWorkspace userWorkspace,
                                                      ProjectStateValidator projectStateValidator,
                                                      WebStudio webStudio,
                                                      AclProjectsHelper aclProjectsHelper,
                                                      TableCreatorService tableCreatorService,
                                                      SummaryTableReader summaryTableReader) throws ProjectException {
        return newService(
                acl,
                bypassService,
                userWorkspace,
                projectStateValidator,
                webStudio,
                aclProjectsHelper,
                tableCreatorService,
                summaryTableReader,
                mock(ApplicationEventPublisher.class));
    }

    private static WorkspaceProjectService newService(RepositoryAclService acl,
                                                      ProtectedBranchBypassService bypassService,
                                                      UserWorkspace userWorkspace,
                                                      ProjectStateValidator projectStateValidator,
                                                      WebStudio webStudio,
                                                      AclProjectsHelper aclProjectsHelper,
                                                      TableCreatorService tableCreatorService,
                                                      SummaryTableReader summaryTableReader,
                                                      ApplicationEventPublisher eventPublisher) throws ProjectException {
        return newService(
                acl,
                bypassService,
                userWorkspace,
                projectStateValidator,
                webStudio,
                aclProjectsHelper,
                tableCreatorService,
                summaryTableReader,
                eventPublisher,
                mock(MultiUserWorkspaceManager.class));
    }

    private static WorkspaceProjectService newService(RepositoryAclService acl,
                                                      ProtectedBranchBypassService bypassService,
                                                      UserWorkspace userWorkspace,
                                                      ProjectStateValidator projectStateValidator,
                                                      WebStudio webStudio,
                                                      AclProjectsHelper aclProjectsHelper,
                                                      TableCreatorService tableCreatorService,
                                                      SummaryTableReader summaryTableReader,
                                                      ApplicationEventPublisher eventPublisher,
                                                      MultiUserWorkspaceManager workspaceManager)
            throws ProjectException {
        var dependencyResolver = mock(ProjectDependencyResolver.class);
        when(dependencyResolver.getProjectDependencies(any(RulesProject.class))).thenReturn(List.of());
        doReturn(List.of()).when(dependencyResolver).getDependsOnProject(any(RulesProject.class));

        return new WorkspaceProjectService(
                acl,
                projectStateValidator,
                dependencyResolver,
                summaryTableReader,
                mock(RawTableReader.class),
                List.of(),
                repository -> mock(NewBranchValidator.class),
                mock(BeanValidationProvider.class),
                tableCreatorService,
                mock(ProjectMetadataService.class),
                mock(TableWriterExecutor.class),
                mock(TableWritersFactory.class),
                eventPublisher,
                bypassService,
                mock(ProjectIdentifierMapper.class),
                mock(DetailedMessageDescriptionMapper.class),
                mock(LocalWorkspaceManager.class),
                workspaceManager,
                aclProjectsHelper,
                mock(ProjectAccessService.class),
                mock(ProjectStatusMapper.class),
                environment(),
                new ProjectTagsCache(mock(CacheManager.class)),
                new ProjectListingContext()) {

            @Override
            public UserWorkspace getUserWorkspace() {
                return userWorkspace;
            }

            @Override
            public WebStudio getWebStudio() {
                return webStudio;
            }
        };
    }

    private static WorkspaceProjectService newDeleteService(RepositoryAclService acl,
                                                            UserWorkspace userWorkspace,
                                                            ProjectStateValidator projectStateValidator,
                                                            WebStudio webStudio,
                                                            AclProjectsHelper aclProjectsHelper,
                                                            MultiUserWorkspaceManager workspaceManager)
            throws ProjectException {
        return newService(
                acl,
                mock(ProtectedBranchBypassService.class),
                userWorkspace,
                projectStateValidator,
                webStudio,
                aclProjectsHelper,
                mock(TableCreatorService.class),
                mock(SummaryTableReader.class),
                mock(ApplicationEventPublisher.class),
                workspaceManager);
    }

    private static RawTableView rawTable(String name) {
        return RawTableView.builder()
                .kind("Rules")
                .name(name)
                .source(List.of())
                .build();
    }

    private static Environment environment() {
        var environment = mock(Environment.class);
        when(environment.getProperty("data.format.datetime")).thenReturn("yyyy-MM-dd HH:mm:ss");
        when(environment.getProperty("repository.design.comment-template.user-message.default.save"))
                .thenReturn("Save {project-name}");
        when(environment.getProperty("repository.design.comment-template.user-message.default.restored-from"))
                .thenReturn("Restore revision {revision} by {author} at {datetime}");
        Props.setEnvironment(environment);
        return environment;
    }

    private static Environment commentValidationEnvironment() {
        var environment = mock(Environment.class);
        when(environment.getProperty("repository.design.comment-template.use-custom-comments")).thenReturn("true");
        when(environment.getProperty("repository.design.comment-template.comment-validation-pattern"))
                .thenReturn("MATCH-.+");
        when(environment.getProperty("repository.design.comment-template.invalid-comment-message"))
                .thenReturn("Invalid generated comment");
        return environment;
    }

    private RulesProject projectWithSimpleExcelModule() throws Exception {
        var projectFolder = tempDir.resolve("PricingProject");
        Files.createDirectories(projectFolder);
        Files.createFile(projectFolder.resolve("Pricing.xlsx"));

        var repository = repository();
        return project(repository, "PricingProject", "PricingProject");
    }

    private RulesProject project(Repository repository, String name, String folderPath) {
        var project = mock(RulesProject.class);
        fillProject(project, repository, name, folderPath);
        return project;
    }

    private RulesProject closedProject(String descriptor, String... files) throws Exception {
        var repositoryRoot = tempDir.resolve("design-repository-" + System.nanoTime());
        Files.createDirectories(repositoryRoot);
        var repository = new FileSystemRepository();
        repository.setRoot(repositoryRoot);
        repository.setId("design");
        repository.setName("Design");

        var projectName = "ClosedProject";
        var projectFolder = repositoryRoot.resolve(projectName);
        Files.createDirectories(projectFolder);
        Files.writeString(projectFolder.resolve("rules.xml"), descriptor);
        for (var file : files) {
            var path = projectFolder.resolve(file);
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createFile(path);
        }

        var user = mock(WorkspaceUser.class);
        when(user.getUserName()).thenReturn("user");
        var localRepoRoot = tempDir.resolve("local-repository-" + System.nanoTime());
        var localRepository = new LocalRepository(localRepoRoot, MetainfoRegistry.open(localRepoRoot));
        var designFileData = repository.check(projectName);
        var project = new RulesProject(user, localRepository, null, repository, designFileData, mock(LockEngine.class));
        project.setFileData(designFileData);
        return project;
    }

    private void fillProject(RulesProject project, Repository repository, String name, String folderPath) {
        when(project.getRepository()).thenReturn(repository);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getBusinessName()).thenReturn(name);
        when(project.getName()).thenReturn(name);
        when(project.getDesignProjectName()).thenReturn(name);
        when(project.getDesignFolderName()).thenReturn(folderPath);
        when(project.getFolderPath()).thenReturn(folderPath);
        when(project.getLocalTags()).thenReturn(Map.of());
    }

    private static Repository repository() {
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        when(repository.getName()).thenReturn("Design");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        return repository;
    }

    private UserWorkspace workspaceFor(RulesProject... projects) {
        var localRepository = mock(LocalRepository.class);
        when(localRepository.getRoot()).thenReturn(tempDir);

        var localWorkspace = mock(LocalWorkspace.class);
        when(localWorkspace.getRepository("design")).thenReturn(localRepository);

        var userWorkspace = mock(UserWorkspace.class);
        when(userWorkspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(userWorkspace.getProjects()).thenReturn(List.of(projects));
        when(userWorkspace.getDesignTimeRepository()).thenReturn(mock(DesignTimeRepository.class));
        return userWorkspace;
    }

    private static void mockMembership(UserWorkspace workspace, RulesProject project, String... branches) {
        var entries = new java.util.LinkedHashMap<String, BranchedProject.BranchEntry>();
        for (String branch : branches) {
            entries.put(branch, mock(BranchedProject.BranchEntry.class));
        }
        var branchedProject = mock(BranchedProject.class);
        when(branchedProject.entries()).thenReturn(entries);
        when(workspace.getDesignTimeRepository()
                .getBranchedProject(project.getDesignRepository().getId(), project.getDesignProjectName()))
                .thenReturn(Optional.of(branchedProject));
    }

    private UserWorkspace userWorkspaceWithNonDirectoryParent() throws Exception {
        var workspacesRoot = tempDir.resolve("workspaces-parent-file");
        Files.writeString(workspacesRoot, "not a directory");
        var localWorkspace = mock(LocalWorkspace.class);
        when(localWorkspace.getLocation()).thenReturn(workspacesRoot.resolve("user").toFile());

        var userWorkspace = mock(UserWorkspace.class);
        when(userWorkspace.getLocalWorkspace()).thenReturn(localWorkspace);
        when(userWorkspace.getDesignTimeRepository()).thenReturn(mock(DesignTimeRepository.class));
        return userWorkspace;
    }

}
