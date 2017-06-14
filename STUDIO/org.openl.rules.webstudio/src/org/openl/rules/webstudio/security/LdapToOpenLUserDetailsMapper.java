package org.openl.rules.webstudio.security;

import java.util.ArrayList;
import java.util.Collection;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

public class LdapToOpenLUserDetailsMapper implements UserDetailsContextMapper {
    private final UserDetailsContextMapper delegate;

    public LdapToOpenLUserDetailsMapper(UserDetailsContextMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx,
            String username,
            Collection<? extends GrantedAuthority> authorities) {
        UserDetails userDetails = delegate.mapUserFromContext(ctx, username, authorities);

        String firstName = ctx.getStringAttribute("givenName");
        String lastName = ctx.getStringAttribute("sn");

        Collection<? extends GrantedAuthority> userAuthorities = userDetails.getAuthorities();
        Collection<Privilege> privileges = new ArrayList<Privilege>(userAuthorities.size());
        for (GrantedAuthority authority : userAuthorities) {
            if (authority instanceof Privilege) {
                privileges.add((Privilege) authority);
            } else {
                privileges.add(new SimplePrivilege(authority.getAuthority(), authority.getAuthority()));
            }
        }
        return new SimpleUser(firstName, lastName, userDetails.getUsername(), null, privileges);
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        delegate.mapUserToContext(user, ctx);
    }
}
