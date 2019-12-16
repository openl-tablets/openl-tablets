package org.openl.rules.webstudio.service;

import org.openl.rules.security.standalone.dao.UserSettingDao;
import org.openl.rules.security.standalone.persistence.UserSetting;
import org.springframework.core.env.Environment;

public class UserSettingManagementService {
    private final UserSettingDao userSettingDao;

    /**
     * Needed to retrieve default values
     */
    private final Environment environment;

    public UserSettingManagementService(UserSettingDao userSettingDao, Environment environment) {
        this.userSettingDao = userSettingDao;
        this.environment = environment;
    }

    public String getStringProperty(String login, String key) {
        UserSetting setting = userSettingDao.getProperty(login, key);
        if (setting == null) {
            // A value for specified user not found. Return default value.
            return environment.getProperty(key);
        }

        return setting.getSettingValue();
    }

    public boolean getBooleanProperty(String login, String key) {
        String value = getStringProperty(login, key);
        if (value == null) {
            throw new IllegalArgumentException(
                "Can't cast null to boolean. Probably default value for property " + key + " is absent.");
        }

        return Boolean.parseBoolean(value);
    }

    public int getIntegerProperty(String login, String key) {
        String value = getStringProperty(login, key);
        if (value == null) {
            throw new IllegalArgumentException(
                "Can't cast null to int. Probably default value for property " + key + " is absent.");
        }

        return Integer.parseInt(value);
    }

    public void setProperty(String login, String key, String value) {
        String defVal = environment.getProperty(key);
        if (defVal == null) {
            throw new IllegalArgumentException("Default value for the key " + key + " is absent.");
        }
        if (defVal.equals(value)) {
            userSettingDao.removeProperty(login, key);
        } else {
            userSettingDao.setProperty(login, key, value);
        }
    }

    public void setProperty(String login, String key, boolean value) {
        setProperty(login, key, Boolean.toString(value));
    }

    public void setProperty(String login, String key, int value) {
        setProperty(login, key, Integer.toString(value));
    }
}
