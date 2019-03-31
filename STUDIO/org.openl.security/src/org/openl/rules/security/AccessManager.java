package org.openl.rules.security;

import java.util.Collection;

import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A utility class for privilege checking.
 *
 * @author Yury Molchan.
 */
public final class AccessManager {

    private static AccessDecisionManager accessDecisionManager;

    /**
     * Inquires whether current user has specified privilege.
     *
     * @param authority privilege to check.
     * @return <code>true</code> if current user has the privilege; <code>false</code> otherwise.
     */
    public static boolean isGranted(GrantedAuthority authority) {
        String privilege = authority.getAuthority();
        try {
            Collection<ConfigAttribute> configAttributes = SecurityConfig.createList(privilege);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            accessDecisionManager.decide(authentication, null, configAttributes);
        } catch (AccessDeniedException e) {
            // not granted
            return false;
        }
        // seem OK
        return true;
    }

    /**
     * Sets static <code>accessDecisionManager</code> property for further use in <code>check</code> methods. It is used
     * in the Spring configuration.
     *
     * @param accessDecisionManager <code>AccessDecisionManager</code> instance.
     */
    public void setStaticAccessDecisionManager(AccessDecisionManager accessDecisionManager) {
        AccessManager.accessDecisionManager = accessDecisionManager;
    }
}
