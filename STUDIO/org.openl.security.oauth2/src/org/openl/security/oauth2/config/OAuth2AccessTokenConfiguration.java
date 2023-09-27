package org.openl.security.oauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

/**
 * Configuration for OAuth2 access token authentication.
 */
@Configuration
@Import(OAuth2ImportSelector.class)
public class OAuth2AccessTokenConfiguration {

    /**
     * Bean name of {@link org.springframework.security.authentication.AuthenticationProvider} for OAuth2 access token authentication.
     */
    public static final String AUTH_PROVIDER_BEAN_NAME = "oauth2AccessTokenAuthenticationProvider";

    @Bean
    public BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter(AuthenticationManager authenticationManager) {
        return new BearerTokenAuthenticationFilter(authenticationManager);
    }

}
