package org.openl.studio.security;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.logout.OpenSaml5LogoutRequestValidator;
import org.springframework.security.saml2.provider.service.metadata.OpenSaml5MetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.saml2.provider.service.web.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml5LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml5LogoutResponseResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestFilter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

import org.openl.rules.security.SimpleUser;
import org.openl.studio.security.saml.OpenLResponseAuthenticationConverter;
import org.openl.studio.security.saml.OpenLSamlBuilder;
import org.openl.studio.security.saml.SamlLogoutSuccessHandler;

@Configuration
@ConditionalOnExpression("'${user.mode}' == 'saml'")
// Scan for auto-wiring classes in spring saml2 packages
@ComponentScan("org.springframework.security.saml2")
public class SamlSecurityConfig {

    // Logout endpoint
    @Bean
    @Order(1)
    public SecurityFilterChain logoutFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            Saml2LogoutRequestFilter logoutFilter) {
        
        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/security_logout"),
                securityContextPersistenceFilter,
                logoutFilter);
    }

    // SAML metadata endpoint
    @Bean
    @Order(2)
    public SecurityFilterChain samlMetadataFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("metadataGeneratorFilter") Saml2MetadataFilter metadataGeneratorFilter) {
        
        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/saml2/service-provider-metadata/**"),
                securityContextPersistenceFilter,
                metadataGeneratorFilter);
    }

    // SAML login endpoint
    @Bean
    @Order(3)
    public SecurityFilterChain samlLoginFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("samlFilter") Saml2WebSsoAuthenticationFilter samlFilter) {
        
        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/login/saml2/**"),
                securityContextPersistenceFilter,
                samlFilter);
    }

    // SAML authenticate endpoint
    @Bean
    @Order(4)
    public SecurityFilterChain samlAuthenticateFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("samlRequestFilter") Saml2WebSsoAuthenticationRequestFilter samlRequestFilter) {
        
        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/saml2/authenticate/**"),
                securityContextPersistenceFilter,
                samlRequestFilter);
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
            Saml2LogoutRequestFilter logoutFilter,
            @Qualifier("exceptionTranslationFilter") ExceptionTranslationFilter exceptionTranslationFilter,
            @Qualifier("filterSecurityInterceptor") AuthorizationFilter filterSecurityInterceptor) {
        
        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/**"),
                securityContextPersistenceFilter,
                logoutFilter,
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
        return "/saml2/authenticate/webstudio";
    }

    // ======================== Logout ==========================

    @Bean
    public String logoutUrl() {
        return "/security_logout";
    }

    @Bean
    public SamlLogoutSuccessHandler samlLogoutHandler(@Qualifier("relyingPartyRegistrationResolver") RelyingPartyRegistrationResolver relyingPartyRegistrationResolver) {
        var logoutRequestResolver = new OpenSaml5LogoutRequestResolver(relyingPartyRegistrationResolver);
        return new SamlLogoutSuccessHandler(logoutRequestResolver);
    }

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public Saml2LogoutRequestFilter samlLogoutFilter(
            @Qualifier("relyingPartyRegistrationResolver") RelyingPartyRegistrationResolver relyingPartyRegistrationResolver,
            SamlLogoutSuccessHandler logoutHandler) {
        
        var logoutRequestValidator = new OpenSaml5LogoutRequestValidator();
        var logoutResponseResolver = new OpenSaml5LogoutResponseResolver(relyingPartyRegistrationResolver);
        
        return new Saml2LogoutRequestFilter(relyingPartyRegistrationResolver, logoutRequestValidator, 
                                          logoutResponseResolver, logoutHandler);
    }

    // ============================== SAML ====================================================================

    @Bean
    public OpenLSamlBuilder openLSamlBuilder(Environment environment) {
        return new OpenLSamlBuilder(environment);
    }

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistration(@Qualifier("openLSamlBuilder") OpenLSamlBuilder openLSamlBuilder) {
        return openLSamlBuilder.relyingPartyRegistration();
    }

    @Bean
    public Saml2AuthenticationRequestResolver authenticationRequestContextResolver(@Qualifier("openLSamlBuilder") OpenLSamlBuilder openLSamlBuilder) {
        return openLSamlBuilder.authenticationRequestContextResolver();
    }

    @Bean
    public RelyingPartyRegistrationResolver relyingPartyRegistrationResolver(@Qualifier("openLSamlBuilder") OpenLSamlBuilder openLSamlBuilder) {
        return openLSamlBuilder.relyingPartyRegistrationResolver();
    }

    @Bean
    public Saml2WebSsoAuthenticationRequestFilter samlRequestFilter(@Qualifier("authenticationRequestContextResolver") Saml2AuthenticationRequestResolver authenticationRequestResolver) {
        return new Saml2WebSsoAuthenticationRequestFilter(authenticationRequestResolver);
    }

    @Bean
    public OpenLResponseAuthenticationConverter responseAuthenticationConverter(
            Environment environment,
            @Qualifier("syncUserData") Consumer<SimpleUser> syncUserData,
            @Qualifier("privilegeMapper") BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> privilegeMapper) {
        
        return new OpenLResponseAuthenticationConverter(environment, syncUserData, privilegeMapper);
    }

    @Bean
    public Saml2WebSsoAuthenticationFilter samlFilter(
            @Qualifier("relyingPartyRegistration") RelyingPartyRegistrationRepository relyingPartyRegistration,
            AuthenticationManager authenticationManager,
            AuthenticationSuccessHandler authenticationSuccessHandler,
            @Qualifier("sessionAuthenticationStrategy") SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        
        var filter = new Saml2WebSsoAuthenticationFilter(relyingPartyRegistration);
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        filter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
        return filter;
    }

    @Bean
    public AuthenticationProvider samlAuthenticationProvider(
            @Qualifier("openLSamlBuilder") OpenLSamlBuilder openLSamlBuilder,
            @Qualifier("responseAuthenticationConverter") OpenLResponseAuthenticationConverter responseAuthenticationConverter) {
        
        var provider = openLSamlBuilder.openSaml5AuthenticationProvider();
        provider.setResponseAuthenticationConverter(responseAuthenticationConverter);
        return provider;
    }

    @Bean
    public Saml2MetadataFilter metadataGeneratorFilter(@Qualifier("relyingPartyRegistrationResolver") RelyingPartyRegistrationResolver relyingPartyRegistrationResolver) {
        var saml2MetadataResolver = new OpenSaml5MetadataResolver();
        return new Saml2MetadataFilter(relyingPartyRegistrationResolver, saml2MetadataResolver);
    }
}
