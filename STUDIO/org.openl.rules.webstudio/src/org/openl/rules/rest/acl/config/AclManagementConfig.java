package org.openl.rules.rest.acl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.openl.rules.rest.acl.resolver.AclRepositoryIdConverter;
import org.openl.rules.rest.acl.resolver.AlcSidValueArgumentResolver;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.rest.acl.service.AclProjectsHelperImpl;
import org.openl.rules.rest.acl.service.AclRepositoriesHelper;
import org.openl.rules.rest.acl.service.AclRepositoriesHelperImpl;
import org.openl.rules.rest.acl.service.BulkAclOverwriteService;
import org.openl.rules.rest.acl.service.BulkAclOverwriteServiceImpl;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.repository.DeploymentManager;
import org.openl.security.acl.JdbcMutableAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;

@Configuration
public class AclManagementConfig {

    @Bean
    public AclRepositoriesHelper aclRepositoriesHelper(RepositoryAclServiceProvider aclServiceProvider) {
        return new AclRepositoriesHelperImpl(aclServiceProvider);
    }

    @Bean
    public AclProjectsHelper aclProjectsHelper(RepositoryAclServiceProvider aclServiceProvider,
                                               SecureDeploymentRepositoryService deploymentRepositoryService,
                                               @Value("${security.allow-project-create-delete}") boolean allowProjectCreateDelete) {
        return new AclProjectsHelperImpl(aclServiceProvider, deploymentRepositoryService, allowProjectCreateDelete);
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

    @Bean
    public BulkAclOverwriteService bulkAclOverwriteService(UserManagementService userManagementService,
                                                           GroupManagementService groupManagementService,
                                                           RepositoryAclServiceProvider aclServiceProvider,
                                                           LockRegistry lockRegistry,
                                                           PlatformTransactionManager txManager,
                                                           SecureDesignTimeRepository designTimeRepository,
                                                           SecureDeploymentRepositoryService deploymentRepositoryService,
                                                           JdbcMutableAclService aclService) {
        return new BulkAclOverwriteServiceImpl(
                userManagementService,
                groupManagementService,
                aclServiceProvider,
                lockRegistry,
                new TransactionTemplate(txManager),
                designTimeRepository,
                deploymentRepositoryService,
                aclService
        );
    }

}
