package org.openl.studio.deployment.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.security.User;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositoryType;
import org.openl.rules.webstudio.web.repository.DeploymentException;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.DeploymentRequest;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.rules.webstudio.web.repository.UnreadableProjectException;
import org.openl.rules.webstudio.web.repository.cache.CachedProjectVersion;
import org.openl.rules.webstudio.web.repository.cache.ProjectVersionCacheManager;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.service.ProjectAuditor;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.deployment.model.DeploymentId;
import org.openl.studio.projects.service.ProjectDependencyResolver;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * Reading deployments must survive what it cannot reach: a broken repository is skipped with a logged error
 * while the remaining ones still answer, and an unreadable version cache costs a design revision, not the
 * whole listing.
 */
class DeploymentServiceImplTest {

    private SecureDeploymentRepositoryService deploymentRepositoryService;
    private DeploymentManager deploymentManager;
    private ProjectVersionCacheManager projectVersionCacheManager;
    private DeploymentServiceImpl service;

    private final List<AuditEvent> events = new ArrayList<>();

    private RepositoryConfiguration broken;
    private RepositoryConfiguration valid;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        deploymentRepositoryService = mock(SecureDeploymentRepositoryService.class);
        deploymentManager = mock(DeploymentManager.class);
        deploymentManager.repositoryFactoryProxy = mock(RepositoryFactoryProxy.class);
        projectVersionCacheManager = mock(ProjectVersionCacheManager.class);
        service = new DeploymentServiceImpl(mock(ProjectDependencyResolver.class),
                deploymentRepositoryService,
                deploymentManager,
                (ObjectProvider<UserWorkspace>) mock(ObjectProvider.class),
                mock(ProjectStateValidator.class),
                mock(AclProjectsHelper.class),
                projectVersionCacheManager,
                new ProjectAuditor(event -> {
                }),
                mock(UserManagementService.class),
                new MockEnvironment());

        broken = config("broken");
        when(deploymentManager.getDeployRepository("broken"))
                .thenThrow(new IllegalStateException("Failed to instantiate a repository: connection refused"));

