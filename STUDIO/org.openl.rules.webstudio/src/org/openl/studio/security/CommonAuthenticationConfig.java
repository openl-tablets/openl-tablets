package org.openl.studio.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import org.openl.studio.security.ad.OpenLAuthenticationProviderWrapper;

@Configuration
@ConditionalOnExpression("'${user.mode}' != 'single'")
public class CommonAuthenticationConfig {

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public SecurityContextPersistenceFilter securityContextPersistenceFilter() {
        return new SecurityContextPersistenceFilter();
    }

    /**
     * Finds who signed in to a browser session for a REST request, and never saves who a request signed in as.
     *
     * <p>A REST request either belongs to a browser session, whose sign-in the login filters have already saved
     * in it, or carries its own credentials — a bearer token, a personal access token — that hold for that
     * request alone. Saving those would open an HTTP session for every such request.
     */
    @Bean
    public SecurityContextHolderFilter restSecurityContextFilter() {
        return new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository());
    }

    @Bean
    public AuthenticationManager authenticationManager(List<AuthenticationProvider> authenticationProviders) {
        if (authenticationProviders.isEmpty()) {
            throw new IllegalStateException("No AuthenticationProvider is configured");
        }
        List<AuthenticationProvider> wrappedAuthProviders = authenticationProviders.stream()
                .map(OpenLAuthenticationProviderWrapper::new)
                .collect(Collectors.toList());
        ProviderManager manager = new ProviderManager(wrappedAuthProviders);
        // Needed for SAML. Without credentials it's not possible to make global single sign out
        manager.setEraseCredentialsAfterAuthentication(false);
        return manager;
    }

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public ExceptionTranslationFilter webExceptionTranslationFilter(
            @Qualifier("httpSessionRequestCache") HttpSessionRequestCache httpSessionRequestCache) {
        return new ExceptionTranslationFilter(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), httpSessionRequestCache);
    }

    @Bean
    public HttpSessionRequestCache httpSessionRequestCache() {
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        // Don't redirect to these pages after login
        cache.setRequestMatcher(RequestMatchers.not(RequestMatchers.matcher("/rest/**")));
        return cache;
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry sessionRegistry) {
        return new RegisterSessionAuthenticationStrategy(sessionRegistry);
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        var successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/");
        successHandler.setTargetUrlParameter("from");
        return successHandler;
    }

    @Bean
    public AuthorizationFilter filterSecurityInterceptor() {
        return new AuthorizationFilter(AuthenticatedAuthorizationManager.authenticated());
    }

}
