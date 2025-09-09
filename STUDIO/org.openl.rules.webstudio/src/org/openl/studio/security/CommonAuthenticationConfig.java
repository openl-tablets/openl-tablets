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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import org.openl.rules.webstudio.security.OpenLAuthenticationProviderWrapper;

@Configuration
@ConditionalOnExpression("'${user.mode}' != 'single'")
public class CommonAuthenticationConfig {

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public SecurityContextPersistenceFilter securityContextPersistenceFilter() {
        return new SecurityContextPersistenceFilter();
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

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public ExceptionTranslationFilter exceptionTranslationFilter(
            @Qualifier("authenticationEntryPoint") AuthenticationEntryPoint authenticationEntryPoint,
            HttpSessionRequestCache httpSessionRequestCache) {
        return new ExceptionTranslationFilter(authenticationEntryPoint, httpSessionRequestCache);
    }

    @Bean
    public HttpSessionRequestCache httpSessionRequestCache() {
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        // Don't redirect to these pages after login
        var excludingRequestMatcher = RequestMatchers.not(RequestMatchers.anyOf(
                "/rest/**",
                "/web/**"
        ));
        cache.setRequestMatcher(excludingRequestMatcher);
        return cache;
    }

    // ======================== Logout ==========================

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public LogoutFilter logoutFilter(
            List<LogoutHandler> logoutHandlers,
            @Qualifier("logoutUrl") String logoutUrl) {
        var filter = new LogoutFilter("/", logoutHandlers.toArray(new LogoutHandler[0]));
        filter.setFilterProcessesUrl(logoutUrl);
        return filter;
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry sessionRegistry) {
        return new RegisterSessionAuthenticationStrategy(sessionRegistry);
    }

    @Bean(initMethod = "afterPropertiesSet")
    public LoginUrlAuthenticationEntryPoint authenticationEntryPoint(@Qualifier("loginUrl") String loginUrl) {
        return new LoginUrlAuthenticationEntryPoint(loginUrl);
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
