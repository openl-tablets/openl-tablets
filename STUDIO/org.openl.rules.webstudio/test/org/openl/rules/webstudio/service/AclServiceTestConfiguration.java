package org.openl.rules.webstudio.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@Configuration
@ImportResource("classpath:META-INF/standalone/spring/security-acl-beans.xml")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class AclServiceTestConfiguration {
    @Bean
    public SecuredService securedService() {
        return new SecuredServiceImpl();
    }

}
