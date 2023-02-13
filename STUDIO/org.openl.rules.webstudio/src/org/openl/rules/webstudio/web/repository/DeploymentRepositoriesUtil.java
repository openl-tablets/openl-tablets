package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.springframework.core.env.PropertyResolver;

class DeploymentRepositoriesUtil {
    private DeploymentRepositoriesUtil() {
    }

    static Collection<RepositoryConfiguration> getRepositories(DeploymentManager deploymentManager,
            PropertyResolver propertyResolver,
            SimpleRepositoryAclService productionRepositoryAclService) {
        List<RepositoryConfiguration> repos = new ArrayList<>();
        Collection<String> repositoryConfigNames = deploymentManager.getRepositoryConfigNames();
        for (String configName : repositoryConfigNames) {
            RepositoryConfiguration config = new RepositoryConfiguration(configName, propertyResolver);
            if (productionRepositoryAclService.isGranted(config.getId(), null, List.of(AclPermission.EDIT))) {
                repos.add(config);
            }
        }
        repos.sort(RepositoryConfiguration.COMPARATOR);
        return repos;
    }
}
