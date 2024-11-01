package org.openl.security.acl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openl.security.acl.repository.DisabledRepositoryAclServiceImpl;
import org.openl.security.acl.repository.DisabledSimpleRepositoryAclServiceImpl;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

/**
 * Configuration for ACL services that are disabled.
 * Imported by {@link AclImportSelector}.
 */
@Configuration
public class DisabledAclConfiguration {

    @Bean
    public RepositoryAclService designRepositoryAclService() {
        return new DisabledRepositoryAclServiceImpl();
    }

    @Bean
    public RepositoryAclService deployConfigRepositoryAclService() {
        return new DisabledRepositoryAclServiceImpl();
    }

    @Bean
    public SimpleRepositoryAclService productionRepositoryAclService() {
        return new DisabledSimpleRepositoryAclServiceImpl();
    }

}
