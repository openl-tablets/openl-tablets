package org.openl.rules.webstudio.service;

import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.User;

/**
 * Creates a user for single user mode.
 */
public class SingleUserModeInit {
    public SingleUserModeInit(UserDao userDao, String userName, String email, String displayName) {
        User defaultUser = userDao.getUserByName(userName);
        if (defaultUser == null) {
            User persistUser = new User();
            persistUser.setLoginName(userName);
            persistUser.setEmail(email);
            persistUser.setEmailVerified(true);
            persistUser.setDisplayName(displayName);
            userDao.save(persistUser);
        } else {
            if (defaultUser.getEmail() == null) {
                defaultUser.setEmailVerified(true);
                defaultUser.setEmail(email);
            }
            if (defaultUser.getDisplayName() == null) {
                defaultUser.setDisplayName(displayName);
            }
            userDao.update(defaultUser);
        }
    }
}
