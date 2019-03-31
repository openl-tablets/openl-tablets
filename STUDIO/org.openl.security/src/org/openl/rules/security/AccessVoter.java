package org.openl.rules.security;

import java.util.Collection;

import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * <p>
 * Based on {@link org.springframework.security.access.vote.RoleVoter}. If Authentication has
 * {@link org.openl.rules.security.Privileges#ADMIN} authority it will get access even if it is not specified
 * explicitly.
 * </p>
 * <p>
 * Votes if any {@link ConfigAttribute#getAttribute()} starts with a prefix indicating that it is a role. The prefix
 * string is <Code>GROUP_</code>.
 * </p>
 * <p>
 * Abstains from voting if no configuration attribute commences with the role prefix. Votes to grant access if there is
 * an exact matching {@link org.springframework.security.core.GrantedAuthority} to a <code>ConfigAttribute</code>
 * starting with the role prefix. Votes to deny access if there is no exact matching <code>GrantedAuthority</code> to a
 * <code>ConfigAttribute</code>.
 * </p>
 * <p>
 * All comparisons and prefixes are case sensitive.
 * </p>
 *
 * @author Aleh Bykhavets
 */
public class AccessVoter implements AccessDecisionVoter<Object> {
    /**
     * This implementation supports any type of class, because it does not query the presented secure object.
     *
     * @param aClass the secure object
     *
     * @return always <code>true</code>
     */
    @Override
    public boolean supports(Class<?> aClass) {
        return true;
    }

    @Override
    public boolean supports(ConfigAttribute configAttribute) {
        return true;
    }

    /**
     * Votes whether access should be granted.
     *
     * @param authentication Authentication (Principal)
     * @param object Ignored in current implementation
     * @param configAttributes attributes (required Authorities)
     * @return {@link #ACCESS_DENIED} or {@link #ACCESS_ABSTAIN} or {@link #ACCESS_GRANTED}
     */
    @Override
    public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes) {
        int result = ACCESS_ABSTAIN;

        for (ConfigAttribute attribute : configAttributes) {
            if (this.supports(attribute)) {
                result = ACCESS_DENIED;

                String neededAuthority = attribute.getAttribute();

                // Attempt to find a matching granted authority
                for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
                    String availableAuthority = grantedAuthority.getAuthority();

                    if (neededAuthority.equals(availableAuthority)) {
                        return ACCESS_GRANTED;
                    }

                    if (Privileges.ADMIN.name().equals(availableAuthority)) {
                        return ACCESS_GRANTED;
                    }

                    if (grantedAuthority instanceof Group) {
                        Group group = (Group) grantedAuthority;
                        // No restrictions
                        if (group.hasPrivilege(Privileges.ADMIN.name())) {
                            return ACCESS_GRANTED;
                        }
                        if (group.hasPrivilege(neededAuthority)) {
                            return ACCESS_GRANTED;
                        }
                    }
                }
            }
        }

        return result;
    }
}
