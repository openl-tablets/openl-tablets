package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.webstudio.web.admin.AdministrationSettings;

/**
 * Covers whether tables are formatted on save, as the administrator sets it.
 *
 * @author Yury Molchan
 */
class TableFormattingTest {

    @Test
    void formatsOnSaveUnlessTheAdministratorTurnedItOff() {
        assertTrue(new TableFormatting(new MockEnvironment()).onSave(), "On by default");
        assertTrue(new TableFormatting(new MockEnvironment()
                .withProperty(AdministrationSettings.FORMAT_TABLES_ON_SAVE, "true")).onSave());
        assertFalse(new TableFormatting(new MockEnvironment()
                .withProperty(AdministrationSettings.FORMAT_TABLES_ON_SAVE, "false")).onSave());
    }
}
