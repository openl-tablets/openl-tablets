package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.openl.config.PropertiesHolder;

class AdministrationSettingsTest {

    @Test
    void detectsProjectsByExcelFilesWhenEnabled() {
        var properties = mock(PropertiesHolder.class);
        when(properties.getProperty(AdministrationSettings.PROJECT_DETECT_BY_EXCEL_FILES)).thenReturn("true");
        var settings = new AdministrationSettings();

        settings.load(properties);

        assertTrue(settings.getDetectProjectsByExcelFiles());

        settings.setDetectProjectsByExcelFiles(false);
        settings.store(properties);
        verify(properties).setProperty(AdministrationSettings.PROJECT_DETECT_BY_EXCEL_FILES, false);
    }

    @Test
    void disablesProjectDetectionByExcelFilesByDefaultAndRevertsIt() {
        var properties = mock(PropertiesHolder.class);
        var settings = new AdministrationSettings();

        settings.load(properties);
        assertFalse(settings.getDetectProjectsByExcelFiles());

        settings.revert(properties);
        verify(properties).revertProperties(
                AdministrationSettings.USER_WORKSPACE_HOME,
                AdministrationSettings.PROJECT_HISTORY_COUNT,
                AdministrationSettings.PROJECT_DETECT_BY_EXCEL_FILES,
                AdministrationSettings.DATE_PATTERN,
                AdministrationSettings.TIME_PATTERN,
                AdministrationSettings.UPDATE_SYSTEM_PROPERTIES,
                AdministrationSettings.TEST_RUN_THREAD_COUNT_PROPERTY,
                org.openl.engine.OpenLSystemProperties.DISPATCHING_VALIDATION,
                AdministrationSettings.AUTO_COMPILE);
    }
}
