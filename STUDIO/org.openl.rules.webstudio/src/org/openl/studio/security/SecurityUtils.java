package org.openl.studio.security;

import org.springframework.security.core.Authentication;

import org.openl.rules.security.Group;

/**
 * Set of utils methods to work with OpenL Security.
 *
 * @author Yury Molchan
 */
public class SecurityUtils {

    /**
     * Checks if an authentification has defined authority. It checks authority hierarchically through Groups.
     */
    public static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authority == null) {
            return false;
        }
        for (var grantedAuthority : authentication.getAuthorities()) {
            if (grantedAuthority.getAuthority().equals(authority)) {
                return true;
            } else if (grantedAuthority instanceof Group group && group.hasPrivilege(authority)) {
                return true;
            }
        }
        return false;
    }
}
