package org.openl.rules.webstudio.service;

import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.User;

/**
 * Creates a user for single user mode.
 */
public class SingleUserModeInit {
    public SingleUserModeInit(UserDao userDao, String userName, String email, String displayName) {
        User defaultUser = userDao.getUserByName(userName);
        UserExternalFlags.Builder externalFlags = UserExternalFlags.builder()
            .withFeature(UserExternalFlags.Feature.EMAIL_VERIFIED);
        if (defaultUser == null) {
            User persistUser = new User();
            persistUser.setLoginName(userName);
            persistUser.setEmail(email);
            persistUser.setFlags(externalFlags.getRawFeatures());
            persistUser.setDisplayName(displayName);
            userDao.save(persistUser);
        } else {
            if (defaultUser.getEmail() == null) {
                defaultUser.setFlags(externalFlags.getRawFeatures());
                defaultUser.setEmail(email);
            }
            if (defaultUser.getDisplayName() == null) {
                defaultUser.setDisplayName(displayName);
            }
            userDao.update(defaultUser);
        }
    }
}
