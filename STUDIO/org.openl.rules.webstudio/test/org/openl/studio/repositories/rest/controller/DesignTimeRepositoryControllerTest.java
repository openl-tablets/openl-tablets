package org.openl.studio.repositories.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.RepositoryProjectService;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.repositories.model.CreateFromProjectModel;
import org.openl.studio.repositories.model.CreateFromRepositoryModel;
import org.openl.studio.repositories.model.CreateFromWorkspaceModel;
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
    private ProtectedBranchBypassService bypassService;
    private ProjectCreationService projectCreationService;
    private DesignTimeRepositoryController controller;

    @BeforeEach
    void setUp() {
        repository = mock(BranchRepository.class);
        bypassService = mock(ProtectedBranchBypassService.class);
        projectCreationService = mock(ProjectCreationService.class);

        when(repository.getId()).thenReturn(REPOSITORY_ID);
        when(repository.getBranch()).thenReturn(BRANCH);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());

        controller = new DesignTimeRepositoryController(
                mock(DesignTimeRepository.class),
                mock(RepositoryAclService.class),
                mock(BeanValidationProvider.class),
                mock(CreateUpdateProjectModelValidator.class),
                mock(ZipArchiveValidator.class),
                mock(ZipProjectSaveStrategy.class),
                "target",
                mock(RepositoryProjectService.class),
                mock(AclProjectsHelper.class),
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
