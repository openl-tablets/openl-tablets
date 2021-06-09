package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.User;

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

    public void addUser(String user, String firstName, String lastName, String passwordHash) {
        User persistUser = new User();
        persistUser.setLoginName(user);
        persistUser.setPasswordHash(passwordHash);
        persistUser.setFirstName(firstName);
        persistUser.setSurname(lastName);

        userDao.save(persistUser);
    }

    public void updateUserData(String user,
            String firstName,
            String lastName,
            String passwordHash,
            boolean updatePassword) {
        User persistUser = userDao.getUserByName(user);
        persistUser.setFirstName(firstName);
        persistUser.setSurname(lastName);
        if (updatePassword) {
            persistUser.setPasswordHash(passwordHash);
        }
        userDao.update(persistUser);
    }

    public void updateAuthorities(String user, Set<String> authorities) {
        User persistUser = userDao.getUserByName(user);
        Set<Group> groups = new HashSet<>();
        for (String auth : authorities) {
            groups.add(groupDao.getGroupByName(auth));
        }
        persistUser.setGroups(groups);

        userDao.update(persistUser);
    }

    public void updateAuthorities(final String user, final Set<String> authorities, final boolean leaveAdminGroups) {
        Set<String> fullAuthorities = new HashSet<>(authorities);
        if (leaveAdminGroups) {
            User persistUser = userDao.getUserByName(user);
            Set<Group> currentGroups = persistUser.getGroups();
            Set<String> currentAdminGroups = getCurrentAdminGroups(currentGroups);
            fullAuthorities.addAll(currentAdminGroups);
        }
        updateAuthorities(user, fullAuthorities);
    }

    public Set<String> getCurrentAdminGroups(final Set<Group> groups) {
        Set<String> groupNames = new HashSet<>();

        for (Group group : groups) {
            SimpleGroup simpleGroup = PrivilegesEvaluator.wrap(group);
            if (simpleGroup.hasPrivilege(Privileges.ADMIN.getAuthority())) {
                groupNames.add(group.getName());
            }
        }

        return groupNames;
    }

    public void deleteUser(String username) {
        userDao.deleteUserByName(username);
    }
}
