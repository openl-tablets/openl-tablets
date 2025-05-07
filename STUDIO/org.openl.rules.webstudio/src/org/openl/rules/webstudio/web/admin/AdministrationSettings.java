package org.openl.rules.webstudio.web.admin;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.config.PropertiesHolder;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.rest.settings.model.SettingValueWrapper;
import org.openl.rules.rest.settings.model.validation.DatePatternConstraint;
import org.openl.rules.rest.settings.model.validation.TimePatternConstraint;
import org.openl.rules.rest.settings.model.validation.WriteDirectoryConstraint;

/**
 * The class contains system settings property names for settings edited in "Administration" page.
 *
 * @author NSamatov
 */
public final class AdministrationSettings implements SettingsHolder {
    private static final Collection<String> allSettings;

    public static final String USER_WORKSPACE_HOME = "user.workspace.home";
    public static final String PROJECT_HISTORY_COUNT = "project.history.count";
    public static final String DATE_PATTERN = "data.format.date";
    public static final String TIME_PATTERN = "data.format.time";
    public static final String DATETIME_PATTERN = "data.format.datetime";
    public static final String UPDATE_SYSTEM_PROPERTIES = "update.system.properties";
    public static final String DESIGN_REPOSITORY_CONFIGS = "design-repository-configs";
    public static final String PRODUCTION_REPOSITORY_CONFIGS = "production-repository-configs";

    public static final String TEST_RUN_THREAD_COUNT_PROPERTY = "test.run.thread.count";
    public static final String AUTO_COMPILE = "compile.auto";

    @Parameter(description = "User Workspace Home.")
    @Schema(oneOf = {String.class, SettingValueWrapper.class})
    @WriteDirectoryConstraint(directoryType = "Workspace Directory")
    @SettingPropertyName(USER_WORKSPACE_HOME)
    private String userWorkspaceHome;

    @Parameter(description = "The maximum count of saved changes for each project per user.")
    @Schema(oneOf = {Integer.class, SettingValueWrapper.class})
    @Min(0)
    @SettingPropertyName(PROJECT_HISTORY_COUNT)
    private Integer projectHistoryCount;

    @Parameter(description = "Update table properties ('createdOn', 'modifiedBy' etc.) on editing.")
    @Schema(oneOf = {Boolean.class, SettingValueWrapper.class})
    @SettingPropertyName(UPDATE_SYSTEM_PROPERTIES)
    private Boolean updateSystemProperties;

    @Parameter(description = "Date Format.")
    @Schema(oneOf = {String.class, SettingValueWrapper.class})
    @NotBlank
    @DatePatternConstraint
    @SettingPropertyName(DATE_PATTERN)
    private String datePattern;

    @Parameter(description = "Time Format.")
    @Schema(oneOf = {String.class, SettingValueWrapper.class})
    @NotBlank
    @TimePatternConstraint
    @SettingPropertyName(TIME_PATTERN)
    private String timeFormat;

    @Parameter(description = "Thread number for tests.")
    @Schema(oneOf = {Integer.class, SettingValueWrapper.class})
    @Min(1)
    @SettingPropertyName(TEST_RUN_THREAD_COUNT_PROPERTY)
    private Integer testRunThreadCount;

    @Parameter(description = "Turn on/off the Dispatching Validation feature.")
    @Schema(oneOf = {Boolean.class, SettingValueWrapper.class})
    @SettingPropertyName(OpenLSystemProperties.DISPATCHING_VALIDATION)
    private Boolean dispatchingValidationEnabled;

    @Parameter(description = "Turn on/off verification on edit.")
    @Schema(oneOf = {Boolean.class, SettingValueWrapper.class})
    @SettingPropertyName(AUTO_COMPILE)
    private Boolean autoCompile;

    static {
        List<String> settingNames = new ArrayList<>();

        for (Field field : AdministrationSettings.class.getFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                try {
                    Object value = field.get(null);
                    if (value instanceof String) {
                        settingNames.add((String) value);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        settingNames.add(OpenLSystemProperties.DISPATCHING_VALIDATION);

        allSettings = Collections.unmodifiableCollection(settingNames);
    }

    /**
     * Get all settings edited in "Administration" page
     *
     * @return setting property names edited in "Administration" page
     */
    public static Collection<String> getAllSettings() {
        return allSettings;
    }

    public AdministrationSettings() {
    }


    public String getUserWorkspaceHome() {
        return userWorkspaceHome;
    }

    public void setUserWorkspaceHome(String userWorkspaceHome) {
        this.userWorkspaceHome = userWorkspaceHome;
    }

    public Integer getProjectHistoryCount() {
        return projectHistoryCount;
    }

    public void setProjectHistoryCount(Integer projectHistoryCount) {
        this.projectHistoryCount = projectHistoryCount;
    }

    public Boolean getUpdateSystemProperties() {
        return updateSystemProperties;
    }

    public void setUpdateSystemProperties(Boolean updateSystemProperties) {
        this.updateSystemProperties = updateSystemProperties;
    }

    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    public Integer getTestRunThreadCount() {
        return testRunThreadCount;
    }

    public void setTestRunThreadCount(Integer testRunThreadCount) {
        this.testRunThreadCount = testRunThreadCount;
    }

    public Boolean getDispatchingValidationEnabled() {
        return dispatchingValidationEnabled;
    }

    public void setDispatchingValidationEnabled(Boolean dispatchingValidationEnabled) {
        this.dispatchingValidationEnabled = dispatchingValidationEnabled;
    }

    public Boolean getAutoCompile() {
        return autoCompile;
    }

    public void setAutoCompile(Boolean autoCompile) {
        this.autoCompile = autoCompile;
    }

    @Override
    public void load(PropertiesHolder properties) {
        userWorkspaceHome = properties.getProperty(USER_WORKSPACE_HOME);
        projectHistoryCount = Optional.ofNullable(properties.getProperty(PROJECT_HISTORY_COUNT))
                .map(Integer::parseInt)
                .orElse(null);
        datePattern = properties.getProperty(DATE_PATTERN);
        timeFormat = properties.getProperty(TIME_PATTERN);
        updateSystemProperties = Optional.ofNullable(properties.getProperty(UPDATE_SYSTEM_PROPERTIES))
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
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(USER_WORKSPACE_HOME, userWorkspaceHome);
        properties.setProperty(PROJECT_HISTORY_COUNT, projectHistoryCount);
        properties.setProperty(DATE_PATTERN, datePattern);
        properties.setProperty(TIME_PATTERN, timeFormat);
        properties.setProperty(UPDATE_SYSTEM_PROPERTIES, updateSystemProperties);
        properties.setProperty(TEST_RUN_THREAD_COUNT_PROPERTY, testRunThreadCount);
        properties.setProperty(OpenLSystemProperties.DISPATCHING_VALIDATION, dispatchingValidationEnabled);
        properties.setProperty(AUTO_COMPILE, autoCompile);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(USER_WORKSPACE_HOME,
                PROJECT_HISTORY_COUNT,
                DATE_PATTERN,
                TIME_PATTERN,
                UPDATE_SYSTEM_PROPERTIES,
                TEST_RUN_THREAD_COUNT_PROPERTY,
                OpenLSystemProperties.DISPATCHING_VALIDATION,
                AUTO_COMPILE);

        load(properties);
    }
}
