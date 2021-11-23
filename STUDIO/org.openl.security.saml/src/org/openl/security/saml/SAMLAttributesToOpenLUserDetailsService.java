package org.openl.security.saml;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.UserExternalFlags;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

public class SAMLAttributesToOpenLUserDetailsService implements SAMLUserDetailsService {
    private final String usernameAttribute;
    private final String firstNameAttribute;
    private final String lastNameAttribute;
    private final String groupsAttribute;
    private final String emailAttribute;
    private final String displayNameAttribute;
    /**
     * Must map to {@link org.openl.rules.security.Privilege}
     */
    private final Function<SimpleUser, SimpleUser> authoritiesMapper;

    public SAMLAttributesToOpenLUserDetailsService(PropertyResolver propertyResolver,
            Function<SimpleUser, SimpleUser> authoritiesMapper) {
        this.usernameAttribute = propertyResolver.getProperty("security.saml.attribute.username");
        this.firstNameAttribute = propertyResolver.getProperty("security.saml.attribute.first-name");
        this.lastNameAttribute = propertyResolver.getProperty("security.saml.attribute.last-name");
        this.emailAttribute = propertyResolver.getProperty("security.saml.attribute.email");
        this.groupsAttribute = propertyResolver.getProperty("security.saml.attribute.groups");
        this.displayNameAttribute = propertyResolver.getProperty("security.saml.attribute.display-name");
        this.authoritiesMapper = authoritiesMapper;
    }

    @Override
    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
        final List<Privilege> grantedAuthorities = new ArrayList<>();
        String username = credential.getNameID().getValue();
        String firstName = null;
        String lastName = null;
        String email = null;
        String displayName = null;

        if (StringUtils.isNotBlank(usernameAttribute)) {
            username = credential.getAttributeAsString(usernameAttribute);
        }

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

        if (StringUtils.isNotBlank(emailAttribute)) {
            email = credential.getAttributeAsString(emailAttribute);

        }
        if (StringUtils.isNotBlank(displayNameAttribute)) {
            displayName = credential.getAttributeAsString(displayNameAttribute);
        }

        SimpleUser simpleUser = new SimpleUser(firstName,
            lastName,
            username,
            null,
            grantedAuthorities,
            email,
            displayName,
            new UserExternalFlags(StringUtils.isNotBlank(firstName),
                StringUtils.isNotBlank(lastName),
                StringUtils.isNotBlank(email),
                StringUtils.isNotBlank(displayName)));
        return authoritiesMapper.apply(simpleUser);
    }
}
