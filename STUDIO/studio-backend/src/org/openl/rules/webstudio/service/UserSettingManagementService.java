package org.openl.rules.webstudio.service;

import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.PropertyResolver;

import org.openl.rules.security.standalone.dao.UserSettingDao;
import org.openl.rules.security.standalone.persistence.UserSetting;
import org.openl.util.StringUtils;

@RequiredArgsConstructor
public class UserSettingManagementService {

    private final UserSettingDao userSettingDao;

    /**
     * Needed to retrieve default values
     */
    private final PropertyResolver propertyResolver;

    public String getStringProperty(String login, String key) {
        var setting = userSettingDao.getProperty(login, key);
        if (setting == null) {
            // A value for specified user not found. Return default value.
            return propertyResolver.getProperty(key);
        }

        return setting.getSettingValue();
    }

    /**
     * The settings of a user, read in one query.
     */
    public UserSettings getSettings(String login) {
        var stored = userSettingDao.getProperties(login)
                .stream()
                .collect(Collectors.toMap(setting -> setting.getId().getSettingKey(), UserSetting::getSettingValue));
        return new UserSettings(stored, propertyResolver);
    }

    public void setProperty(String login, String key, String value) {
        var defVal = propertyResolver.getProperty(key);
        if (StringUtils.isBlank(defVal) && StringUtils.isBlank(value) || Objects.equals(defVal, value)) {
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
