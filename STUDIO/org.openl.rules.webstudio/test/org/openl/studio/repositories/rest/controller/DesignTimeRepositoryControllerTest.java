package org.openl.studio.repositories.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.repositories.model.CreateFromProjectModel;
import org.openl.studio.repositories.model.CreateFromRepositoryModel;
import org.openl.studio.repositories.model.CreateFromWorkspaceModel;
import org.openl.studio.repositories.model.CreateUpdateProjectModel;
import org.openl.studio.repositories.service.DesignTimeRepositoryService;
import org.openl.studio.repositories.service.ProjectCreationService;
import org.openl.studio.repositories.service.ProjectRevisionService;
import org.openl.studio.repositories.service.ZipProjectSaveStrategy;
import org.openl.studio.repositories.validator.CreateUpdateProjectModelValidator;
import org.openl.studio.repositories.validator.ZipArchiveValidator;

class DesignTimeRepositoryControllerTest {

    private static final String REPOSITORY_ID = "design";
    private static final String BRANCH = "main";

    private BranchRepository repository;
    private DesignTimeRepository designTimeRepository;
    private RepositoryAclService designRepositoryAclService;
    private BeanValidationProvider validationProvider;
    private ZipProjectSaveStrategy zipProjectSaveStrategy;
    private AclProjectsHelper aclProjectsHelper;
    private ProtectedBranchBypassService bypassService;
    private ProjectCreationService projectCreationService;
    private DesignTimeRepositoryController controller;

    @BeforeEach
    void setUp() {
        repository = mock(BranchRepository.class);
        designTimeRepository = mock(DesignTimeRepository.class);
        designRepositoryAclService = mock(RepositoryAclService.class);
        validationProvider = mock(BeanValidationProvider.class);
        zipProjectSaveStrategy = mock(ZipProjectSaveStrategy.class);
        aclProjectsHelper = mock(AclProjectsHelper.class);
        bypassService = mock(ProtectedBranchBypassService.class);
        projectCreationService = mock(ProjectCreationService.class);

        when(repository.getId()).thenReturn(REPOSITORY_ID);
        when(repository.getBranch()).thenReturn(BRANCH);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());

        controller = new DesignTimeRepositoryController(
                designTimeRepository,
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
                projectCreationService);

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProjectsFromWorkspaceRequiresBranchProtectionBypass() {
        controller.createProjectsFromWorkspace(repository, new CreateFromWorkspaceModel(List.of("Project"), null, "comment"));

        verify(bypassService).requireBypassOrThrow(repository, BRANCH, REPOSITORY_ID, false);
    }

    @Test
    void createProjectFromProjectRequiresBranchProtectionBypass() {
        when(projectCreationService.copyProject(eq(REPOSITORY_ID), eq("Copy"), eq(null), eq(REPOSITORY_ID),
                eq("Source"), eq("comment"))).thenReturn(new FileData());

        controller.createProjectFromProject(repository, "Copy",
                new CreateFromProjectModel(REPOSITORY_ID, "Source", null, "comment"));

        verify(bypassService).requireBypassOrThrow(repository, BRANCH, REPOSITORY_ID, false);
    }

    @Test
    void createProjectFromArchiveRegistersTagsFromDesignProject() throws Exception {
        when(aclProjectsHelper.hasCreateProjectPermission(REPOSITORY_ID)).thenReturn(true);
        var archive = mock(MultipartFile.class);
        when(archive.getOriginalFilename()).thenReturn("Project.zip");
        when(archive.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(zipProjectSaveStrategy.save(any(CreateUpdateProjectModel.class), any(Path.class)))
                .thenReturn(new FileData());
        var project = mock(AProject.class);
        when(designTimeRepository.getProject(REPOSITORY_ID, "Project")).thenReturn(project);

        controller.createProject(repository, "Project", null, "comment", List.of(archive), null, null, null,
                "Models", "rules/Models.xlsx", "Algorithms", "rules/Algorithms.xlsx", false, false);

        verify(projectCreationService).registerExtensibleTagsAfterDesignChange(project);
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

    @Test
    void createProjectFromRepositoryRequiresBranchProtectionBypass() {
        when(projectCreationService.importFromRepository(REPOSITORY_ID, "folder", Map.of())).thenReturn(new FileData());

        controller.createProjectFromRepository(repository, new CreateFromRepositoryModel("folder", Map.of()));

        verify(bypassService).requireBypassOrThrow(repository, BRANCH, REPOSITORY_ID, false);
    }

    @Test
    void createProjectFromRepositoryRequestBodyIsValidated() throws NoSuchMethodException {
        var method = DesignTimeRepositoryController.class.getMethod("createProjectFromRepository", Repository.class,
                CreateFromRepositoryModel.class);
        var requestParameter = method.getParameters()[1];

        assertTrue(requestParameter.isAnnotationPresent(Valid.class));
    }

}
