package org.openl.security.oauth2;

import java.util.Map;
import java.util.Objects;

import org.openl.rules.security.SimpleUser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Extends OidcUserService to create a SimpleUser based on OidcUser.
 *
 * @author Eugene Biruk
 */
public class OpenLOAuth2UserService extends OidcUserService {

    private final PropertyResolver propertyResolver;
    private final Converter<Map<String, Object>, SimpleUser> userInfoClaimsConverter;

    public OpenLOAuth2UserService(PropertyResolver propertyResolver,
                                  Converter<Map<String, Object>, SimpleUser> userInfoClaimsConverter) {
        this.propertyResolver = propertyResolver;
        this.userInfoClaimsConverter = userInfoClaimsConverter;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        Map<String, Object> claims = super.loadUser(userRequest).getClaims();
        var userInfo = Objects.requireNonNull(userInfoClaimsConverter.convert(claims));

        return new DefaultOidcUser(userInfo.getAuthorities(),
            userRequest.getIdToken(),
            propertyResolver.getProperty("security.oauth2.attribute.username"));
    }
}
