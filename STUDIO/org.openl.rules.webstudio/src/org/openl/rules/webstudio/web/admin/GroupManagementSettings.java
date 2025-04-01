package org.openl.rules.webstudio.web.admin;

import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.config.PropertiesHolder;
import org.openl.rules.rest.settings.model.SettingValueWrapper;

public class GroupManagementSettings implements SettingsHolder {

    public static final String SECURITY_DEF_GROUP_PROP = "security.default-group";

    @Size(max = 65)
    @Parameter(description = "Default user group")
    @Schema(oneOf = {String.class, SettingValueWrapper.class})
    @SettingPropertyName(SECURITY_DEF_GROUP_PROP)
    private String defaultGroup;

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    @Override
    public void load(PropertiesHolder properties) {
        defaultGroup = properties.getProperty(SECURITY_DEF_GROUP_PROP);
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(SECURITY_DEF_GROUP_PROP, defaultGroup);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(SECURITY_DEF_GROUP_PROP);
        load(properties);
    }
}
