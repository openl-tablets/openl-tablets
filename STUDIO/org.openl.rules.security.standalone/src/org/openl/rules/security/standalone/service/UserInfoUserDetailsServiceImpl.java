package org.openl.rules.security.standalone.service;

import org.openl.rules.security.DefaultPrivileges;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.User;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
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

        Set<Group> groups = user.getGroups();
        for (Group group : groups) {
            grantedList.add(
                    new SimpleGroup(group.getName(), group.getDescription(), createPrivileges(group)));
        }

        return grantedList;
    }

    protected Collection<Privilege> createPrivileges(String privileges) {
        Collection<Privilege> grantedList = new ArrayList<Privilege>();

        if (privileges != null) {
            StringTokenizer st = new StringTokenizer(privileges, ",");
            while (st.hasMoreElements()) {
                String privilege = st.nextToken();
                grantedList.add(DefaultPrivileges.valueOf(privilege));
             }
        }

        return grantedList;
    }

    protected Collection<Privilege> createPrivileges(Group group) {
        Collection<Privilege> grantedList = new ArrayList<Privilege>();

        Set<Group> groups = group.getIncludedGroups();
        for (Group persistGroup : groups) {
            grantedList.add(
                    new SimpleGroup(persistGroup.getName(), persistGroup.getDescription(), createPrivileges(persistGroup)));
        }

        grantedList.addAll(createPrivileges(group.getPrivileges()));

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
