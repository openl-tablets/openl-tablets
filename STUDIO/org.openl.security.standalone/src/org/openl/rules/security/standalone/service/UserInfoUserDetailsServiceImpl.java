package org.openl.rules.security.standalone.service;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.User;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;

/**
 * {@link org.springframework.security.core.userdetails.UserDetailsService} that can load UserInfo as UserDetails from
 * database.
 *
 * @author Andrey Naumenko
 * @author adjusted to new security model.
 */
public class UserInfoUserDetailsServiceImpl implements UserInfoUserDetailsService {

    protected UserDao userDao;

    @Override
    public org.openl.rules.security.User loadUserByUsername(String name) throws UsernameNotFoundException,
                                                                         DataAccessException {
        User user = userDao.getUserByName(name);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("Unknown user: '%s'", name));
        }

        Collection<Privilege> privileges = PrivilegesEvaluator.createPrivileges(user);
        return new SimpleUser(user
            .getFirstName(), user.getSurname(), user.getLoginName(), user.getPasswordHash(), privileges);
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
