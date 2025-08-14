package org.openl.studio.security;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.access.DefaultWebInvocationPrivilegeEvaluator;
import org.springframework.security.web.access.intercept.DefaultFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.util.matcher.RequestMatcher;

import org.openl.rules.security.AccessVoter;
import org.openl.rules.security.config.MethodSecurityConfig;

@Configuration
@Import(MethodSecurityConfig.class)
@ConditionalOnExpression("'${user.mode}' != 'single'")
public class AuthorizationConfig {

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public FilterSecurityInterceptor filterSecurityInterceptor(@Qualifier("authenticationManager") AuthenticationManager authenticationManager,
                                                               @Qualifier("accessDecisionManager") AccessDecisionManager accessDecisionManager) {
        
        var interceptor = new FilterSecurityInterceptor();
        interceptor.setAuthenticationManager(authenticationManager);
        interceptor.setAccessDecisionManager(accessDecisionManager);
        
        // Create security metadata source with intercept URLs
        // Note the order that entries are placed against the objectDefinitionSource is critical.
        // The FilterSecurityInterceptor will work from the top of the list down to the FIRST pattern that matches the request URL.
        // Accordingly, you should place MOST SPECIFIC (ie a/b/c/d.*) expressions first, with LEAST SPECIFIC (ie a/.*) expressions last
        var requestMap = new LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>>();
        
        requestMap.put(RequestMatchers.matcher("/web/acl/repo/**"), SecurityConfig.createList("ADMIN"));
        requestMap.put(RequestMatchers.matcher("/**"), SecurityConfig.createList("IS_AUTHENTICATED_REMEMBERED"));
        
        var metadataSource = new DefaultFilterInvocationSecurityMetadataSource(requestMap);
        interceptor.setSecurityMetadataSource(metadataSource);
        return interceptor;
    }

    @Bean
    public DefaultWebInvocationPrivilegeEvaluator webPrivilegeEvaluator(@Qualifier("filterSecurityInterceptor") FilterSecurityInterceptor filterSecurityInterceptor) {
        return new DefaultWebInvocationPrivilegeEvaluator(filterSecurityInterceptor);
    }

    // ======================== Roles ==========================

    @Bean
    public AccessVoter roleVoter() {
        return new AccessVoter();
    }

    @Bean
    public AccessDecisionManager accessDecisionManager(@Qualifier("roleVoter") AccessVoter roleVoter) {
        var manager = new AffirmativeBased(List.of(roleVoter));
        manager.setAllowIfAllAbstainDecisions(false);
        return manager;
    }

}
