package org.openl.studio.repositories.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.converter.ProjectIdentityConverter;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.repositories.model.CreateFromProjectModel;
import org.openl.studio.repositories.model.CreateFromWorkspaceModel;
import org.openl.studio.repositories.model.CreateUpdateProjectModel;
import org.openl.studio.repositories.model.RepositoryConfigModel;
import org.openl.studio.repositories.service.DesignTimeRepositoryService;
import org.openl.studio.repositories.service.ProjectCreationService;
import org.openl.studio.repositories.service.ProjectCreationTargetResolver;
import org.openl.studio.repositories.service.ProjectRevisionService;
import org.openl.studio.repositories.service.RepositoryConfigService;
import org.openl.studio.repositories.service.ZipProjectSaveStrategy;
import org.openl.studio.repositories.validator.CreateUpdateProjectModelValidator;
import org.openl.studio.repositories.validator.ZipArchiveValidator;

class DesignTimeRepositoryControllerTest {

    private static final String REPOSITORY_ID = "design";
    private static final String BRANCH = "main";

    private BranchRepository repository;
    private RepositoryAclService designRepositoryAclService;
    private BeanValidationProvider validationProvider;
    private ZipProjectSaveStrategy zipProjectSaveStrategy;
    private AclProjectsHelper aclProjectsHelper;
    private ProtectedBranchBypassService bypassService;
    private ProjectCreationService projectCreationService;
    private ProjectCreationTargetResolver projectCreationTargetResolver;
    private RepositoryConfigService repositoryConfigService;
    private ProjectIdentityConverter projectIdentityConverter;
    private Comments comments;
    private DesignTimeRepositoryController controller;

