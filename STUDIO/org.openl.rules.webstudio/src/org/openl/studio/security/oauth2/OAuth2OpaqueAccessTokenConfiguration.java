package org.openl.studio.security.oauth2;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

/**
 * Configuration for OAuth2 opaque access token authentication.
 */
public class OAuth2OpaqueAccessTokenConfiguration {

    @Bean(OAuth2AccessTokenConfiguration.AUTH_PROVIDER_BEAN_NAME)
    public AuthenticationProvider oauth2AccessTokenAuthenticationProvider(OpaqueTokenIntrospector introspector) {
        return new OpaqueTokenAuthenticationProvider(introspector);
    }

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector(@Qualifier("environment") PropertyResolver propertyResolver,
                                                           OAuth2Configuration oAuth2Configuration,
                                                           ClientRegistrationRepository clientRegistrationRepository,
                                                           UserInfoClaimsConverter userInfoClaimsConverter,
                                                           CacheManager cacheManager) {
        var clientRegistration = clientRegistrationRepository.findByRegistrationId("webstudio");
        return new UserInfoOpaqueTokenIntrospector(oAuth2Configuration.getIntrospectionEndpoint().get(),
                clientRegistration,
                userInfoClaimsConverter,
                propertyResolver,
                cacheManager.getCache("userInfoOAuth2Cache"));
    }

}
