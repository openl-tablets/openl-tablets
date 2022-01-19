package org.openl.security.oauth2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.core.GrantedAuthority;
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
    private final Consumer<User> syncUserData;
    private final BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper;

    public OpenLOAuth2UserService(PropertyResolver propertyResolver,
            Consumer<User> syncUserData,
            BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper) {
        this.propertyResolver = propertyResolver;
        this.syncUserData = syncUserData;
        this.privilegeMapper = privilegeMapper;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        Map<String, Object> claims = super.loadUser(userRequest).getClaims();
        final List<Privilege> grantedAuthorities = new ArrayList<>();
        Object claimGroups = claims.get(propertyResolver.getProperty("security.oauth2.attribute.groups"));
        if (claimGroups != null) {
            if (List.class.isAssignableFrom(claimGroups.getClass())) {
                List<?> groups = (List<?>) claims.get(propertyResolver.getProperty("security.oauth2.attribute.groups"));
                for (Object name : groups) {
                    grantedAuthorities.add(new SimplePrivilege(name.toString()));
                }
            }
        }
        String firstName = getAttributeAsString(claims, "security.oauth2.attribute.first-name");
        String lastName = getAttributeAsString(claims, "security.oauth2.attribute.last-name");
        String username = getAttributeAsString(claims, "security.oauth2.attribute.username");
        String email = getAttributeAsString(claims, "security.oauth2.attribute.email");
        String displayName = getAttributeAsString(claims, "security.oauth2.attribute.display-name");
        SimpleUser simpleUser = SimpleUser.builder()
            .setFirstName(firstName)
            .setLastName(lastName)
            .setUsername(username)
            .setPrivileges(grantedAuthorities)
            .setEmail(email)
            .setDisplayName(displayName)
            .build();

        syncUserData.accept(simpleUser);
        Collection<Privilege> privileges = privilegeMapper.apply(username, grantedAuthorities);

        return new DefaultOidcUser(privileges,
            userRequest.getIdToken(),
            propertyResolver.getProperty("security.oauth2.attribute.username"));
    }

    private String getAttributeAsString(Map<String, Object> claims, String key) {
        String attribute = null;
        String property = propertyResolver.getProperty(key);
        if (StringUtils.isNotBlank(property)) {
            Object claim = claims.get(property);
            if (claim != null) {
                if (List.class.isAssignableFrom(claims.getClass())) {
                    List<?> stringClaims = (List<?>) claim;
                    if (!stringClaims.isEmpty()) {
                        attribute = stringClaims.get(0).toString();
                    }
                } else {
                    attribute = claim.toString();
                }
            }
        }
        return attribute;
    }
}
