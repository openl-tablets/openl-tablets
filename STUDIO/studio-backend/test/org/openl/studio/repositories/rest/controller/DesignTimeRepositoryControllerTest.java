package org.openl.studio.repositories.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.openl.rules.workspace.ProjectId;
import org.openl.rules.workspace.ProjectIds;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.converter.ProjectIdConverter;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.repositories.model.CreateFromProjectModel;
import org.openl.studio.repositories.model.CreateUpdateProjectModel;
import org.openl.studio.repositories.model.RepositoryConfigModel;
import org.openl.studio.repositories.service.DesignTimeRepositoryService;
import org.openl.studio.repositories.service.ProjectCreationService;
import org.openl.studio.repositories.service.ProjectCreationTargetResolver;
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
    private ProjectIdConverter projectIdConverter;
    private Comments comments;
    private DesignTimeRepositoryController controller;

    @BeforeEach
    void setUp() {
        repository = mock(BranchRepository.class);
        designRepositoryAclService = mock(RepositoryAclService.class);
        validationProvider = mock(BeanValidationProvider.class);
        zipProjectSaveStrategy = mock(ZipProjectSaveStrategy.class);
        repositoryConfigService = mock(RepositoryConfigService.class);
        projectIdConverter = mock(ProjectIdConverter.class);
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
                bypassService,
                projectCreationService,
                projectCreationTargetResolver,
                repositoryConfigService,
                projectIdConverter) {
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
    void createProjectFromProjectRequiresBranchProtectionBypass() {
        var source = sourceProject("Source");
        when(projectCreationService.copyProject(repository, "Copy", null, source, "comment", "rev-1"))
                .thenReturn(created("Copy"));

        controller.createProjectFromProject(repository, "Copy",
                new CreateFromProjectModel(sourceId("Source"), null, "comment", "rev-1"));

        verify(bypassService).requireBypassOrThrow(repository, BRANCH, REPOSITORY_ID, false);
    }

    @Test
    void anOmittedCommentNamesTheProjectThatIsActuallyCopied() {
        // The request addresses its source by an id, which is no name to put in a commit comment.
        var source = sourceProject("Source");
        when(source.getBusinessName()).thenReturn("Source Rules");
        when(comments.copiedFrom("Source Rules")).thenReturn("Copied from: Source Rules.");
        when(projectCreationService.copyProject(repository, "Copy", null, source, "Copied from: Source Rules.", null))
                .thenReturn(created("Copy"));

        controller.createProjectFromProject(repository, "Copy",
                new CreateFromProjectModel(sourceId("Source"), null, null, null));

        verify(comments).copiedFrom("Source Rules");
    }

    @Test
    void projectCopyUsesTheResolvedTargetBranch() {
        var target = mock(BranchRepository.class);
        var data = created("Copy");
        when(target.getId()).thenReturn(REPOSITORY_ID);
        when(target.supports()).thenReturn(new FeaturesBuilder(target).setBranches(true).build());
        when(projectCreationTargetResolver.resolve(repository, "feature/rates")).thenReturn(target);
        var source = sourceProject("Source");
        when(projectCreationService.copyProject(target, "Copy", null, source, "comment", null)).thenReturn(data);

        controller.createProjectFromProject(repository, "Copy",
                new CreateFromProjectModel(sourceId("Source"), null, "comment", null, "feature/rates"));

        verify(projectCreationTargetResolver).resolve(repository, "feature/rates");
        verify(projectCreationService).copyProject(target, "Copy", null, source, "comment", null);
    }

    @Test
    void copyChecksBranchProtectionBeforeResolvingTheTarget() {
        rejectRequestedBranch("protected/new");
        sourceProject("Source");
        var request = new CreateFromProjectModel(sourceId("Source"), null, "comment", null, "protected/new");

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

        var id = idOfWritten(repository, data);

        var created = controller.createProject(repository, "Project", null, "comment", null, "predefined",
                "templates", "Sample Project", "Models", "rules/Models.xlsx", "Algorithms", "rules/Algorithms.xlsx",
                false, null, null, false);

        verify(projectCreationService).applyStatusAfterCreate(repository, "Project:hash", ProjectStatus.VIEWING);
        // The answer names the project by the id of the folder written, which the project list reports for it.
        assertEquals(id, created.id);
    }

    @Test
    void copyAnswersTheIdOfTheCopyWithTheBranchAndTheRevisionOfItsCommit() {
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        var source = sourceProject("Source");
        var data = created("Copy:hash");
        data.setBranch(BRANCH);
        data.setVersion("rev-2");
        when(projectCreationService.copyProject(repository, "Copy", null, source, "comment", null)).thenReturn(data);
        var id = idOfWritten(repository, data);

        var copy = controller.createProjectFromProject(repository, "Copy",
                new CreateFromProjectModel(sourceId("Source"), null, "comment", null));

        assertEquals(id, copy.id);
        assertEquals(BRANCH, copy.branch);
        assertEquals("rev-2", copy.revision);
    }

    /** An id no project of the workspace has, a name or an id of an earlier OpenL Studio among them. */
    @Test
    void copyOfAnUnknownSourceIsNotFound() {
        var request = new CreateFromProjectModel(sourceId("Missing"), null, "comment", null);

        var error = assertThrows(NotFoundException.class,
                () -> controller.createProjectFromProject(repository, "Copy", request));

        assertEquals("openl.error.404.project.identifier.message", error.getErrorCode());
        verify(projectCreationService, never()).copyProject(any(Repository.class), any(), any(),
                any(RulesProject.class), any(), any());
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

    /** What the repository answers a project write with: the design folder the project is listed under. */
    private static FileData created(String indexedName) {
        var data = new FileData();
        data.setName("DESIGN/rules/" + indexedName);
        return data;
    }

    /** The id the creation service tells for what a write answered. */
    private ProjectId idOfWritten(Repository target, FileData data) {
        var id = ProjectIds.of(REPOSITORY_ID, data.getName());
        when(projectCreationService.idOfWritten(target, data)).thenReturn(id);
        return id;
    }

    /** The id of the project in the given design folder. */
    private static ProjectId sourceId(String folder) {
        return ProjectIds.of(REPOSITORY_ID, "DESIGN/rules/" + folder);
    }

    /** The project the request names as its source, as the id converter finds it. */
    private RulesProject sourceProject(String folder) {
        var source = mock(RulesProject.class);
        when(source.getBusinessName()).thenReturn(folder);
        when(projectIdConverter.find(sourceId(folder))).thenReturn(source);
        return source;
    }

    private void rejectRequestedBranch(String branch) {
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        doThrow(new ConflictException("repository.branch.message"))
                .when(bypassService)
                .requireBypassOrThrow(repository, branch, REPOSITORY_ID, false);
    }

}
