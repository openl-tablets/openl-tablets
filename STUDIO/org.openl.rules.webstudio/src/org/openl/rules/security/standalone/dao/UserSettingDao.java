package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.openl.rules.security.standalone.persistence.UserSetting;

public interface UserSettingDao extends Dao<UserSetting> {
    UserSetting getProperty(String login, String key);

    /**
     * The settings a user has saved. A setting the user has not changed is not among them.
     */
    List<UserSetting> getProperties(String login);

    void setProperty(String login, String key, String value);

    void removeProperty(String login, String key);
}
