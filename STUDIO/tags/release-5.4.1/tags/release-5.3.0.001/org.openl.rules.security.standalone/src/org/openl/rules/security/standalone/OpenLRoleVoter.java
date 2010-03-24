package org.openl.rules.security.standalone;

import org.acegisecurity.Authentication;
import org.acegisecurity.ConfigAttribute;
import org.acegisecurity.ConfigAttributeDefinition;
import org.acegisecurity.vote.AccessDecisionVoter;
import org.openl.rules.security.Privileges;

import java.util.Iterator;

/**
 * <p>
 * Based on {@link org.acegisecurity.vote.RoleVoter}. If Authentication has
 * {@link org.openl.rules.security.Privileges#ROLE_ADMIN} authority it will get
 * access even if it is not specified explicitly.
 * </p>
 * <p>
 * Votes if any {@link ConfigAttribute#getAttribute()} starts with a prefix
 * indicating that it is a role. The prefix string is <Code>ROLE_</code>.
 * </p>
 * <p>
 * Abstains from voting if no configuration attribute commences with the role
 * prefix. Votes to grant access if there is an exact matching
 * {@link org.acegisecurity.GrantedAuthority} to a <code>ConfigAttribute</code>
 * starting with the role prefix. Votes to deny access if there is no exact
 * matching <code>GrantedAuthority</code> to a <code>ConfigAttribute</code>
 * starting with the role prefix ({@link org.openl.rules.security.Privileges#ROLE_PREFIX}).
 * </p>
 * <p>
 * All comparisons and prefixes are case sensitive.
 * </p>
 *
 * @author Aleh Bykhavets
 */
public class OpenLRoleVoter implements AccessDecisionVoter {
    /**
     * This implementation supports any type of class, because it does not query
     * the presented secure object.
     *
     * @param aClass the secure object
     *
     * @return always <code>true</code>
     */
    public boolean supports(Class aClass) {
        return true;
    }

    public boolean supports(ConfigAttribute configAttribute) {
        return true;
        /*
         * String attr = configAttribute.getAttribute();
         *
         * if ((attr != null) && attr.startsWith(Roles.ROLE_PREFIX)) { return
         * true; } else { return false; }
         */
    }

    /**
     * Votes whether access should be granted.
     *
     * @param authentication Authentication (Principal)
     * @param object Ignored in current implementation
     * @param configAttributeDefinition attributes (required Authorities)
     * @return {@link #ACCESS_DENIED} or {@link #ACCESS_ABSTAIN} or
     *         {@link #ACCESS_GRANTED}
     */
    public int vote(Authentication authentication, Object object, ConfigAttributeDefinition configAttributeDefinition) {
        int result = ACCESS_ABSTAIN;
        Iterator iter = configAttributeDefinition.getConfigAttributes();

        while (iter.hasNext()) {
            ConfigAttribute attribute = (ConfigAttribute) iter.next();

            if (this.supports(attribute)) {
                result = ACCESS_DENIED;

                String attr = attribute.getAttribute();

                // Attempt to find a matching granted authority
                for (int i = 0; i < authentication.getAuthorities().length; i++) {
                    String authority = authentication.getAuthorities()[i].getAuthority();
                    if (attr.equals(authority) || Privileges.ROLE_ADMIN.equals(authority)) {
                        // admin is always right, even if it is not stated
                        // directly
                        return ACCESS_GRANTED;
                    }
                }
            }
        }

        return result;
    }
}
