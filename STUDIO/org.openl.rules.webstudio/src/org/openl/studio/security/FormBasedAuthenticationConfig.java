package org.openl.studio.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@ConditionalOnExpression("'${user.mode}' == 'ad' || '${user.mode}' == 'multi'")
public class FormBasedAuthenticationConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain loginFilterChain(@Qualifier("loginUrl") String loginUrl) {
        return new DefaultSecurityFilterChain(RequestMatchers.matcher(loginUrl));
    }

    // REST endpoints
    @Bean
    @Order(3)
    public SecurityFilterChain restEndpointsFilterChain(
            @Qualifier("restBasicAuthenticationFilter") BasicAuthenticationFilter restBasicAuthenticationFilter,
            @Qualifier("restExceptionTranslationFilter") ExceptionTranslationFilter restExceptionTranslationFilter,
            @Qualifier("filterSecurityInterceptor") FilterSecurityInterceptor filterSecurityInterceptor) {

        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/rest/**"),
                restBasicAuthenticationFilter,
                restExceptionTranslationFilter,
                filterSecurityInterceptor);
    }

    // Web endpoints
    @Bean
    @Order(4)
    public SecurityFilterChain webEndpointsFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("webBasicAuthenticationFilter") BasicAuthenticationFilter webBasicAuthenticationFilter,
            @Qualifier("webExceptionTranslationFilter") ExceptionTranslationFilter webExceptionTranslationFilter,
            @Qualifier("filterSecurityInterceptor") FilterSecurityInterceptor filterSecurityInterceptor) {

        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/web/**"),
                securityContextPersistenceFilter,
                webBasicAuthenticationFilter,
                webExceptionTranslationFilter,
                filterSecurityInterceptor);
    }

    // All other patterns - catch-all
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain defaultFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("logoutFilter") LogoutFilter logoutFilter,
            @Qualifier("usernamePasswordAuthenticationFilter") UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter,
            @Qualifier("basicAuthenticationFilter") BasicAuthenticationFilter basicAuthenticationFilter,
            @Qualifier("exceptionTranslationFilter") ExceptionTranslationFilter exceptionTranslationFilter,
            @Qualifier("filterSecurityInterceptor") FilterSecurityInterceptor filterSecurityInterceptor) {

        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/**"),
                securityContextPersistenceFilter,
                logoutFilter,
                usernamePasswordAuthenticationFilter,
                basicAuthenticationFilter,
                exceptionTranslationFilter,
                filterSecurityInterceptor);
    }

    @Bean
    public String loginUrl() {
        return "/login";
    }

    @Bean
    public String loginCheckUrl() {
        return "/security_login_check";
    }

    @Bean
    public String logoutUrl() {
        return "/security_logout";
    }

    // /web/settings with specific filters
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public SecurityFilterChain webSettingsFilterChain(
            @Qualifier("securityContextPersistenceFilter") SecurityContextPersistenceFilter securityContextPersistenceFilter,
            @Qualifier("anonymousAuthenticationFilter") AnonymousAuthenticationFilter anonymousAuthenticationFilter,
            @Qualifier("webExceptionTranslationFilter") ExceptionTranslationFilter webExceptionTranslationFilter) {

        return new DefaultSecurityFilterChain(RequestMatchers.matcher("/web/settings"),
                securityContextPersistenceFilter,
                anonymousAuthenticationFilter,
                webExceptionTranslationFilter);
    }

    // ======================== AUTHENTICATION =======================

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public AnonymousAuthenticationFilter anonymousAuthenticationFilter() {
        return new AnonymousAuthenticationFilter("anonymousKey");
    }

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public BasicAuthenticationFilter basicAuthenticationFilter(
            AuthenticationManager authenticationManager,
            @Qualifier("basicAuthenticationEntryPoint") BasicAuthenticationEntryPoint basicAuthenticationEntryPoint) {
        return new BasicAuthenticationFilter(authenticationManager, basicAuthenticationEntryPoint);
    }

    @Bean(initMethod = "afterPropertiesSet")
    public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() throws Exception {
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("OpenL Studio Realm");
        return entryPoint;
    }

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public BasicAuthenticationFilter restBasicAuthenticationFilter(
            AuthenticationManager authenticationManager,
            @Qualifier("restBasicAuthenticationEntryPoint") RestBasicAuthenticationEntryPoint restBasicAuthenticationEntryPoint) {
        return new BasicAuthenticationFilter(authenticationManager, restBasicAuthenticationEntryPoint);
    }

    @Bean(initMethod = "afterPropertiesSet")
    public RestBasicAuthenticationEntryPoint restBasicAuthenticationEntryPoint() {
        RestBasicAuthenticationEntryPoint entryPoint = new RestBasicAuthenticationEntryPoint();
        entryPoint.setRealmName("OpenL Studio Realm");
        return entryPoint;
    }

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public ExceptionTranslationFilter restExceptionTranslationFilter(
            @Qualifier("restBasicAuthenticationEntryPoint") RestBasicAuthenticationEntryPoint restBasicAuthenticationEntryPoint) {
        return new ExceptionTranslationFilter(restBasicAuthenticationEntryPoint, new NullRequestCache());
    }

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public BasicAuthenticationFilter webBasicAuthenticationFilter(
            AuthenticationManager authenticationManager) {
        return new BasicAuthenticationFilter(authenticationManager);
    }

    @Bean(initMethod = "afterPropertiesSet", destroyMethod = "destroy")
    public UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter(
            AuthenticationManager authenticationManager,
            @Qualifier("loginCheckUrl") String loginCheckUrl,
            @Qualifier("sessionAuthenticationStrategy") SessionAuthenticationStrategy sessionAuthenticationStrategy,
            AuthenticationSuccessHandler authenticationSuccessHandler) {

        var filter = new UsernamePasswordAuthenticationFilter();
        filter.setAuthenticationManager(authenticationManager);

        filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        filter.setFilterProcessesUrl(loginCheckUrl);
        filter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
        
        return filter;
    }

    @Bean
    public LogoutHandler logoutHandler() {
        return new SecurityContextLogoutHandler();
    }
}
