package org.openl.rules.webstudio.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openl.rules.security.Group;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.util.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class OpenLAuthenticationProviderWrapper implements AuthenticationProvider {
    private final AuthenticationProvider delegate;
    private final UserManagementService userManagementService;
    private final GroupManagementService groupManagementService;
    private final String origin;
    private String defaultGroup = null;
    private boolean groupsAreManagedInStudio = true;

    public OpenLAuthenticationProviderWrapper(AuthenticationProvider delegate,
            UserManagementService userManagementService,
            GroupManagementService groupManagementService,
            String origin) {
        this.delegate = delegate;
        this.userManagementService = userManagementService;
        this.groupManagementService = groupManagementService;
        this.origin = StringUtils.trimToNull(origin);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication delegatedAuth = delegate.authenticate(authentication);
        if (!groupsAreManagedInStudio) {
            return delegatedAuth;
        }

        if (delegatedAuth != null) {
            Collection<? extends GrantedAuthority> authorities;
            try {
                UserDetails dbUser = userManagementService.loadUserByUsername(authentication.getName());
                authorities = dbUser.getAuthorities();
            } catch (UsernameNotFoundException e) {
                List<Privilege> groups;
                if (!StringUtils.isBlank(defaultGroup) && groupManagementService.isGroupExist(defaultGroup)) {
                    Group group = groupManagementService.getGroupByName(defaultGroup);
                    groups = Collections.singletonList((Privilege) group);
                } else {
                    groups = Collections.emptyList();
                }
                authorities = groups;
                userManagementService.addUser(new SimpleUser("", "", authentication.getName(), "", origin, groups));
            }
            return new UsernamePasswordAuthenticationToken(delegatedAuth.getPrincipal(), delegatedAuth.getCredentials(),
                    authorities);
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public void setGroupsAreManagedInStudio(boolean groupsAreManagedInStudio) {
        this.groupsAreManagedInStudio = groupsAreManagedInStudio;
    }
}
