package org.openl.rules.webstudio.web.admin;


import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonMerge;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

import org.openl.config.PropertiesHolder;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.repository.IgnoredPaths;
import org.openl.studio.settings.converter.SettingPropertyName;
import org.openl.studio.settings.model.constraint.PeriodConstraint;

/**
 * The class contains system settings property names for settings edited in "Administration" page.
 *
 * @author NSamatov
 */
public final class AdministrationSettings implements SettingsHolder {

    public static final String USER_WORKSPACE_HOME = "user.workspace.home";
    public static final String PROJECT_HISTORY_COUNT = "project.history.count";
    public static final String PROJECT_DETECT_BY_EXCEL_FILES = "project.detect-by-excel-files";
    public static final String UPDATE_SYSTEM_PROPERTIES = "update.system.properties";
    public static final String FORMAT_TABLES_ON_SAVE = "table.format-on-save";
    public static final String DESIGN_REPOSITORY_CONFIGS = "design-repository-configs";
    public static final String PRODUCTION_REPOSITORY_CONFIGS = "production-repository-configs";

    public static final String TEST_RUN_THREAD_COUNT_PROPERTY = "test.run.thread.count";
    public static final String AUTO_COMPILE = "compile.auto";
    public static final String AUDIT_RETENTION = "audit.retention";
    public static final String AUDIT_OUTPUTS_CONSOLE = "audit.outputs.console";

    @Getter
    @Parameter(description = "The maximum count of saved changes for each project per user.")
    @Setter
    @Min(0)
    @SettingPropertyName(PROJECT_HISTORY_COUNT)
    private Integer projectHistoryCount;

    @Getter
    @Parameter(description = "Whether folders without rules.xml can be discovered as projects from Excel files in their root. Enabling this setting slows down the Repository tab.")
    @Setter
    @SettingPropertyName(PROJECT_DETECT_BY_EXCEL_FILES)
    private Boolean detectProjectsByExcelFiles;

    @Getter
    @Parameter(description = """
            The paths no repository shows or changes, such as build files or the files of a version \
            control system: Ant-style patterns relative to the repository root, separated by commas. The .git \
            folder is always ignored.""")
    @Setter
    @SettingPropertyName(IgnoredPaths.PROPERTY)
    private String ignoredPaths;

    @Getter
    @Parameter(description = "Update table properties ('createdOn', 'modifiedBy' etc.) on editing.")
    @Setter
    @SettingPropertyName(UPDATE_SYSTEM_PROPERTIES)
    private Boolean updateSystemProperties;

    @Getter
    @Parameter(description = """
            Format a table with the OpenL table style every time it is saved, created or copied, \
            and every Excel file uploaded into a project. While it is on, a table edit sets no styling but the \
            indent.""")
    @Setter
    @SettingPropertyName(FORMAT_TABLES_ON_SAVE)
    private Boolean formatTablesOnSave;

    @Getter
    @Parameter(description = "Thread number for tests.")
    @Setter
    @Min(1)
    @SettingPropertyName(TEST_RUN_THREAD_COUNT_PROPERTY)
    private Integer testRunThreadCount;

    @Getter
    @Parameter(description = "Turn on/off the Dispatching Validation feature.")
    @Setter
    @SettingPropertyName(OpenLSystemProperties.DISPATCHING_VALIDATION)
    private Boolean dispatchingValidationEnabled;

    @Getter
    @Parameter(description = "Turn on/off verification on edit.")
    @Setter
    @SettingPropertyName(AUTO_COMPILE)
    private Boolean autoCompile;

    @Getter
    @Parameter(description = """
            How long the audit log keeps an event: a number with its unit, such as 90d, 2w, 6m or 1y, or a period \
            in ISO 8601, such as P1Y6M. A number without its unit is refused. Empty keeps the events forever.""")
    @Setter
    @PeriodConstraint
    @SettingPropertyName(AUDIT_RETENTION)
    private String auditRetention;

    @Getter
    @Parameter(description = "Copy every event of the audit log to the server log, by the OpenL.audit logger.")
    @Setter
    @SettingPropertyName(AUDIT_OUTPUTS_CONSOLE)
    private Boolean auditOutputsConsole;

    @Getter
    @Valid
    @NotNull
    @Setter
    @JsonMerge
    private DBSettings db;

    public AdministrationSettings() {
        db = new DBSettings();
    }

    @Override
    public void load(PropertiesHolder properties) {
        projectHistoryCount = Optional.ofNullable(properties.getProperty(PROJECT_HISTORY_COUNT))
                .map(Integer::parseInt)
                .orElse(null);
        detectProjectsByExcelFiles = Optional.ofNullable(properties.getProperty(PROJECT_DETECT_BY_EXCEL_FILES))
                .map(Boolean::parseBoolean)
                .orElse(false);
        ignoredPaths = properties.getProperty(IgnoredPaths.PROPERTY);
        updateSystemProperties = Optional.ofNullable(properties.getProperty(UPDATE_SYSTEM_PROPERTIES))
                .map(Boolean::parseBoolean)
                .orElse(null);
        formatTablesOnSave = Optional.ofNullable(properties.getProperty(FORMAT_TABLES_ON_SAVE))
                .map(Boolean::parseBoolean)
                .orElse(null);
        testRunThreadCount = Optional.ofNullable(properties.getProperty(TEST_RUN_THREAD_COUNT_PROPERTY))
                .map(Integer::parseInt)
                .orElse(null);
        dispatchingValidationEnabled = Optional.ofNullable(properties.getProperty(OpenLSystemProperties.DISPATCHING_VALIDATION))
                .map(Boolean::parseBoolean)
                .orElse(null);
        autoCompile = Optional.ofNullable(properties.getProperty(AUTO_COMPILE))
                .map(Boolean::parseBoolean)
                .orElse(null);
        auditRetention = properties.getProperty(AUDIT_RETENTION);
        auditOutputsConsole = Optional.ofNullable(properties.getProperty(AUDIT_OUTPUTS_CONSOLE))
                .map(Boolean::parseBoolean)
                .orElse(null);
        db.load(properties);
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(PROJECT_HISTORY_COUNT, projectHistoryCount);
        properties.setProperty(PROJECT_DETECT_BY_EXCEL_FILES, detectProjectsByExcelFiles);
        properties.setProperty(IgnoredPaths.PROPERTY, ignoredPaths);
        properties.setProperty(UPDATE_SYSTEM_PROPERTIES, updateSystemProperties);
        properties.setProperty(FORMAT_TABLES_ON_SAVE, formatTablesOnSave);
        properties.setProperty(TEST_RUN_THREAD_COUNT_PROPERTY, testRunThreadCount);
        properties.setProperty(OpenLSystemProperties.DISPATCHING_VALIDATION, dispatchingValidationEnabled);
        properties.setProperty(AUTO_COMPILE, autoCompile);
        properties.setProperty(AUDIT_RETENTION, auditRetention);
        properties.setProperty(AUDIT_OUTPUTS_CONSOLE, auditOutputsConsole);
        db.store(properties);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(USER_WORKSPACE_HOME,
                PROJECT_HISTORY_COUNT,
                PROJECT_DETECT_BY_EXCEL_FILES,
                IgnoredPaths.PROPERTY,
                UPDATE_SYSTEM_PROPERTIES,
                FORMAT_TABLES_ON_SAVE,
                TEST_RUN_THREAD_COUNT_PROPERTY,
                OpenLSystemProperties.DISPATCHING_VALIDATION,
                AUTO_COMPILE,
                AUDIT_RETENTION,
                AUDIT_OUTPUTS_CONSOLE);
        db.revert(properties);
        load(properties);
    }
}
