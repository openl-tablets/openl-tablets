package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.ModuleViewModel;
import org.openl.studio.projects.model.ProjectBranchInfo;
import org.openl.studio.projects.model.ProjectInclude;
import org.openl.studio.projects.model.ProjectStatusUpdateModel;
import org.openl.studio.projects.service.history.ProjectHistoryService;
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
        var service = newService(acl, mock(ProtectedBranchBypassService.class));
        var project = mock(RulesProject.class);
        var repository = mock(BranchRepository.class);
        var branchNames = List.of("main", "feature");

        when(project.isSupportsBranches()).thenReturn(true);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getBranch()).thenReturn("main");
        when(acl.isGranted(project, List.of(BasePermission.READ))).thenReturn(true);
        when(repository.getBaseBranch()).thenReturn("main");
        when(repository.isBranchProtected("main")).thenReturn(true);
        when(repository.getBranches(null)).thenReturn(branchNames);

        List<ProjectBranchInfo> result = service.getBranches(project);

        assertEquals(List.of("feature", "main"), result.stream().map(ProjectBranchInfo::name).toList());
        assertEquals(List.of(false, true), result.stream().map(ProjectBranchInfo::base).toList());
        assertEquals(List.of(false, true), result.stream().map(ProjectBranchInfo::protectedFlag).toList());
        // The branch list feeds pickers only, so it never pays for the tip commits.
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
    void get_project_keeps_branch_metadata_without_modules() throws Exception {
        var project = projectWithSimpleExcelModule();
        when(project.isSupportsBranches()).thenReturn(true);
        when(project.getSelectedBranches()).thenReturn(new ArrayList<>(List.of("release", "master")));
        var service = newService(mock(RepositoryAclService.class), mock(ProtectedBranchBypassService.class),
                workspaceFor(project));

        var result = service.getProject(project);

        assertEquals(List.of("master", "release"), result.selectedBranches);
        assertNull(result.descriptor);
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
        var matched = result.descriptor.modules().get(0).modules();
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
                result.descriptor.modules().get(0).modules().stream().map(ModuleViewModel::name).toList());
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
                result.descriptor.modules().get(0).modules().stream().map(ModuleViewModel::path).toList());
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

        verify(project).setBranch("release");
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
        verify(project).setBranch("feature");
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
        var dependencyResolver = mock(ProjectDependencyResolver.class);
        when(dependencyResolver.getProjectDependencies(any(RulesProject.class))).thenReturn(List.of());
        doReturn(List.of()).when(dependencyResolver).getDependsOnProject(any(RulesProject.class));

        return new WorkspaceProjectService(
                acl,
                projectStateValidator,
                dependencyResolver,
                mock(SummaryTableReader.class),
                mock(RawTableReader.class),
                List.of(),
                repository -> mock(NewBranchValidator.class),
                mock(BeanValidationProvider.class),
                mock(TableCreatorService.class),
                mock(TableWriterExecutor.class),
                mock(TableWritersFactory.class),
                mock(ApplicationEventPublisher.class),
                bypassService,
                mock(ProjectIdentifierMapper.class),
                mock(DetailedMessageDescriptionMapper.class),
                mock(LocalWorkspaceManager.class),
                mock(MultiUserWorkspaceManager.class),
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
        FileData designFileData = repository.check(projectName);
        var project = new RulesProject(user, localRepository, null, repository, designFileData, mock(LockEngine.class));
        project.setFileData(designFileData);
        return project;
    }

    private void fillProject(RulesProject project, Repository repository, String name, String folderPath) {
        when(project.getRepository()).thenReturn(repository);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getBusinessName()).thenReturn(name);
        when(project.getName()).thenReturn(name);
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
        return userWorkspace;
    }

    private UserWorkspace userWorkspaceWithNonDirectoryParent() throws Exception {
        var workspacesRoot = tempDir.resolve("workspaces-parent-file");
        Files.writeString(workspacesRoot, "not a directory");
        var localWorkspace = mock(LocalWorkspace.class);
        when(localWorkspace.getLocation()).thenReturn(workspacesRoot.resolve("user").toFile());

        var userWorkspace = mock(UserWorkspace.class);
        when(userWorkspace.getLocalWorkspace()).thenReturn(localWorkspace);
        return userWorkspace;
    }

}