    @BeforeEach
    void setUp() {
        repository = mock(BranchRepository.class);
        designRepositoryAclService = mock(RepositoryAclService.class);
        validationProvider = mock(BeanValidationProvider.class);
        zipProjectSaveStrategy = mock(ZipProjectSaveStrategy.class);
        repositoryConfigService = mock(RepositoryConfigService.class);
        projectIdentityConverter = mock(ProjectIdentityConverter.class);
        comments = mock(Comments.class);
        aclProjectsHelper = mock(AclProjectsHelper.class);
        bypassService = mock(ProtectedBranchBypassService.class);
        projectCreationService = mock(ProjectCreationService.class);
        projectCreationTargetResolver = mock(ProjectCreationTargetResolver.class);

        when(repository.getId()).thenReturn(REPOSITORY_ID);
        when(repository.getBranch()).thenReturn(BRANCH);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        when(aclProjectsHelper.hasCreateProjectPermission(REPOSITORY_ID)).thenReturn(true);
        when(projectCreationTargetResolver.resolve(eq(repository), nullable(String.class))).thenReturn(repository);
        when(projectCreationTargetResolver.resolve(eq(repository), nullable(String.class), any(Boolean.class)))
                .thenReturn(repository);

        controller = new DesignTimeRepositoryController(
                designRepositoryAclService,
                validationProvider,
                mock(CreateUpdateProjectModelValidator.class),
                mock(ZipArchiveValidator.class),
                zipProjectSaveStrategy,
                "target",
                aclProjectsHelper,
                mock(DesignTimeRepositoryService.class),
                mock(ProjectRevisionService.class),
                bypassService,
                projectCreationService,
                projectCreationTargetResolver,
                repositoryConfigService,
                projectIdentityConverter) {
            // The comment service is a @Lookup bean, absent outside a Spring context.
            @Override
            protected Comments getCommentsService(String repoName) {
                return comments;
            }
        };

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void configOfTheRepositoryIsReadForTheCreateForms() {
        var config = new RepositoryConfigModel(null, new RepositoryConfigModel.Comment(null, null,
                new RepositoryConfigModel.Templates(null, "Project {project-name} is created.", null, null)));
        when(repositoryConfigService.getConfig(REPOSITORY_ID)).thenReturn(config);

        assertEquals(config, controller.getConfig(repository));
    }

    @Test
    void createProjectsFromWorkspaceRequiresBranchProtectionBypass() {
        controller.createProjectsFromWorkspace(repository, new CreateFromWorkspaceModel(List.of("Project"), null, "comment"));

        verify(bypassService).requireBypassOrThrow(repository, BRANCH, REPOSITORY_ID, false);
    }

    @Test
    void createProjectFromProjectRequiresBranchProtectionBypass() {
        var source = sourceProject("Source");
        when(projectCreationService.copyProject(repository, "Copy", null, source, "comment", "rev-1"))
                .thenReturn(new FileData());

        controller.createProjectFromProject(repository, "Copy",
                new CreateFromProjectModel(REPOSITORY_ID, "Source", null, "comment", "rev-1"));

        verify(bypassService).requireBypassOrThrow(repository, BRANCH, REPOSITORY_ID, false);
    }

    @Test
    void anOmittedCommentNamesTheProjectThatIsActuallyCopied() {
        // The request may address its source by an identifier, which is no name to put in a commit comment.
        var source = sourceProject("ZGVzaWduOlNvdXJjZTpoYXNo");
        when(source.getBusinessName()).thenReturn("Source");
        when(comments.copiedFrom("Source")).thenReturn("Copied from: Source.");
        when(projectCreationService.copyProject(repository, "Copy", null, source, "Copied from: Source.", null))
                .thenReturn(new FileData());

        controller.createProjectFromProject(repository, "Copy",
                new CreateFromProjectModel(REPOSITORY_ID, "ZGVzaWduOlNvdXJjZTpoYXNo", null, null, null));

        verify(comments).copiedFrom("Source");
    }

    @Test
    void publishFromWorkspaceUsesTheResolvedTargetBranch() {
        var target = mock(BranchRepository.class);
        when(target.getId()).thenReturn(REPOSITORY_ID);
        when(target.supports()).thenReturn(new FeaturesBuilder(target).setBranches(true).build());
        when(target.getBranch()).thenReturn("feature/rates");
        when(projectCreationTargetResolver.resolve(repository, "feature/rates")).thenReturn(target);

        controller.createProjectsFromWorkspace(repository,
                new CreateFromWorkspaceModel(List.of("Project"), null, "comment", "feature/rates"));

        verify(projectCreationTargetResolver).resolve(repository, "feature/rates");
        verify(projectCreationService).uploadLocalProjects(target, List.of("Project"), null, "comment");
    }

    @Test
    void projectCopyUsesTheResolvedTargetBranch() {
        var target = mock(BranchRepository.class);
        var data = new FileData();
        when(target.getId()).thenReturn(REPOSITORY_ID);
        when(target.supports()).thenReturn(new FeaturesBuilder(target).setBranches(true).build());
        when(projectCreationTargetResolver.resolve(repository, "feature/rates")).thenReturn(target);
        var source = sourceProject("Source");
        when(projectCreationService.copyProject(target, "Copy", null, source, "comment", null)).thenReturn(data);

        controller.createProjectFromProject(repository, "Copy",
                new CreateFromProjectModel(REPOSITORY_ID, "Source", null, "comment", null, "feature/rates"));

        verify(projectCreationTargetResolver).resolve(repository, "feature/rates");
        verify(projectCreationService).copyProject(target, "Copy", null, source, "comment", null);
    }

    @Test
    void publishChecksBranchProtectionBeforeResolvingTheTarget() {
        rejectRequestedBranch("protected/new");
        var request = new CreateFromWorkspaceModel(List.of("Project"), null, "comment", "protected/new");

        assertThrows(ConflictException.class, () -> controller.createProjectsFromWorkspace(repository, request));

        verify(projectCreationTargetResolver, never()).resolve(repository, "protected/new");
        verify(projectCreationService, never()).uploadLocalProjects(any(Repository.class), anyList(), any(), any());
    }

    @Test
    void copyChecksBranchProtectionBeforeResolvingTheTarget() {
        rejectRequestedBranch("protected/new");
        sourceProject("Source");
        var request = new CreateFromProjectModel(REPOSITORY_ID,
                "Source",
                null,
                "comment",
                null,
                "protected/new");

        assertThrows(ConflictException.class,
                () -> controller.createProjectFromProject(repository, "Copy", request));

        verify(projectCreationTargetResolver, never()).resolve(repository, "protected/new");
        verify(projectCreationService, never()).copyProject(any(Repository.class), any(), any(),
                any(RulesProject.class), any(), any());
    }

    @Test
    void createChecksBranchProtectionBeforeResolvingTheTarget() {
        rejectRequestedBranch("protected/new");

        assertThrows(ConflictException.class,
                () -> controller.createProject(repository,
                        "Project",
                        null,
                        "comment",
                        null,
                        "predefined",
                        "templates",
                        "Sample Project",
                        "Models",
                        "rules/Models.xlsx",
                        "Algorithms",
                        "rules/Algorithms.xlsx",
                        false,
                        null,
                        "protected/new",
                        false));

        verify(projectCreationTargetResolver, never()).resolve(repository, "protected/new", true);
        verify(projectCreationService, never()).createFromTemplate(any(Repository.class), any(), any(), any(), any(),
                any(), any(), any());
    }

    @Test
    void createProjectFromArchiveRegistersTagsFromDesignProject() throws Exception {
        when(aclProjectsHelper.hasCreateProjectPermission(REPOSITORY_ID)).thenReturn(true);
        var archive = mock(MultipartFile.class);
        when(archive.getOriginalFilename()).thenReturn("Project.zip");
        when(archive.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        var data = new FileData();
        data.setName("Project");
        when(zipProjectSaveStrategy.save(eq(repository), any(CreateUpdateProjectModel.class), any(Path.class)))
                .thenReturn(data);

        controller.createProject(repository, "Project", null, "comment", List.of(archive), null, null, null,
                "Models", "rules/Models.xlsx", "Algorithms", "rules/Algorithms.xlsx", false, null, null, false);

        var order = inOrder(designRepositoryAclService, projectCreationService);
        order.verify(designRepositoryAclService).createAcl(any(AProject.class), anyList(), eq(true));
        order.verify(projectCreationService).registerExtensibleTags(any(AProject.class));
        order.verify(projectCreationService).awaitProjectVisibility(repository);
        order.verify(projectCreationService).refreshWorkspaceAfterDesignChange();
        verify(projectCreationService).applyStatusAfterCreate(repository, "Project", null);
    }

    @Test
    void contentCreationKeepsItsOpenedDefaultAndUsesTheIndexedProjectName() throws Exception {
        var data = new FileData();
        data.setName("Project:hash");
        when(projectCreationService.createFromTemplate(repository, "Project", null,
                "predefined", "templates", "Sample Project", "comment", null)).thenReturn(data);

        controller.createProject(repository, "Project", null, "comment", null, "predefined", "templates",
                "Sample Project", "Models", "rules/Models.xlsx", "Algorithms", "rules/Algorithms.xlsx",
                false, null, null, false);

        verify(projectCreationService).applyStatusAfterCreate(repository, "Project:hash", ProjectStatus.VIEWING);
    }

    @Test
    void archiveIndexFailureKeepsFinalizedAclAndTags() throws Exception {
        when(aclProjectsHelper.hasCreateProjectPermission(REPOSITORY_ID)).thenReturn(true);
        var archive = mock(MultipartFile.class);
        when(archive.getOriginalFilename()).thenReturn("Project.zip");
        when(archive.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        var data = new FileData();
        data.setName("Project");
        when(zipProjectSaveStrategy.save(eq(repository), any(CreateUpdateProjectModel.class), any(Path.class)))
                .thenReturn(data);
        doThrow(new ConflictException("project.indexing.incomplete.message"))
                .when(projectCreationService).awaitProjectVisibility(repository);

        var archives = List.of(archive);
        assertThrows(ConflictException.class,
                () -> controller.createProject(repository, "Project", null, "comment", archives, null,
                        null, null, "Models", "rules/Models.xlsx", "Algorithms", "rules/Algorithms.xlsx",
                        false, null, null, false));

        verify(designRepositoryAclService).createAcl(any(AProject.class), anyList(), eq(true));
        verify(projectCreationService).registerExtensibleTags(any(AProject.class));
        verify(projectCreationService, never()).refreshWorkspaceAfterDesignChange();
    }

    @Test
    void createProjectFromProjectRequestBodyIsValidated() throws NoSuchMethodException {
        var method = DesignTimeRepositoryController.class.getMethod("createProjectFromProject", Repository.class,
                String.class, CreateFromProjectModel.class);
        var requestParameter = method.getParameters()[2];

        assertTrue(requestParameter.isAnnotationPresent(Valid.class));
    }

    @Test
    void createProjectsFromWorkspaceRequestBodyIsValidated() throws NoSuchMethodException {
        var method = DesignTimeRepositoryController.class.getMethod("createProjectsFromWorkspace", Repository.class,
                CreateFromWorkspaceModel.class);
        var requestParameter = method.getParameters()[1];

        assertTrue(requestParameter.isAnnotationPresent(Valid.class));
    }

    @Test
    void createProjectsFromWorkspaceLimitsBatchSize() throws NoSuchMethodException {
        var size = CreateFromWorkspaceModel.class.getMethod("names").getAnnotation(Size.class);

        assertNotNull(size);
        assertEquals(CreateFromWorkspaceModel.MAX_PROJECTS, size.max());
    }

    /** The project the request names as its source, as the identity converter resolves it. */
    private RulesProject sourceProject(String identifier) {
        var source = mock(RulesProject.class);
        when(projectIdentityConverter.resolveProjectIdentity(identifier, REPOSITORY_ID)).thenReturn(source);
        return source;
    }


    private void rejectRequestedBranch(String branch) {
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        doThrow(new ConflictException("repository.branch.message"))
                .when(bypassService)
                .requireBypassOrThrow(repository, branch, REPOSITORY_ID, false);
    }

}
