package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.User;
import org.openl.rules.security.standalone.service.PrivilegesEvaluator;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Andrei Astrouski
 */
public class UserManagementService {

    private final UserDao userDao;
    private final GroupDao groupDao;

    public UserManagementService(UserDao userDao, GroupDao groupDao) {
        this.userDao = userDao;
        this.groupDao = groupDao;
    }

    public org.openl.rules.security.User loadUserByUsername(String name) {
        User user = userDao.getUserByName(name);

        if (user == null) {
            return null;
        }

        Collection<Privilege> privileges = PrivilegesEvaluator.createPrivileges(user);
        String firstName = user.getFirstName();
        String lastName = user.getSurname();
        String username = user.getLoginName();
        String passwordHash = user.getPasswordHash();

        return new SimpleUser(firstName, lastName, username, passwordHash, privileges);
    }

    public List<org.openl.rules.security.User> getAllUsers() {
        List<User> users = userDao.getAllUsers();
        List<org.openl.rules.security.User> resultUsers = new ArrayList<>();
        for (User user : users) {
            org.openl.rules.security.User resultUser = new SimpleUser(user.getFirstName(),
                user.getSurname(),
                user.getLoginName(),
                user.getPasswordHash(),
                PrivilegesEvaluator.createPrivileges(user));
            resultUsers.add(resultUser);
        }
        return resultUsers;
    }

    public void addUser(org.openl.rules.security.User user) {
        User persistUser = new User();
        persistUser.setLoginName(user.getUsername());
        persistUser.setPasswordHash(user.getPassword());
        persistUser.setFirstName(user.getFirstName());
        persistUser.setSurname(user.getLastName());

        Set<Group> groups = new HashSet<>();
        for (GrantedAuthority auth : user.getAuthorities()) {
            groups.add(groupDao.getGroupByName(auth.getAuthority()));
        }
        persistUser.setGroups(groups);

        userDao.save(persistUser);
    }

    public void updateUser(org.openl.rules.security.User user) {
        User persistUser = userDao.getUserByName(user.getUsername());

        persistUser.setFirstName(user.getFirstName());
        persistUser.setSurname(user.getLastName());

        Set<Group> groups = new HashSet<>();
        for (GrantedAuthority auth : user.getAuthorities()) {
            groups.add(groupDao.getGroupByName(auth.getAuthority()));
        }
        persistUser.setGroups(groups);
        if (user.getPassword() != null) {
            persistUser.setPasswordHash(user.getPassword());
        }

        userDao.update(persistUser);
    }

    public void deleteUser(String username) {
        userDao.deleteUserByName(username);
    }
}
