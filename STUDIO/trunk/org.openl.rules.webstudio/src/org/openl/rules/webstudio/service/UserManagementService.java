package org.openl.rules.webstudio.service;

import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.standalone.persistence.User;
import org.openl.rules.security.standalone.service.UserInfoUserDetailsServiceImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrei Astrouski
 */
public class UserManagementService extends UserInfoUserDetailsServiceImpl {

    public List<org.openl.rules.security.User> getAllUsers() {
        List<User> users = userDao.getAll();
        List<org.openl.rules.security.User> resultUsers = new ArrayList<org.openl.rules.security.User>();
        for (User user : users) {
            org.openl.rules.security.User resultUser = new SimpleUser(user.getFirstName(), user.getSurname(),
                    user.getLoginName(), user.getPasswordHash(), createPrivileges(user));
            resultUsers.add(resultUser);
        }
        return resultUsers;
    }

    public void deleteUser(String username) {
        userDao.delete(userDao.getUserByName(username));
    }

}
