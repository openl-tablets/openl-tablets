package org.openl.rules.webstudio.security;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UserDetails;

/**
 * Register this bean as managed bean with application scope to get possibility
 * to access user's info in JSF EL.
 *
 * @author Andrey Naumenko
 */
public class CurrentUserInfo {
    /**
     * Currently logged in user.
     *
     * @return userInfo
     */
    public UserDetails getUser() {
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx == null || ctx.getAuthentication() == null) {
            return null;
        }
        return (UserDetails) ctx.getAuthentication().getPrincipal();
    }
}
