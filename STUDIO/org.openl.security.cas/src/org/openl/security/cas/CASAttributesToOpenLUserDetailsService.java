package org.openl.security.cas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jasig.cas.client.validation.Assertion;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.cas.userdetails.AbstractCasAssertionUserDetailsService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CASAttributesToOpenLUserDetailsService extends AbstractCasAssertionUserDetailsService {
    private final String firstNameAttribute;
    private final String lastNameAttribute;
    private final String groupsAttribute;
    private final BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper;
    private final Consumer<User> syncUserData;
    private final String emailAttribute;
    private final String displayNameAttribute;

    public CASAttributesToOpenLUserDetailsService(PropertyResolver propertyResolver,
            Consumer<User> syncUserData,
            BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper) {
        this.firstNameAttribute = propertyResolver.getProperty("security.cas.attribute.first-name");
        this.lastNameAttribute = propertyResolver.getProperty("security.cas.attribute.last-name");
        this.groupsAttribute = propertyResolver.getProperty("security.cas.attribute.groups");
        this.emailAttribute = propertyResolver.getProperty("security.cas.attribute.email");
        this.displayNameAttribute = propertyResolver.getProperty("security.cas.attribute.display-name");
        this.syncUserData = syncUserData;
        this.privilegeMapper = privilegeMapper;
    }

    @Override
    protected UserDetails loadUserDetails(Assertion assertion) {
        final List<Privilege> grantedAuthorities = new ArrayList<>();
        String firstName = null;
        String lastName = null;
        String email = null;
        String displayName = null;

        if (StringUtils.isNotBlank(firstNameAttribute)) {
            final Object value = assertion.getPrincipal().getAttributes().get(firstNameAttribute);
            if (value != null) {
                firstName = value.toString();
            }
        }

        if (StringUtils.isNotBlank(lastNameAttribute)) {
            final Object value = assertion.getPrincipal().getAttributes().get(lastNameAttribute);
            if (value != null) {
                lastName = value.toString();
            }
        }

        if (StringUtils.isNotBlank(emailAttribute)) {
            final Object value = assertion.getPrincipal().getAttributes().get(emailAttribute);
            if (value != null) {
                email = value.toString();
            }
        }

        if (StringUtils.isNotBlank(displayNameAttribute)) {
            final Object value = assertion.getPrincipal().getAttributes().get(displayNameAttribute);
            if (value != null) {
                displayName = value.toString();
            }
        }

        if (StringUtils.isNotBlank(groupsAttribute)) {
            final Object value = assertion.getPrincipal().getAttributes().get(groupsAttribute);
            if (value != null) {
                if (value instanceof List) {
                    final List list = (List) value;

                    for (final Object o : list) {
                        String name = o.toString();
                        grantedAuthorities.add(new SimplePrivilege(name));
                    }

                } else {
                    String name = value.toString();
                    grantedAuthorities.add(new SimplePrivilege(name));
                }
            }
        }

        String username = assertion.getPrincipal().getName();

        SimpleUser simpleUser = SimpleUser.builder()
            .setFirstName(firstName)
            .setLastName(lastName)
            .setUsername(username)
            .setPrivileges(grantedAuthorities)
            .setEmail(email)
            .setDisplayName(displayName)
            .build();

        syncUserData.accept(simpleUser);

        Collection<Privilege> privileges = privilegeMapper.apply(username, simpleUser.getAuthorities());

        return SimpleUser.builder(simpleUser).setPrivileges(privileges).build();
    }

}
