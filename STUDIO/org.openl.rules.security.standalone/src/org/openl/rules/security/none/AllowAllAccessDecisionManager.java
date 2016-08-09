package org.openl.rules.security.none;

import java.util.Collection;

import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Aliaksandr Antonik.
 */
public class AllowAllAccessDecisionManager implements AccessDecisionManager {

    private String allowedAuthority;
    /**
     * Resolves an access control decision for the passed parameters.
     *
     * @param authentication the caller invoking the method
     * @param object the secured object being called
     * @param configAttributes the configuration attributes associated with the secured
     *            object being invoked
     * @throws org.springframework.security.access.AccessDeniedException if access is denied as
     *             the authentication does not hold a required authority or ACL
     *             privilege
     * @throws org.springframework.security.authentication.InsufficientAuthenticationException if access
     *             is denied as the authentication does not provide a sufficient
     *             level of trust
     */
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes)
            throws AccessDeniedException, InsufficientAuthenticationException {
        if (allowedAuthority != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                if (allowedAuthority.equals(authority.getAuthority())) {
                    return;
                }
            }
            throw new AccessDeniedException("Access is denied. Allowed authority is " + allowedAuthority);
        }
    }

    /**
     * Indicates whether the <code>AccessDecisionManager</code> implementation
     * is able to provide access control decisions for the indicated secured
     * object type.
     *
     * @param clazz the class that is being queried
     * @return <code>true</code> if the implementation can process the
     *         indicated class
     */
    public boolean supports(Class<?> clazz) {
        return true;
    }

    /**
     * Indicates whether this <code>AccessDecisionManager</code> is able to
     * process authorization requests presented with the passed
     * <code>ConfigAttribute</code>.
     * <p>
     * This allows the <code>AbstractSecurityInterceptor</code> to check every
     * configuration attribute can be consumed by the configured
     * <code>AccessDecisionManager</code> and/or <code>RunAsManager</code>
     * and/or <code>AfterInvocationManager</code>.
     * </p>
     *
     * @param attribute a configuration attribute that has been configured
     *            against the <code>AbstractSecurityInterceptor</code>
     * @return true if this <code>AccessDecisionManager</code> can support the
     *         passed configuration attribute
     */
    public boolean supports(ConfigAttribute attribute) {
        return true;
    }

    /**
     * Specifies an authority for full access. If it is empty then any authority is allowed.
     * @param allowedAuthority
     */
    public void setAllowedAuthority(String allowedAuthority) {
        this.allowedAuthority = allowedAuthority;
    }
}
