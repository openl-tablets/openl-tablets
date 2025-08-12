package org.openl.security.oauth2.config;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;
import org.openl.security.oauth2.UserInfoClaimsConverter;

/**
 * Configuration for OAuth2 access token authentication.
 */
@Configuration
@Import(OAuth2ImportSelector.class)
public class OAuth2AccessTokenConfiguration {

    /**
     * Bean name of {@link org.springframework.security.authentication.AuthenticationProvider} for OAuth2 access token
     * authentication.
     */
    public static final String AUTH_PROVIDER_BEAN_NAME = "oauth2AccessTokenAuthenticationProvider";

    @Bean
    public BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter(AuthenticationManager authenticationManager,
                                                                           BearerTokenAuthenticationEntryPoint authenticationEntryPoint) {

        var authFilter = new BearerTokenAuthenticationFilter(authenticationManager);
        authFilter.setAuthenticationEntryPoint(authenticationEntryPoint);
        authFilter.setAuthenticationFailureHandler((request, response, exception) -> {
            if (exception instanceof AuthenticationServiceException) {
                // If the exception is an instance of AuthenticationServiceException, it means that the issue on the
                // server side. In this case, we should return 500 status code.
                // CAUTION: do not re-thrown the exception, because it will be redirected to HTML error page which is not accepted for REST API
                var log = LoggerFactory.getLogger(BearerTokenAuthenticationFilter.class);
                log.error(exception.getMessage(), exception);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                authenticationEntryPoint.commence(request, response, exception);
            }
        });
        return authFilter;
    }

    @Bean
    public BearerTokenAuthenticationEntryPoint bearerTokenAuthenticationEntryPoint() {
        var entrypoint = new BearerTokenAuthenticationEntryPoint();
        entrypoint.setRealmName("OpenL Studio Realm");
        return entrypoint;
    }

    @Bean
    public ExceptionTranslationFilter bearerExceptionTranslationFilter(BearerTokenAuthenticationEntryPoint authenticationEntryPoint) {
        return new ExceptionTranslationFilter(authenticationEntryPoint, new NullRequestCache());
    }

    @Bean
    public UserInfoClaimsConverter userInfoClaimsConverter(@Qualifier("environment") PropertyResolver propertyResolver,
                                                           @Qualifier("syncUserData") Consumer<SimpleUser> syncUserData,
                                                           @Qualifier("privilegeMapper") BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper) {
        return new UserInfoClaimsConverter(propertyResolver, syncUserData, privilegeMapper);
    }

}
