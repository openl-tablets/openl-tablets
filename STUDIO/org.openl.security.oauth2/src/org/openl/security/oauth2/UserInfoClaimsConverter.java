package org.openl.security.oauth2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.core.GrantedAuthority;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.openl.util.StringUtils;

/**
 * Converts OAuth2 user info claims to {@link SimpleUser}.
 */
public class UserInfoClaimsConverter implements Converter<Map<String, Object>, SimpleUser> {

    private final PropertyResolver propertyResolver;
    private final Consumer<SimpleUser> syncUserData;
    private final BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper;

    public UserInfoClaimsConverter(PropertyResolver propertyResolver,
            Consumer<SimpleUser> syncUserData,
            BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper) {
        this.syncUserData = syncUserData;
        this.propertyResolver = propertyResolver;
        this.privilegeMapper = privilegeMapper;
    }

    @Override
    public SimpleUser convert(Map<String, Object> claims) {
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

        String username = getAttributeAsString(claims, "security.oauth2.attribute.username");
        var userBuilder = SimpleUser.builder()
            .setFirstName(getAttributeAsString(claims, "security.oauth2.attribute.first-name"))
            .setLastName(getAttributeAsString(claims, "security.oauth2.attribute.last-name"))
            .setUsername(username)
            .setPrivileges(grantedAuthorities)
            .setEmail(getAttributeAsString(claims, "security.oauth2.attribute.email"))
            .setDisplayName(getAttributeAsString(claims, "security.oauth2.attribute.display-name"));

        syncUserData.accept(userBuilder.build());
        Collection<Privilege> privileges = privilegeMapper.apply(username, grantedAuthorities);

        return userBuilder.setPrivileges(privileges).build();
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
