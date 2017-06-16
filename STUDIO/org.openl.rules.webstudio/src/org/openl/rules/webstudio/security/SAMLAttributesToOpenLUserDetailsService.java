package org.openl.rules.webstudio.security;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.openl.util.StringUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

public class SAMLAttributesToOpenLUserDetailsService implements SAMLUserDetailsService {
    private final String firstNameAttribute;
    private final String lastNameAttribute;
    private final String groupsAttribute;

    public SAMLAttributesToOpenLUserDetailsService(String firstNameAttribute, String lastNameAttribute, String groupsAttribute) {
        this.firstNameAttribute = firstNameAttribute;
        this.lastNameAttribute = lastNameAttribute;
        this.groupsAttribute = groupsAttribute;
    }

    @Override
    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
        final List<Privilege> grantedAuthorities = new ArrayList<Privilege>();
        String firstName = null;
        String lastName = null;

        if (StringUtils.isNotBlank(firstNameAttribute)) {
            firstName = credential.getAttributeAsString(firstNameAttribute);
        }

        if (StringUtils.isNotBlank(lastNameAttribute)) {
            lastName = credential.getAttributeAsString(lastNameAttribute);
        }

        if (StringUtils.isNotBlank(groupsAttribute)) {
            String[] names = credential.getAttributeAsStringArray(groupsAttribute);
            if (names != null) {
                for (final String name : names) {
                    grantedAuthorities.add(new SimplePrivilege(name, name));
                }
            }
        }

        return new SimpleUser(firstName, lastName, credential.getNameID().getValue(), null, grantedAuthorities);
    }
}
