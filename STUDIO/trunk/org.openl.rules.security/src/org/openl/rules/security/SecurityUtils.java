package org.openl.rules.security;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Aliaksandr Antonik.
 */
public final class SecurityUtils {

    public static final String LOCAL_USER_ID = "LOCAL";

    private static AccessDecisionManager accessDecisionManager;

    /**
     * Checks that current security context authentication is authorized to
     * access a secured object with given security attributes.
     *
     * @param config the configuration attributes of a secured object.
     * @throws AccessDeniedException if current authentication context does not
     *             hold a required authority
     */
    public static void check(Collection<ConfigAttribute> configAttributes) throws AccessDeniedException {
        accessDecisionManager.decide(SecurityContextHolder.getContext().getAuthentication(), null, configAttributes);
    }

    /**
     * Converts <code>privilege</code> to
     * collection of <code>ConfigAttribute</code> objects.
     *
     * @param privilege privilege to check.
     * @throws AccessDeniedException if current authentication context does not
     *             hold a required authority
     */
    public static void check(String privilege) throws AccessDeniedException {
        Collection<ConfigAttribute> configAttributes = new ArrayList<ConfigAttribute>();
        configAttributes.add(new SecurityConfig(privilege));

        check(configAttributes);
    }

    /**
     * Inquires whether current user has specified privilege.
     *
     * @param privilege privilege to check.
     * @return <code>true</code> if current user has the privilege;
     *         <code>false</code> otherwise.
     */
    public static boolean isGranted(String privilege) {
        try {
            check(privilege);
        } catch (AccessDeniedException e) {
            // not granted
            return false;
        }
        // seem OK
        return true;
    }

    /**
     * Sets static <code>accessDecisionManager</code> property for further use
     * in <code>check</code> methods.
     *
     * @param adm <code>AccessDecisionManager</code> instance.
     */
    public static void useAccessDecisionManager(AccessDecisionManager adm) {
        accessDecisionManager = adm;
    }

    /**
     * Sets static <code>accessDecisionManager</code> property for further use
     * in <code>check</code> methods. <br/> The only difference of this method
     * from
     * {@link #useAccessDecisionManager(org.springframework.security.access.AccessDecisionManager)}
     * that this one is not <i>static</i>.
     *
     * @param accessDecisionManager <code>AccessDecisionManager</code>
     *            instance.
     */
    public void setStaticAccessDecisionManager(AccessDecisionManager accessDecisionManager) {
        useAccessDecisionManager(accessDecisionManager);
    }
}
