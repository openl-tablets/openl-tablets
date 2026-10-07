package org.openl.rules.webstudio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.security.standalone.dao.UserSettingDao;
import org.openl.rules.security.standalone.persistence.UserSetting;
import org.openl.rules.security.standalone.persistence.UserSettingId;

/**
 * Reading the settings of a user at once, with the defaults of the configuration.
 *
 * @author Yury Molchan
 */
class UserSettingsTest {

    private final MockEnvironment defaults = new MockEnvironment().withProperty("test.tests.perpage", "5")
            .withProperty("table.formulas.show", "false")
            .withProperty("table.view", "developer");

    @Test
    void readsTheStoredValueBeforeTheDefault() {
        var settings = new UserSettings(Map.of("test.tests.perpage", "20", "table.formulas.show", "true"), defaults);

        assertEquals(20, settings.getInteger("test.tests.perpage"));
        assertEquals(true, settings.getBoolean("table.formulas.show"));
    }

    @Test
    void readsTheDefaultOfASettingTheUserHasNotChanged() {
        var settings = new UserSettings(Map.of(), defaults);

        assertEquals(5, settings.getInteger("test.tests.perpage"));
        assertEquals(false, settings.getBoolean("table.formulas.show"));
        assertEquals("developer", settings.getString("table.view"));
    }

    @Test
    void readsNothingForASettingWithNeitherValueNorDefault() {
        var settings = new UserSettings(Map.of(), new MockEnvironment().withProperty("test.failures.only", " "));

        assertNull(settings.getString("table.view"));
        assertNull(settings.getBoolean("test.failures.only"));
        assertNull(settings.getInteger("test.tests.perpage"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"all", "5.5", "99999999999"})
    void readsNothingForANumberThatCannotBeRead(String value) {
        var settings = new UserSettings(Map.of("test.tests.perpage", value), defaults);

        assertNull(settings.getInteger("test.tests.perpage"));
    }

    @Test
    void readsAllSettingsOfTheUserInOneQuery() {
        var dao = mock(UserSettingDao.class);
        when(dao.getProperties("jdoe")).thenReturn(List.of(setting("test.tests.perpage", "20")));

        var settings = new UserSettingManagementService(dao, defaults).getSettings("jdoe");

        assertEquals(20, settings.getInteger("test.tests.perpage"));
        assertEquals(false, settings.getBoolean("table.formulas.show"));
        verify(dao).getProperties("jdoe");
        verifyNoMoreInteractions(dao);
    }

    private static UserSetting setting(String key, String value) {
        var setting = new UserSetting();
        setting.setId(new UserSettingId("jdoe", key));
        setting.setSettingValue(value);
        return setting;
    }
}