        valid = config("valid");
        var validRepository = deployRepository("valid");
        when(deploymentManager.getDeployRepository("valid")).thenReturn(validRepository);
        when(deploymentManager.repositoryFactoryProxy.getBasePath("valid")).thenReturn("deploy/");
    }

    private static RepositoryConfiguration config(String id) {
        var config = mock(RepositoryConfiguration.class);
        when(config.getId()).thenReturn(id);
        return config;
    }

    /** A non-folder repository holding one deployment with one project archive. */
    private static Repository deployRepository(String id) {
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn(id);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setVersions(false).build());
        var project = new FileData();
        project.setName("deploy/rules#1/project");
        try {
            when(repository.list("deploy/")).thenReturn(List.of(project));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return repository;
    }

    @Test
    void a_broken_repository_is_skipped_and_the_valid_ones_still_answer() {
        when(deploymentRepositoryService.getRepositories()).thenReturn(List.of(broken, valid));

        var deployments = service.getDeployments(DeploymentCriteriaQuery.builder().build());

        assertEquals(1, deployments.size());
        assertEquals("rules", deployments.getFirst().getDeploymentName());
    }

    @Test
    void asking_the_broken_repository_directly_answers_an_empty_list_instead_of_failing() {
        when(deploymentRepositoryService.getRepository("broken")).thenReturn(Optional.of(broken));

        var deployments = service.getDeployments(DeploymentCriteriaQuery.builder().repository("broken").build());

        assertTrue(deployments.isEmpty());
    }

    @Test
    void the_design_revision_of_a_deployed_project_is_the_one_matching_its_content() throws IOException {
        var deployedProject = mock(AProject.class);
        var designVersion = new CachedProjectVersion("design-revision-1", new Date(1_721_000_000_000L), "john");
        when(projectVersionCacheManager.getDesignVersionOfDeployedProject(deployedProject)).thenReturn(designVersion);

        assertEquals(Optional.of(designVersion), service.findDesignRevision(deployedProject));
    }

    @Test
    void no_design_revision_is_reported_when_the_version_cache_cannot_be_read() throws IOException {
        var deployedProject = mock(AProject.class);
        when(deployedProject.getName()).thenReturn("Alpha");
        when(projectVersionCacheManager.getDesignVersionOfDeployedProject(deployedProject))
                .thenThrow(new IOException("cache is unreadable"));

        assertTrue(service.findDesignRevision(deployedProject).isEmpty());
    }

    @Test
    void no_design_revision_is_reported_when_the_deployed_project_has_no_readable_version() throws IOException {
        var deployedProject = mock(AProject.class);
        when(deployedProject.getName()).thenReturn("Alpha");
        // A deployed project with no file data has no version to hash: it must cost its revision, not the listing.
        when(projectVersionCacheManager.getDesignVersionOfDeployedProject(deployedProject))
                .thenThrow(new NullPointerException());

        assertTrue(service.findDesignRevision(deployedProject).isEmpty());
    }

    @Test
    void a_deployment_is_recorded_in_the_audit_log() throws Exception {
        deployingService(RepositoryType.DB, mock(UserManagementService.class))
                .deploy(new DeploymentId("production", "Pricing Rules"), deployableProject(), "Release 7");

        verify(deploymentManager).deploy(any(DeploymentRequest.class));
        var event = events.getFirst();
        assertEquals(AuditAction.PROJECT_DEPLOYED, event.action());
        assertEquals("Pricing Rules", event.target());
        assertEquals(Map.of("repository", "production", "project", "Pricing", "projectRepository", "design",
                "comment", "Release 7"), event.details());
    }

    /** EPBDS-16641: a project that cannot be read stops the deployment with a reason, and nothing is recorded. */
    @Test
    void an_unreadable_project_refuses_the_deployment() throws Exception {
        doThrow(new DeploymentException("Failed to deploy",
                new UnreadableProjectException("P16638", null))).when(deploymentManager).deploy(any(DeploymentRequest.class));
        var deploying = deployingService(RepositoryType.DB, mock(UserManagementService.class));
        var deploymentId = new DeploymentId("production", "Pricing Rules");
        var project = deployableProject();

        var refusal = assertThrows(ConflictException.class, () -> deploying.deploy(deploymentId, project, "R"));

        assertEquals("openl.error.409.project.deploy.unreadable.message", refusal.getErrorCode());
        assertArrayEquals(new Object[]{"P16638"}, refusal.getArgs());
        assertTrue(events.isEmpty());
    }

    /**
     * EPBDS-17258: only a Git repository asks for the email and the display name, which its commits record.
     */
    @ParameterizedTest
    @MethodSource
    void a_user_deploys_when_the_repository_needs_no_more_than_the_profile_holds(RepositoryType type,
                                                                                 UserManagementService users)
            throws Exception {
        deployingService(type, users)
                .deploy(new DeploymentId("production", "Pricing Rules"), deployableProject(), "Release 7");

        verify(deploymentManager).deploy(any(DeploymentRequest.class));
    }

    static Stream<Arguments> a_user_deploys_when_the_repository_needs_no_more_than_the_profile_holds() {
        return Stream.of(Arguments.of(RepositoryType.DB, usersWith(null, null)),
                Arguments.of(RepositoryType.AWS_S3, usersWith("", " ")),
                Arguments.of(RepositoryType.GIT, usersWith("jdoe@example.com", "John Doe")));
    }

    @ParameterizedTest
    @MethodSource
    void a_git_repository_refuses_a_user_without_email_or_display_name(UserManagementService users)
            throws Exception {
        var deploying = deployingService(RepositoryType.GIT, users);
        var project = deployableProject();
        var deploymentId = new DeploymentId("production", "Pricing Rules");

        var refusal = assertThrows(BadRequestException.class, () -> deploying.deploy(deploymentId, project, "R"));

        assertEquals("openl.error.400.commit-info.required.message", refusal.getErrorCode());
        verify(deploymentManager, never()).deploy(any(DeploymentRequest.class));
    }

    static Stream<UserManagementService> a_git_repository_refuses_a_user_without_email_or_display_name() {
        return Stream.of(usersWith("", " "),
                usersWith("jdoe@example.com", null),
                usersWith(null, "John Doe"),
                mock(UserManagementService.class));
    }

    /** The user {@code jdoe} with the given email and display name. */
    private static UserManagementService usersWith(String email, String displayName) {
        var user = mock(User.class);
        when(user.getEmail()).thenReturn(email);
        when(user.getDisplayName()).thenReturn(displayName);
        var users = mock(UserManagementService.class);
        when(users.getUser("jdoe")).thenReturn(user);
        return users;
    }

    /**
     * A service that lets {@code jdoe} deploy every project to {@code production}, a repository of the given type,
     * recording the audit events into {@link #events}.
     */
    @SuppressWarnings("unchecked")
    private DeploymentServiceImpl deployingService(RepositoryType type, UserManagementService users) {
        var stateValidator = mock(ProjectStateValidator.class);
        when(stateValidator.canDeploy(any(RulesProject.class))).thenReturn(true);
        var aclProjectsHelper = mock(AclProjectsHelper.class);
        when(aclProjectsHelper.hasCreateDeploymentPermission("production")).thenReturn(true);
        when(aclProjectsHelper.hasPermission(any(DeploymentRequest.class), eq(BasePermission.WRITE))).thenReturn(true);
        var user = mock(WorkspaceUser.class);
        when(user.getUserName()).thenReturn("jdoe");
        var workspace = mock(UserWorkspace.class);
        when(workspace.getUser()).thenReturn(user);
        var workspaceProvider = (ObjectProvider<UserWorkspace>) mock(ObjectProvider.class);
        when(workspaceProvider.getObject()).thenReturn(workspace);
        return new DeploymentServiceImpl(mock(ProjectDependencyResolver.class),
                deploymentRepositoryService,
                deploymentManager,
                workspaceProvider,
                stateValidator,
                aclProjectsHelper,
                projectVersionCacheManager,
                new ProjectAuditor(event -> events.add((AuditEvent) event)),
                users,
                new MockEnvironment().withProperty("repository.production.factory", type.getFactoryId()));
    }

    /** The project {@code Pricing} of the design repository {@code design}. */
    private static RulesProject deployableProject() {
        var project = mock(RulesProject.class);
        var design = mock(Repository.class);
        when(design.getId()).thenReturn("design");
        when(design.supports()).thenReturn(new FeaturesBuilder(design).build());
        when(project.getRepository()).thenReturn(design);
        when(project.getDesignRepository()).thenReturn(design);
        when(project.getBusinessName()).thenReturn("Pricing");
        return project;
    }
}
