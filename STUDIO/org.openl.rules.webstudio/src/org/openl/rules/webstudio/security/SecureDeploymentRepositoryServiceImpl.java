package org.openl.rules.webstudio.security;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.env.PropertyResolver;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Component;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

@Component
public class SecureDeploymentRepositoryServiceImpl implements SecureDeploymentRepositoryService {

    private final DeploymentManager deploymentManager;
    private final PropertyResolver propertyResolver;
    private final SimpleRepositoryAclService productionRepositoryAclService;

    public SecureDeploymentRepositoryServiceImpl(DeploymentManager deploymentManager,
                                                 PropertyResolver propertyResolver,
                                                 RepositoryAclServiceProvider aclServiceProvider) {
        this.deploymentManager = deploymentManager;
        this.propertyResolver = propertyResolver;
        this.productionRepositoryAclService = aclServiceProvider.getProdRepoAclService();
    }

    @Override
    public List<RepositoryConfiguration> getReadableRepositories() {
        return getRepositories(AclPermission.READ)
                .collect(Collectors.toList());
    }

    @Override
    public List<RepositoryConfiguration> getManageableRepositories() {
        return getRepositories(AclPermission.ADMINISTRATION)
                .collect(Collectors.toList());
    }

    private Stream<RepositoryConfiguration> getRepositories(Permission permission) {
        return deploymentManager.getRepositoryConfigNames().stream()
                .map(configName -> new RepositoryConfiguration(configName, propertyResolver))
                .filter(config -> productionRepositoryAclService.isGranted(config.getId(),
                        null,
                        List.of(permission)))
                .sorted(RepositoryConfiguration.COMPARATOR);
    }

}
