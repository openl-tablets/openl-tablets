package org.openl.security.oauth2.config;

import java.util.Collection;
import java.util.function.BiFunction;

import org.openl.rules.security.Privilege;
import org.openl.security.oauth2.OpenLOpaqueTokenAuthenticationConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;

/**
 * Configuration for OAuth2 opaque access token authentication.
 */
@Configuration
public class OAuth2OpaqueAccessTokenConfiguration {

    @Bean(OAuth2AccessTokenConfiguration.AUTH_PROVIDER_BEAN_NAME)
    public AuthenticationProvider oauth2AccessTokenAuthenticationProvider(OpaqueTokenIntrospector introspector,
            OpaqueTokenAuthenticationConverter authConverter) {

        var provider = new OpaqueTokenAuthenticationProvider(introspector);
        provider.setAuthenticationConverter(authConverter);
        return provider;
    }

    @Bean
    public OpenLOpaqueTokenAuthenticationConverter openLOpaqueTokenAuthenticationConverter(
            @Qualifier("privilegeMapper") BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper) {
        return new OpenLOpaqueTokenAuthenticationConverter(privilegeMapper);
    }

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector(
            @Value("${security.oauth2.introspection-uri}") String introspectionUri,
            ClientRegistrationRepository clientRegistrationRepository) {

        var clientRegistration = clientRegistrationRepository.findByRegistrationId("webstudio");
        return new SpringOpaqueTokenIntrospector(introspectionUri,
            clientRegistration.getClientId(),
            clientRegistration.getClientSecret());
    }

}
