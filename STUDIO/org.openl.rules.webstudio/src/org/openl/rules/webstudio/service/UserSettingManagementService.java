package org.openl.rules.webstudio.service;

import org.openl.rules.security.standalone.dao.UserSettingDao;
import org.openl.rules.security.standalone.persistence.UserSetting;
import org.springframework.core.env.PropertyResolver;

public class UserSettingManagementService {
    public static final String RULES_TREE_VIEW = "rules.tree.view";
    public static final String TABLE_VIEW = "table.view";
    public static final String TABLE_FORMULAS_SHOW = "table.formulas.show";
    public static final String TEST_TESTS_PERPAGE = "test.tests.perpage";
    public static final String TEST_FAILURES_ONLY = "test.failures.only";
    public static final String TEST_FAILURES_PERTEST = "test.failures.pertest";
    public static final String TEST_RESULT_COMPLEX_SHOW = "test.result.complex.show";
    public static final String TRACE_REALNUMBERS_SHOW = "trace.realNumbers.show";

    private final UserSettingDao userSettingDao;

    /**
     * Needed to retrieve default values
     */
    private final PropertyResolver propertyResolver;

    public UserSettingManagementService(UserSettingDao userSettingDao, PropertyResolver propertyResolver) {
        this.userSettingDao = userSettingDao;
        this.propertyResolver = propertyResolver;
    }

    public String getStringProperty(String login, String key) {
        UserSetting setting = userSettingDao.getProperty(login, key);
        if (setting == null) {
            // A value for specified user not found. Return default value.
            return propertyResolver.getProperty(key);
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
        String defVal = propertyResolver.getProperty(key);
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
