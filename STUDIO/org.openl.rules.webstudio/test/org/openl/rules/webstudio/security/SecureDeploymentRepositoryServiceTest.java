package org.openl.rules.webstudio.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

@ExtendWith(MockitoExtension.class)
class SecureDeploymentRepositoryServiceTest {

    @Mock
    private DeploymentManager deploymentManager;

    @Mock
    private SimpleRepositoryAclService productionRepositoryAclService;

    @Mock
    private Function<String, RepositoryConfiguration> repositoryConfigurationFactory;

    private SecureDeploymentRepositoryServiceImpl deploymentRepositoryService;

    @BeforeEach
    void setUp() {
        deploymentRepositoryService = new SecureDeploymentRepositoryServiceImpl(deploymentManager, productionRepositoryAclService, repositoryConfigurationFactory);
    }

    @Test
    void test_getRepositories() {
        doReturn(List.of("prod1", "prod2")).when(deploymentManager).getRepositoryConfigNames();
        var repositoryConfiguration1 = createRepositoryConfiguration("prod1");
        when(repositoryConfigurationFactory.apply("prod1")).thenReturn(repositoryConfiguration1);
        doReturn(createRepositoryConfiguration("prod2")).when(repositoryConfigurationFactory).apply("prod2");
        when(productionRepositoryAclService.isGranted(eq("prod1"), isNull(), eq(List.of(BasePermission.READ)))).thenReturn(true);
        when(productionRepositoryAclService.isGranted(eq("prod2"), isNull(), eq(List.of(BasePermission.READ)))).thenReturn(false);

        var actualRepositories = deploymentRepositoryService.getRepositories();
        assertEquals(1, actualRepositories.size());
        assertSame(repositoryConfiguration1, actualRepositories.getFirst());
    }

    @Test
    void test_getManageableRepositories() {
        doReturn(List.of("prod1", "prod2")).when(deploymentManager).getRepositoryConfigNames();
        var repositoryConfiguration1 = createRepositoryConfiguration("prod1");
        when(repositoryConfigurationFactory.apply("prod1")).thenReturn(repositoryConfiguration1);
        doReturn(createRepositoryConfiguration("prod2")).when(repositoryConfigurationFactory).apply("prod2");
        when(productionRepositoryAclService.isGranted(eq("prod1"), isNull(), eq(List.of(BasePermission.ADMINISTRATION)))).thenReturn(true);
        when(productionRepositoryAclService.isGranted(eq("prod2"), isNull(), eq(List.of(BasePermission.ADMINISTRATION)))).thenReturn(false);

        var actualRepositories = deploymentRepositoryService.getManageableRepositories();
        assertEquals(1, actualRepositories.size());
        assertSame(repositoryConfiguration1, actualRepositories.getFirst());
    }

    @Test
    void test_hasPermission() {
        doReturn(List.of("prod1", "prod2")).when(deploymentManager).getRepositoryConfigNames();
        var repositoryConfiguration1 = createRepositoryConfiguration("prod1");
        when(repositoryConfigurationFactory.apply("prod1")).thenReturn(repositoryConfiguration1);
        when(productionRepositoryAclService.isGranted(eq("prod1"), isNull(), eq(List.of(BasePermission.WRITE)))).thenReturn(true);

        assertTrue(deploymentRepositoryService.hasPermission(BasePermission.WRITE));
    }

    private RepositoryConfiguration createRepositoryConfiguration(String id) {
        var repositoryConfiguration = mock(RepositoryConfiguration.class);
        doReturn(id).when(repositoryConfiguration).getId();
        return repositoryConfiguration;
    }
}
