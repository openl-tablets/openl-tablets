package org.openl.security.oauth2;

import java.time.Instant;
import java.util.Collection;
import java.util.function.BiFunction;

import org.openl.rules.security.Privilege;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenAuthenticationConverter;

/**
 * Converter for {@link OAuth2AccessToken} to {@link Authentication} with OpenL {@link Privilege}s.
 */
public class OpenLOpaqueTokenAuthenticationConverter implements OpaqueTokenAuthenticationConverter {

    private final BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper;

    public OpenLOpaqueTokenAuthenticationConverter(BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper) {
        this.privilegeMapper = privilegeMapper;
    }

    @Override
    public Authentication convert(String introspectedToken, OAuth2AuthenticatedPrincipal principal) {
        Instant iat = principal.getAttribute(OAuth2TokenIntrospectionClaimNames.IAT);
        Instant exp = principal.getAttribute(OAuth2TokenIntrospectionClaimNames.EXP);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
            introspectedToken,
            iat,
            exp);
        var privileges = privilegeMapper.apply(principal.getName(), principal.getAuthorities());
        return new BearerTokenAuthentication(principal, accessToken, privileges);
    }
}
