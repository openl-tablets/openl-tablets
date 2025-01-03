package org.openl.security.acl.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.security.acl.repository.RepositoryAclServiceProviderImpl;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

/**
 * Main configuration for ACL services.
 */
@Configuration
@Import(AclImportSelector.class)
public class OpenLAclConfiguration {

    @Bean
    public RepositoryAclServiceProvider repositoryAclServiceProvider(
            @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService,
            @Qualifier("productionRepositoryAclService") SimpleRepositoryAclService productionRepositoryAclService) {
        return new RepositoryAclServiceProviderImpl(designRepositoryAclService,
                productionRepositoryAclService);
    }

}
