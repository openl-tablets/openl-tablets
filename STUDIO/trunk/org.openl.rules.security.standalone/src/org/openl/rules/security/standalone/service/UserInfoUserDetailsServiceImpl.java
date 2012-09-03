package org.openl.rules.security.standalone.service;

import org.openl.rules.security.PredefinedGroups;
import org.openl.rules.security.PredefinedPrivileges;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.User;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * {@link org.springframework.security.core.userdetails.UserDetailsService} that can load
 * UserInfo as UserDetails from database.
 *
 * @author Andrey Naumenko
 * @author adjusted to new security model.
 */
public class UserInfoUserDetailsServiceImpl implements UserInfoUserDetailsService {

    protected UserDao userDao;

    protected Collection<Privilege> createPrivileges(User user) {
        Collection<Privilege> grantedList = new ArrayList<Privilege>();
        String privileges = user.getPrivileges();
        if (privileges != null) {
            StringTokenizer st = new StringTokenizer(privileges, ",");
            while (st.hasMoreElements()) {
                String privilege = st.nextToken();
                if (privilege.startsWith("GROUP")) {
                    grantedList.add(PredefinedGroups.valueOf(privilege));
                } else {
                    grantedList.add(PredefinedPrivileges.valueOf(privilege));
                }
            }
        }

        return grantedList;
    }

    @Override
    public org.openl.rules.security.User loadUserByUsername(String name)
            throws UsernameNotFoundException, DataAccessException {
        User user = userDao.getUserByName(name);
        if (user == null) {
            throw new UsernameNotFoundException("Unknown user: '" + name + "'");
        }

        Collection<Privilege> privileges = createPrivileges(user);
        return new SimpleUser(user.getFirstName(), user.getSurname(),
                user.getLoginName(), user.getPasswordHash(), privileges);
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
