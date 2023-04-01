package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.acls.model.Permission;

class DeploymentRepositoriesUtil {
    private DeploymentRepositoriesUtil() {
    }

    static Collection<RepositoryConfiguration> getRepositories(DeploymentManager deploymentManager,
            PropertyResolver propertyResolver,
            SimpleRepositoryAclService productionRepositoryAclService,
            Permission... permissions) {
        List<RepositoryConfiguration> repos = new ArrayList<>();
        if (permissions != null) {
            Collection<String> repositoryConfigNames = deploymentManager.getRepositoryConfigNames();
            for (String configName : repositoryConfigNames) {
                RepositoryConfiguration config = new RepositoryConfiguration(configName, propertyResolver);
                if (Arrays.stream(permissions)
                    .allMatch(e -> productionRepositoryAclService.isGranted(config.getId(), null, List.of(e)))) {
                    repos.add(config);
                }
            }
            repos.sort(RepositoryConfiguration.COMPARATOR);
        }
        return repos;
    }
}
