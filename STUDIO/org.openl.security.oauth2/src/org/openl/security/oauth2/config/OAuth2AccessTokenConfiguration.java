package org.openl.security.oauth2.config;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

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
    public BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter(
            AuthenticationManager authenticationManager) {

        return new BearerTokenAuthenticationFilter(authenticationManager);
    }

    @Bean
    public UserInfoClaimsConverter userInfoClaimsConverter(@Qualifier("environment") PropertyResolver propertyResolver,
                                                           @Qualifier("syncUserData") Consumer<SimpleUser> syncUserData,
                                                           @Qualifier("privilegeMapper") BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper) {
        return new UserInfoClaimsConverter(propertyResolver, syncUserData, privilegeMapper);
    }

}
