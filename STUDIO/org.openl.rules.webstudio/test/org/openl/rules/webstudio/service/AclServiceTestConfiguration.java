package org.openl.rules.webstudio.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.openl.rules.webstudio.config.SpringCachingConfig;
import org.openl.security.acl.config.EnabledAclConfiguration;
import org.openl.security.acl.config.OpenLAclConfiguration;
import org.openl.studio.security.MethodSecurityConfig;

@Configuration
@Import({EnabledAclConfiguration.class, OpenLAclConfiguration.class, SpringCachingConfig.class, MethodSecurityConfig.class})
public class AclServiceTestConfiguration {

    @Bean
    public SecuredService securedService() {
        return new SecuredServiceImpl();
    }

}
