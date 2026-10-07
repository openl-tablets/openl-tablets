package org.openl.studio.deployment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.rules.webstudio.web.repository.cache.CachedProjectVersion;
import org.openl.rules.webstudio.web.repository.cache.ProjectVersionCacheManager;
import org.openl.rules.workspace.uw.UserWorkspace;
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
                projectVersionCacheManager);

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
}
