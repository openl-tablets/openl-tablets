package org.openl.rules.webstudio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.rules.security.standalone.dao.UserSettingDao;
import org.openl.rules.security.standalone.persistence.UserSetting;

/**
 * Tests the values {@link UserSettingManagementService} keeps for a user.
 *
 * @author Yury Molchan
 */
@ExtendWith(MockitoExtension.class)
class UserSettingManagementServiceTest {

    @Mock
    private UserSettingDao dao;

    @InjectMocks
    private UserSettingManagementService service;

    @Test
    void readsTheStoredValue() {
        var setting = new UserSetting();
        setting.setSettingValue("token");
        when(dao.getProperty("jdoe", "mail.verify.token")).thenReturn(setting);

        assertEquals("token", service.getStringProperty("jdoe", "mail.verify.token"));
    }

    @Test
    void readsNothingWhereNoValueIsStored() {
        assertNull(service.getStringProperty("jdoe", "mail.verify.token"));
    }

    @Test
    void storesAValue() {
        service.setProperty("jdoe", "mail.verify.token", "token");

        verify(dao).setProperty("jdoe", "mail.verify.token", "token");
        verify(dao, never()).removeProperty("jdoe", "mail.verify.token");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void removesTheValueForABlankOne(String value) {
        service.setProperty("jdoe", "mail.verify.token", value);

        verify(dao).removeProperty("jdoe", "mail.verify.token");
        verify(dao, never()).setProperty("jdoe", "mail.verify.token", value);
    }
}
