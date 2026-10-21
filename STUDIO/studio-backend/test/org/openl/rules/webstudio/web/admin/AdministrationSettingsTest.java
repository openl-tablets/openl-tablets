package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

import org.openl.config.PropertiesHolder;
import org.openl.engine.OpenLSystemProperties;

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
    void readsAndWritesWhetherTablesAreFormattedOnSave() {
        var properties = mock(PropertiesHolder.class);
        when(properties.getProperty(AdministrationSettings.FORMAT_TABLES_ON_SAVE)).thenReturn("true");
        var settings = new AdministrationSettings();

        settings.load(properties);

        assertTrue(settings.getFormatTablesOnSave());

        settings.setFormatTablesOnSave(false);
        settings.store(properties);
        verify(properties).setProperty(AdministrationSettings.FORMAT_TABLES_ON_SAVE, false);
    }

    @Test
    void readsAndWritesTheSettingsOfTheAuditLog() {
        var properties = mock(PropertiesHolder.class);
        when(properties.getProperty(AdministrationSettings.AUDIT_RETENTION)).thenReturn("1y");
        when(properties.getProperty(AdministrationSettings.AUDIT_OUTPUTS_CONSOLE)).thenReturn("true");
        var settings = new AdministrationSettings();

        settings.load(properties);

        assertEquals("1y", settings.getAuditRetention());
        assertTrue(settings.getAuditOutputsConsole());

        settings.setAuditRetention("");
        settings.setAuditOutputsConsole(false);
        settings.store(properties);
        verify(properties).setProperty(AdministrationSettings.AUDIT_RETENTION, "");
        verify(properties).setProperty(AdministrationSettings.AUDIT_OUTPUTS_CONSOLE, false);
    }

    @Test
    void refusesARetentionOfTheAuditLogThatIsNoPeriodLongerThanZero() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var settings = new AdministrationSettings();

            settings.setAuditRetention("90");
            assertEquals(List.of("""
                            Write a period longer than zero with its unit, such as 90d, 2w, 6m or 1y, or in \
                            ISO 8601, such as P1Y6M."""),
                    validator.validateProperty(settings, "auditRetention").stream()
                            .map(ConstraintViolation::getMessage)
                            .toList());

            settings.setAuditRetention("90d");
            assertTrue(validator.validateProperty(settings, "auditRetention").isEmpty());
        }
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
                AdministrationSettings.FORMAT_TABLES_ON_SAVE,
                AdministrationSettings.TEST_RUN_THREAD_COUNT_PROPERTY,
                OpenLSystemProperties.DISPATCHING_VALIDATION,
                AdministrationSettings.AUTO_COMPILE,
                AdministrationSettings.AUDIT_RETENTION,
                AdministrationSettings.AUDIT_OUTPUTS_CONSOLE);
    }
}
