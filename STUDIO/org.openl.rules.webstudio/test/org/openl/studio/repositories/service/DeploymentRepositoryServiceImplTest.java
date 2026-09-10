package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

/**
 * Whether the user may deploy anywhere is asked while listing projects, so a broken production
 * repository must read as "not deployable" without hiding the repositories that still work.
 */
class DeploymentRepositoryServiceImplTest {

    private SecureDeploymentRepositoryService deploymentRepositoryService;
    private DeploymentManager deploymentManager;
    private DeploymentRepositoryServiceImpl service;

    @BeforeEach
    void setUp() {
        deploymentRepositoryService = mock(SecureDeploymentRepositoryService.class);
        deploymentManager = mock(DeploymentManager.class);
        var aclServiceProvider = mock(RepositoryAclServiceProvider.class);
        var prodAclService = mock(SimpleRepositoryAclService.class);
        when(aclServiceProvider.getProdRepoAclService()).thenReturn(prodAclService);
        when(prodAclService.isGranted(any(), any(), any())).thenReturn(true);
        service = new DeploymentRepositoryServiceImpl(deploymentRepositoryService,
                mock(RepositoryAccessService.class),
                aclServiceProvider,
                deploymentManager);
    }

    private RepositoryConfiguration config(String id, Repository repository) {
        var config = mock(RepositoryConfiguration.class);
        when(config.getId()).thenReturn(id);
        when(config.getConfigName()).thenReturn(id);
        if (repository != null) {
            when(deploymentManager.getDeployRepository(id)).thenReturn(repository);
        } else {
            when(deploymentManager.getDeployRepository(id))
                    .thenThrow(new IllegalStateException("Failed to instantiate a repository: connection refused"));
        }
        return config;
    }

    private static Repository plainRepository() {
        var repository = mock(Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setVersions(false).build());
        return repository;
    }

    /**
     * Offers the repositories the way the secured service does: one at a time, until one accepts.
     */
    private void repositories(RepositoryConfiguration... configs) {
        var offered = List.of(configs);
        when(deploymentRepositoryService.getRepositories()).thenReturn(offered);
        when(deploymentRepositoryService.anyRepository(any())).thenAnswer(invocation -> {
            Predicate<RepositoryConfiguration> accepted = invocation.getArgument(0);
            return offered.stream().anyMatch(accepted);
        });
    }

    @Test
    void a_broken_repository_does_not_hide_a_deployable_one_behind_it() {
        repositories(config("broken", null), config("valid", plainRepository()));

        assertTrue(service.canDeployToAnyRepository());
    }

    @Test
    void nothing_is_deployable_when_every_repository_is_broken() {
        repositories(config("broken", null));

        assertFalse(service.canDeployToAnyRepository());
    }

    @Test
    void the_question_is_settled_by_the_first_repository_that_answers_it() {
        repositories(config("first", plainRepository()), config("second", plainRepository()));

        assertTrue(service.canDeployToAnyRepository());

        // Reading a repository's configuration is not free, so the ones behind the answer are left alone.
        verify(deploymentManager, never()).getDeployRepository("second");
    }
}
