package org.openl.studio.security;

import java.util.Map;
import jakarta.servlet.Filter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

import org.openl.rules.security.SimpleUser;
import org.openl.studio.security.oauth2.LazyClientRegistrationRepository;
import org.openl.studio.security.oauth2.Oauth2LogoutSuccessHandler;
import org.openl.studio.security.oauth2.OpenLOAuth2UserService;
import org.openl.studio.security.oauth2.OAuth2AccessTokenConfiguration;

@Configuration
@ConditionalOnExpression("'${user.mode}' == 'oauth2'")
@Import(OAuth2AccessTokenConfiguration.class)
// Scan for auto-wiring classes in spring oauth2 packages
@ComponentScan("org.springframework.security.oauth2")
public class OAuth2SecurityConfig {

    // Logout endpoint
    @Bean
    @Order(1)
    public SecurityFilterChain logoutFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("logoutFilter") LogoutFilter logoutFilter) {

        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/logout"),
                securityContextPersistenceFilter,
                logoutFilter);
    }

    // OAuth2 callback endpoint
    @Bean
    @Order(2)
    public SecurityFilterChain oauth2CallbackFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("oauth2Filter") OAuth2LoginAuthenticationFilter oauth2Filter) {

        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/login/oauth2/code/**"),
                securityContextPersistenceFilter,
                oauth2Filter);
    }

    // OAuth2 authorization endpoint
    @Bean
    @Order(3)
    public SecurityFilterChain oauth2AuthorizationFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("oauth2RedirectFilter") OAuth2AuthorizationRequestRedirectFilter oauth2RedirectFilter) {

        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/oauth2/authorization/**"),
                securityContextPersistenceFilter,
                oauth2RedirectFilter);
    }

    // Web endpoints
    @Bean
    @Order(4)
    public SecurityFilterChain restEndpointsFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("bearerTokenAuthenticationFilter") Filter bearerTokenAuthenticationFilter,
            @Qualifier("bearerExceptionTranslationFilter") ExceptionTranslationFilter bearerExceptionTranslationFilter,
            @Qualifier("filterSecurityInterceptor") AuthorizationFilter filterSecurityInterceptor) {

        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/rest/**"),
                securityContextPersistenceFilter,
                bearerTokenAuthenticationFilter,
                bearerExceptionTranslationFilter,
                filterSecurityInterceptor);
    }

    @Bean
    @Order(6)
    public SecurityFilterChain webEndpointsFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("webExceptionTranslationFilter") ExceptionTranslationFilter webExceptionTranslationFilter,
            @Qualifier("filterSecurityInterceptor") AuthorizationFilter filterSecurityInterceptor) {

        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/web/**"),
                securityContextPersistenceFilter,
                webExceptionTranslationFilter,
                filterSecurityInterceptor);
    }

    // All other patterns - catch-all
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain defaultFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("exceptionTranslationFilter") ExceptionTranslationFilter exceptionTranslationFilter,
            @Qualifier("filterSecurityInterceptor") AuthorizationFilter filterSecurityInterceptor) {

        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/**"),
                securityContextPersistenceFilter,
                exceptionTranslationFilter,
                filterSecurityInterceptor);
    }

    // ============================ Define needed beans for dependencies ======================================

    @Bean
    public Boolean canCreateInternalUsers() {
        return Boolean.FALSE;
    }

    @Bean
    public String loginUrl() {
        return "/oauth2/authorization/webstudio";
    }

    // ======================== Logout ========================== 

    @Bean
    public String logoutUrl() {
        return "/logout";
    }

    @Bean
    public LogoutHandler oauth2LogoutSuccessHandler(@Qualifier("registrationRepository") ClientRegistrationRepository registrationRepository) {
        return new Oauth2LogoutSuccessHandler(registrationRepository);
    }

    // ============================== OAuth2 ====================================================================

    @Bean
    @Order(1)
    public AuthenticationProvider oauth2AuthenticationProvider(
            @Qualifier("userInfoClaimsConverter") Converter<Map<String, Object>, SimpleUser> userInfoClaimsConverter,
            Environment environment) {

        var accessTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
        var userService = new OpenLOAuth2UserService(environment, userInfoClaimsConverter);

        return new OidcAuthorizationCodeAuthenticationProvider(accessTokenResponseClient, userService);
    }

    @Bean
    public LazyClientRegistrationRepository registrationRepository(Environment environment) {
        return new LazyClientRegistrationRepository(environment);
    }

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public OAuth2AuthorizationRequestRedirectFilter oauth2RedirectFilter(ClientRegistrationRepository registrationRepository) {
        return new OAuth2AuthorizationRequestRedirectFilter(registrationRepository);
    }

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public OAuth2LoginAuthenticationFilter oauth2Filter(ClientRegistrationRepository registrationRepository,
                                                        AuthenticationManager authenticationManager,
                                                        SessionAuthenticationStrategy sessionAuthenticationStrategy,
                                                        AuthenticationSuccessHandler authenticationSuccessHandler) {

        var authorizedClientService = new InMemoryOAuth2AuthorizedClientService(registrationRepository);
        var filter = new OAuth2LoginAuthenticationFilter(registrationRepository, authorizedClientService);
        filter.setAuthenticationManager(authenticationManager);
        filter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
        filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        return filter;
    }
}
