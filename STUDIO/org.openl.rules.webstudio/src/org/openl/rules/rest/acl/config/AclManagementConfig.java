package org.openl.rules.rest.acl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.rules.rest.acl.resolver.AclRepositoryIdConverter;
import org.openl.rules.rest.acl.resolver.AlcSidValueArgumentResolver;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.rest.acl.service.AclProjectsHelperImpl;
import org.openl.rules.rest.acl.service.AclRepositoriesHelper;
import org.openl.rules.rest.acl.service.AclRepositoriesHelperImpl;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;

@Configuration
public class AclManagementConfig {

    @Bean
    public AclRepositoriesHelper aclRepositoriesHelper(RepositoryAclServiceProvider aclServiceProvider) {
        return new AclRepositoriesHelperImpl(aclServiceProvider);
    }

    @Bean
    public AclProjectsHelper aclProjectsHelper(RepositoryAclServiceProvider aclServiceProvider,
                                               @Value("${security.allow-project-create-delete}") boolean allowProjectCreateDelete) {
        return new AclProjectsHelperImpl(aclServiceProvider, allowProjectCreateDelete);
    }

    @Bean
    public AclRepositoryIdConverter aclRepositoryIdConverter(SecureDesignTimeRepository designTimeRepository,
                                                             DeploymentManager deploymentManager) {
        return new AclRepositoryIdConverter(designTimeRepository, deploymentManager);
    }

    @Bean
    public AlcSidValueArgumentResolver alcSidValueArgumentResolver() {
        return new AlcSidValueArgumentResolver();
    }

}