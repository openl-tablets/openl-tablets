package org.openl.rules.webstudio.web.admin.security;

import java.util.Optional;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.webstudio.web.admin.SettingsHolder;
import org.openl.studio.settings.converter.SettingPropertyName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "userMode", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NOPAuthenticationSettings.class, name = "single"),
        @JsonSubTypes.Type(value = InheritedAuthenticationSettings.class, name = "multi"),
        @JsonSubTypes.Type(value = ADAuthenticationSettings.class, name = "ad"),
        @JsonSubTypes.Type(value = SAMLAuthenticationSettings.class, name = "saml"),
        @JsonSubTypes.Type(value = OAuth2AuthenticationSettings.class, name = "oauth2")
})
public abstract class AuthenticationSettings implements SettingsHolder {

    public static final String USER_MODE = "user.mode";
    private static final String ALLOW_PROJECT_CREATE_DELETE = "security.allow-project-create-delete";
    private static final String ALLOW_BYPASS_PROTECTED_BRANCHES = "security.allow-bypass-protected-branches";

    @Parameter(description = "Authentication mode.", example = "single")
    @SettingPropertyName(USER_MODE)
    @NotNull
    private UserMode userMode;

    @Parameter(description = "Global permission to create and delete projects")
    @SettingPropertyName(ALLOW_PROJECT_CREATE_DELETE)
    private boolean allowProjectCreateDelete;

    @Parameter(description = "Allow users with the Manager role to bypass protected branch restrictions")
    @SettingPropertyName(ALLOW_BYPASS_PROTECTED_BRANCHES)
    private boolean allowBypassProtectedBranches;

    @Override
    public void load(PropertiesHolder properties) {
        userMode = UserMode.fromValue(properties.getProperty(USER_MODE));
        allowProjectCreateDelete = Optional.ofNullable(properties.getProperty(ALLOW_PROJECT_CREATE_DELETE))
                .map(Boolean::parseBoolean)
                .orElse(false);
        allowBypassProtectedBranches = Optional.ofNullable(properties.getProperty(ALLOW_BYPASS_PROTECTED_BRANCHES))
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(USER_MODE, userMode.getValue());
        properties.setProperty(ALLOW_PROJECT_CREATE_DELETE, Boolean.toString(allowProjectCreateDelete));
        properties.setProperty(ALLOW_BYPASS_PROTECTED_BRANCHES, Boolean.toString(allowBypassProtectedBranches));
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(USER_MODE, ALLOW_PROJECT_CREATE_DELETE, ALLOW_BYPASS_PROTECTED_BRANCHES);
        load(properties);
    }

    public UserMode getUserMode() {
        return userMode;
    }

    public void setUserMode(UserMode userMode) {
        this.userMode = userMode;
    }

    public boolean isAllowProjectCreateDelete() {
        return allowProjectCreateDelete;
    }

    public void setAllowProjectCreateDelete(boolean allowProjectCreateDelete) {
        this.allowProjectCreateDelete = allowProjectCreateDelete;
    }

    public boolean isAllowBypassProtectedBranches() {
        return allowBypassProtectedBranches;
    }

    public void setAllowBypassProtectedBranches(boolean allowBypassProtectedBranches) {
        this.allowBypassProtectedBranches = allowBypassProtectedBranches;
    }

}
