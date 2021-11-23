package org.openl.rules.webstudio.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.UserExternalFlags;
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

    public org.openl.rules.security.User getApplicationUser(String name) {
        return Optional.ofNullable(userDao.getUserByName(name))
            .map(user -> new SimpleUser(user.getFirstName(),
                user.getSurname(),
                user.getLoginName(),
                user.getPasswordHash(),
                PrivilegesEvaluator.createPrivileges(user),
                user.getEmail(),
                user.getDisplayName(),
                new UserExternalFlags(user.isFirstNameExternal(),
                    user.isLastNameExternal(),
                    user.isEmailExternal(),
                    user.isDisplayNameExternal())))
            .orElse(null);
    }

    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }

    public User getUser(String username) {
        return userDao.getUserByName(username);
    }

    public void addUser(String user,
            String firstName,
            String lastName,
            String passwordHash,
            String email,
            String displayName,
            UserExternalFlags externalFlags) {
        User persistUser = new User();
        persistUser.setLoginName(user);
        persistUser.setPasswordHash(passwordHash);
        persistUser.setFirstName(firstName);
        persistUser.setSurname(lastName);
        persistUser.setEmail(email);
        persistUser.setDisplayName(displayName);
        persistUser.setFirstNameExternal(externalFlags.isFirstNameExternal());
        persistUser.setLastNameExternal(externalFlags.isLastNameExternal());
        persistUser.setEmailExternal(externalFlags.isEmailExternal());
        persistUser.setDisplayNameExternal(externalFlags.isDisplayNameExternal());

        // TODO Implement flag below in a proper way according to EPBDS-11277
        persistUser.setEmailVerified(true);

        userDao.save(persistUser);
    }

    public void updateUserData(String user,
            String firstName,
            String lastName,
            String passwordHash,
            boolean updatePassword,
            String email,
            String displayName,
            UserExternalFlags externalFlags) {
        User persistUser = userDao.getUserByName(user);
        persistUser.setFirstName(firstName);
        persistUser.setSurname(lastName);
        persistUser.setEmail(email);
        persistUser.setDisplayName(displayName);
        persistUser.setFirstNameExternal(externalFlags.isFirstNameExternal());
        persistUser.setLastNameExternal(externalFlags.isLastNameExternal());
        persistUser.setEmailExternal(externalFlags.isEmailExternal());
        persistUser.setDisplayNameExternal(externalFlags.isDisplayNameExternal());
        if (updatePassword) {
            persistUser.setPasswordHash(passwordHash);
        }
        userDao.update(persistUser);
    }

    public void updateUserData(String user,
            String firstName,
            String lastName,
            String passwordHash,
            boolean updatePassword,
            String email,
            String displayName) {
        User persistUser = userDao.getUserByName(user);
        persistUser.setFirstName(firstName);
        persistUser.setSurname(lastName);
        persistUser.setEmail(email);
        persistUser.setDisplayName(displayName);
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
