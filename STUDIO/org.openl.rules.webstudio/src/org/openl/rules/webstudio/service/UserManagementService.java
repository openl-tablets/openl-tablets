package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.User;
import org.openl.rules.security.standalone.service.PrivilegesEvaluator;
import org.openl.rules.security.standalone.service.UserInfoUserDetailsServiceImpl;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Andrei Astrouski
 */
public class UserManagementService extends UserInfoUserDetailsServiceImpl {

    private GroupDao groupDao;

    public List<org.openl.rules.security.User> getAllUsers() {
        List<User> users = userDao.getAllUsers();
        List<org.openl.rules.security.User> resultUsers = new ArrayList<>();
        for (User user : users) {
            org.openl.rules.security.User resultUser = new SimpleUser(user.getFirstName(), user.getSurname(),
                    user.getLoginName(), user.getPasswordHash(), PrivilegesEvaluator.createPrivileges(user));
            resultUsers.add(resultUser);
        }
        return resultUsers;
    }

    public List<org.openl.rules.security.User> getUsersByPrivilege(String privilege) {
        List<User> users = userDao.getAllUsers();
        List<org.openl.rules.security.User> resultUsers = new ArrayList<>();
        for (User user : users) {
            org.openl.rules.security.User resultUser = new SimpleUser(user.getFirstName(), user.getSurname(),
                    user.getLoginName(), user.getPasswordHash(), PrivilegesEvaluator.createPrivileges(user));
            if (resultUser.hasPrivilege(Privileges.ADMIN.name())
                    || resultUser.hasPrivilege(privilege)) {
                resultUsers.add(resultUser);
            }
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

    public void setGroupDao(GroupDao groupDao) {
        this.groupDao = groupDao;
    }
}
