package org.openl.rules.webstudio.security;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Component;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

@Component("secureDeploymentRepositoryService")
public class SecureDeploymentRepositoryServiceImpl implements SecureDeploymentRepositoryService {

    private final DeploymentManager deploymentManager;
    private final SimpleRepositoryAclService productionRepositoryAclService;
    private final Function<String, RepositoryConfiguration> repositoryConfigurationFactory;

    @Autowired
    public SecureDeploymentRepositoryServiceImpl(DeploymentManager deploymentManager,
                                                 PropertyResolver propertyResolver,
                                                 RepositoryAclServiceProvider aclServiceProvider) {
        this(deploymentManager,
                aclServiceProvider.getProdRepoAclService(),
                configName -> new RepositoryConfiguration(configName, propertyResolver));
    }

    // For tests
    SecureDeploymentRepositoryServiceImpl(DeploymentManager deploymentManager,
                                          SimpleRepositoryAclService productionRepositoryAclService,
                                          Function<String, RepositoryConfiguration> repositoryConfigurationFactory) {
        this.deploymentManager = deploymentManager;
        this.productionRepositoryAclService = productionRepositoryAclService;
        this.repositoryConfigurationFactory = repositoryConfigurationFactory;
    }

    @Override
    public List<RepositoryConfiguration> getRepositories() {
        return getRepositories(AclPermission.READ)
                .sorted(RepositoryConfiguration.COMPARATOR)
                .collect(Collectors.toList());
    }

    @Override
    public List<RepositoryConfiguration> getManageableRepositories() {
        return getRepositories(AclPermission.ADMINISTRATION)
                .sorted(RepositoryConfiguration.COMPARATOR)
                .collect(Collectors.toList());
    }

    private Stream<RepositoryConfiguration> getRepositories(Permission permission) {
        return deploymentManager.getRepositoryConfigNames().stream()
                .map(repositoryConfigurationFactory)
                .filter(config -> productionRepositoryAclService.isGranted(config.getId(),
                        null,
                        List.of(permission)));
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return getRepositories(permission).findAny().isPresent();
    }
}
