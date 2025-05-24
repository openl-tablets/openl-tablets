package org.openl.rules.rest.acl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.rest.acl.service.AclProjectsHelperImpl;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;

@Configuration
public class AclCommonConfig {

    @Bean
    public AclProjectsHelper aclProjectsHelper(RepositoryAclServiceProvider aclServiceProvider,
                                               SecureDeploymentRepositoryService deploymentRepositoryService,
                                               @Value("${security.allow-project-create-delete}") boolean allowProjectCreateDelete) {
        return new AclProjectsHelperImpl(aclServiceProvider, deploymentRepositoryService, allowProjectCreateDelete);
    }

}
