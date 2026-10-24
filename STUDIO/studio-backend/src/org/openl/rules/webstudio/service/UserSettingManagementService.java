package org.openl.rules.webstudio.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.security.standalone.dao.UserSettingDao;
import org.openl.util.StringUtils;

/**
 * The values OpenL Studio keeps for each user, such as the token that verifies the e-mail.
 *
 * <p>A value is stored only while it holds more than white space.
 */
@RequiredArgsConstructor
public class UserSettingManagementService {

    private final UserSettingDao userSettingDao;

    /**
     * The value stored for the user under the key, or {@code null} when none is stored.
     */
    public @Nullable String getStringProperty(String login, String key) {
        var setting = userSettingDao.getProperty(login, key);
        return setting == null ? null : setting.getSettingValue();
    }

    /**
     * Stores the value for the user under the key. A blank value removes the one stored.
     */
    public void setProperty(String login, String key, @Nullable String value) {
        if (StringUtils.isBlank(value)) {
            userSettingDao.removeProperty(login, key);
        } else {
            userSettingDao.setProperty(login, key, value);
        }
    }
}
