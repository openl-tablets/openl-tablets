package org.openl.rules.security.standalone.dao;

import org.openl.rules.security.standalone.persistence.UserSetting;

public interface UserSettingDao extends Dao<UserSetting> {
    UserSetting getProperty(String login, String key);

    void setProperty(String login, String key, String value);

    void removeProperty(String login, String key);
}
