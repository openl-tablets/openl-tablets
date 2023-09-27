package org.openl.security.oauth2.config;

import java.util.Collection;
import java.util.function.BiFunction;

import org.openl.rules.security.Privilege;
import org.openl.security.oauth2.OpenLJwtGrantedAuthoritiesConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Configuration for OAuth2 JWT access token authentication.
 */
@Configuration
public class OAuth2JwtAccessTokenConfiguration {

    @Bean(OAuth2AccessTokenConfiguration.AUTH_PROVIDER_BEAN_NAME)
    public AuthenticationProvider oauth2AccessTokenAuthenticationProvider(JwtDecoder decoder,
            JwtAuthenticationConverter jwtAuthenticationConverter) {

        var provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
        return provider;
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${security.oauth2.issuer-uri}") String issuerUri,
            ClientRegistrationRepository clientRegistrationRepository) {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(),
            new OidcIdTokenValidator(clientRegistrationRepository.findByRegistrationId("webstudio")));

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            OpenLJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter) {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return converter;
    }

    @Bean
    public OpenLJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter(
            @Qualifier("privilegeMapper") BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper) {
        return new OpenLJwtGrantedAuthoritiesConverter(new JwtGrantedAuthoritiesConverter(), privilegeMapper);
    }

}
