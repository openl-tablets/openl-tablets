package org.openl.rules.security.standalone.service;

import org.openl.rules.security.Privileges;
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
import java.util.HashSet;
import java.util.Set;

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
        Set<Group> visitedGroups = new HashSet<Group>();
        for (Group group : groups) {
            Collection<Privilege> privileges = createPrivileges(group, visitedGroups);
            grantedList.add(new SimpleGroup(group.getName(), group.getDescription(), privileges));
        }

        return grantedList;
    }

    protected Collection<Privilege> createPrivileges(Group group, Set<Group> visitedGroups) {
        visitedGroups.add(group);
        Collection<Privilege> grantedList = new ArrayList<Privilege>();

        Set<Group> groups = group.getIncludedGroups();
        for (Group persistGroup : groups) {
            if (!visitedGroups.contains(persistGroup)) {
                visitedGroups.add(persistGroup);
                Collection<Privilege> privileges = createPrivileges(persistGroup, visitedGroups);
                SimpleGroup simpleGroup = new SimpleGroup(persistGroup.getName(), persistGroup.getDescription(), privileges);
                grantedList.add(simpleGroup);
            }
        }

        Set<String> privileges = group.getPrivileges();

        if (privileges != null) {
            for(String privilege: privileges) {
                grantedList.add(Privileges.valueOf(privilege));
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
