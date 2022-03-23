package org.openl.rules.webstudio.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Register this bean as managed bean with application scope to get possibility to access user's info in JSF EL.
 *
 * @author Andrey Naumenko
 */
public class CurrentUserInfo {
    /**
     * Currently logged in user.
     */
    public String getUserName() {
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx == null || ctx.getAuthentication() == null) {
            return null;
        }

        return ctx.getAuthentication().getName();
    }

}
