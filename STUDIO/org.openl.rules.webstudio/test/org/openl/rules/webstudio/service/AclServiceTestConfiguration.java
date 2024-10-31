package org.openl.rules.webstudio.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import org.openl.rules.webstudio.config.SpringCachingConfig;
import org.openl.security.acl.config.OpenLAclConfiguration;

@Configuration
@Import({OpenLAclConfiguration.class, SpringCachingConfig.class})
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class AclServiceTestConfiguration {

    @Bean
    public SecuredService securedService() {
        return new SecuredServiceImpl();
    }

}
