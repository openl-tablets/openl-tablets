package org.openl.studio.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessEventPublishingLogoutHandler;
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
     * Finds who signed in to a browser session for an API request, and never saves who a request signed in as.
     *
     * <p>An API request — a REST call, the WebSocket handshake or an MCP call — either belongs to a browser session,
     * whose sign-in the login filters have already saved in it, or carries its own credentials — a bearer token, a
     * personal access token — that hold for that request alone. Saving those would open an HTTP session for every such
     * request.
     */
    @Bean
    public SecurityContextHolderFilter restSecurityContextFilter() {
        return new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository());
    }

    /**
     * Publishes every sign-in and every refused sign-in as an event, which the audit log records.
     *
     * <p>A refusal with no event of its own, such as a rejected SAML response or OAuth2 authorization code, is
     * published as a refusal of bad credentials.
     */
    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher eventPublisher) {
        var publisher = new DefaultAuthenticationEventPublisher(eventPublisher);
        publisher.setDefaultAuthenticationFailureEvent(AuthenticationFailureBadCredentialsEvent.class);
        return publisher;
    }

    @Bean
    public AuthenticationManager authenticationManager(List<AuthenticationProvider> authenticationProviders,
                                                       AuthenticationEventPublisher authenticationEventPublisher) {
        if (authenticationProviders.isEmpty()) {
            throw new IllegalStateException("No AuthenticationProvider is configured");
        }
        List<AuthenticationProvider> wrappedAuthProviders = authenticationProviders.stream()
                .map(OpenLAuthenticationProviderWrapper::new)
                .collect(Collectors.toList());
        var manager = new ProviderManager(wrappedAuthProviders);
        manager.setAuthenticationEventPublisher(authenticationEventPublisher);
        // Needed for SAML. Without credentials it's not possible to make global single sign out
        manager.setEraseCredentialsAfterAuthentication(false);
        return manager;
    }

    /**
     * Publishes every sign-out as an event, which the audit log records.
     *
     * <p>The login form has a publisher of its own; the logout filters of SAML and OAuth2 take this one, first of
     * their handlers.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public LogoutSuccessEventPublishingLogoutHandler logoutEventPublishingHandler() {
        return new LogoutSuccessEventPublishingLogoutHandler();
    }

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public ExceptionTranslationFilter webExceptionTranslationFilter(
            @Qualifier("httpSessionRequestCache") HttpSessionRequestCache httpSessionRequestCache) {
        return new ExceptionTranslationFilter(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), httpSessionRequestCache);
    }

    @Bean
    public HttpSessionRequestCache httpSessionRequestCache() {
        var cache = new HttpSessionRequestCache();
        // Don't redirect to these pages after login
        cache.setRequestMatcher(RequestMatchers.not(RequestMatchers.api()));
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
