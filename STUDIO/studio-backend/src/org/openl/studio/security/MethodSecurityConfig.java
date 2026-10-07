package org.openl.studio.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Configuration class for method security.
 *
 * @author Vladyslav Pikus
 * @see org.springframework.security.config.annotation.method.configuration.PrePostMethodSecurityConfiguration
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    @Bean
    public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        var expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return new RoleHierarchyImpl();
    }

}
