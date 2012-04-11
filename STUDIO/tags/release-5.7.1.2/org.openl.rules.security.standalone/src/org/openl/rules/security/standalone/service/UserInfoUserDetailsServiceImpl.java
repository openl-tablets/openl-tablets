package org.openl.rules.security.standalone.service;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.openl.rules.security.standalone.authentication.UserInfo;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.User;
import org.springframework.dao.DataAccessException;

import java.util.LinkedHashSet;
import java.util.StringTokenizer;

/**
 * {@link org.acegisecurity.userdetails.UserDetailsService} that can load
 * UserInfo as UserDetails from database.
 *
 * @author Andrey Naumenko
 * @author adjusted to new security model.
 */
public class UserInfoUserDetailsServiceImpl implements UserInfoUserDetailsService {
    private UserDao userDao;

    /**
     * Creates new instance of
     * <code>org.openl.rules.security.standalone.model.User</code> based on
     * persistent user object.
     *
     * @param user persistent user object
     * @return model user object
     */
    private static org.openl.rules.security.standalone.model.User modelUserFromUser(User user) {
        org.openl.rules.security.standalone.model.User ret = new org.openl.rules.security.standalone.model.User();
        ret.setFirstName(user.getFirstName());
        ret.setLastName(user.getSurname());
        ret.setLoginName(user.getLoginName());
        ret.setPassword(user.getPasswordHash());

        return ret;
    }

    protected GrantedAuthority[] createGrantedAuthorities(User user) {
        LinkedHashSet<GrantedAuthority> grantedSet = new LinkedHashSet<GrantedAuthority>();
        String privileges = user.getPrivileges();
        if (privileges != null) {
            StringTokenizer st = new StringTokenizer(privileges, ",");
            while (st.hasMoreElements()) {
                String privilege = st.nextToken();
                grantedSet.add(new GrantedAuthorityImpl(privilege));
            }
        }

        return grantedSet.toArray(new GrantedAuthority[grantedSet.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public UserInfo loadUserByUsername(String name) throws UsernameNotFoundException, DataAccessException {
        User user = userDao.getUserByName(name);
        if (user == null) {
            throw new UsernameNotFoundException("Unknown user: '" + name + "'");
        }

        GrantedAuthority[] grantedAuthorities = createGrantedAuthorities(user);
        return new UserInfo(modelUserFromUser(user), grantedAuthorities);
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
