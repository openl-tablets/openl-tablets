package org.openl.rules.security.standalone.service;

import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.openl.rules.security.standalone.authentication.UserInfo;
import org.springframework.dao.DataAccessException;

/**
 * {@link org.acegisecurity.userdetails.UserDetailsService} extentsion that
 * returns {@link UserInfoUserDetailsService} objects. This interface must be
 * implemented by any OpenL security implementation.
 *
 * @author Aliaksandr Antonik.
 */
public interface UserInfoUserDetailsService extends UserDetailsService {
    /**
     * Locates the user based on the username. In the actual implementation, the
     * search may possibly be case insensitive, or case insensitive depending on
     * how the implementaion instance is configured. In this case, the
     * <code>UserDetails</code> object that comes back may have a username
     * that is of a different case than what was actually requested..
     *
     * @param username the username presented to the
     *            {@link org.acegisecurity.providers.dao.DaoAuthenticationProvider}
     * @return a fully populated user record (never <code>null</code>)
     * @throws org.acegisecurity.userdetails.UsernameNotFoundException if the
     *             user could not be found or the user has no GrantedAuthority
     * @throws org.springframework.dao.DataAccessException if user could not be
     *             found for a repository-specific reason
     */
    UserInfo loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException;
}
