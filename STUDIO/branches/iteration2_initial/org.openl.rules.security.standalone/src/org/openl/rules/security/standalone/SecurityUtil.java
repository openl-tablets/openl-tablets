package org.openl.rules.security.standalone;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.context.SecurityContextHolder;
import org.openl.rules.security.Privileges;

/**
 * @author Aleh Bykhavets
 */
public class SecurityUtil {
    /**
     * Checks whether current user is granted with specified authority.
     * <p/>
     * Simple use case:
     * <pre>
     *   if (SecurityUtil.isGranted(Roles.ROLE_ADMIN)) {
     *     // user is allowed to do something
     *   } else {
     *     // user isn't allowed to do that
     *   }
     * </pre>
     *
     * @param authority name of authority (role)
     * @return <code>true</code> if active user is granted with the authority;
     *         <code>false</code> otherwise.
     */
    public static boolean isGranted(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // no user
        if (authentication == null) return false;

        GrantedAuthority[] grantedAuthorities = authentication.getAuthorities();
        if (grantedAuthorities == null) return false;

        for(GrantedAuthority granted : grantedAuthorities) {
            String s = granted.getAuthority();
            if (authority.equals(s) || Privileges.ROLE_ADMIN.equals(s)) {
                // has such authority or the user is admin
                return true;
            }
        }

        return false;
    }
}
