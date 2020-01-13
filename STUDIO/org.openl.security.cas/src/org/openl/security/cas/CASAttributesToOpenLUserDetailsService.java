package org.openl.security.cas;

import java.util.ArrayList;
import java.util.List;

import org.jasig.cas.client.validation.Assertion;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.cas.userdetails.AbstractCasAssertionUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;

public class CASAttributesToOpenLUserDetailsService extends AbstractCasAssertionUserDetailsService {
    private final String firstNameAttribute;
    private final String lastNameAttribute;
    private final String groupsAttribute;

    public CASAttributesToOpenLUserDetailsService(PropertyResolver propertyResolver) {
        this.firstNameAttribute = propertyResolver.getProperty("security.cas.attribute.first-name");
        this.lastNameAttribute = propertyResolver.getProperty("security.cas.attribute.last-name");
        this.groupsAttribute = propertyResolver.getProperty("security.cas.attribute.groups");
    }

    @Override
    protected UserDetails loadUserDetails(Assertion assertion) {
        final List<Privilege> grantedAuthorities = new ArrayList<Privilege>();
        String firstName = null;
        String lastName = null;

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

        if (StringUtils.isNotBlank(groupsAttribute)) {
            final Object value = assertion.getPrincipal().getAttributes().get(groupsAttribute);
            if (value != null) {
                if (value instanceof List) {
                    final List list = (List) value;

                    for (final Object o : list) {
                        String name = o.toString();
                        grantedAuthorities.add(new SimplePrivilege(name, name));
                    }

                } else {
                    String name = value.toString();
                    grantedAuthorities.add(new SimplePrivilege(name, name));
                }
            }
        }

        return new SimpleUser(firstName, lastName, assertion.getPrincipal().getName(), null, grantedAuthorities);
    }

}
