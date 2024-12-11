package org.openl.rules.webstudio.web.admin.security;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.config.PropertiesHolder;
import org.openl.rules.webstudio.web.admin.SettingPropertyName;
import org.openl.rules.webstudio.web.admin.SettingsHolder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "userMode", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NOPAuthenticationSettings.class, name = "single"),
        @JsonSubTypes.Type(value = InheritedAuthenticationSettings.class, name = "multi"),
        @JsonSubTypes.Type(value = ADAuthenticationSettings.class, name = "ad"),
        @JsonSubTypes.Type(value = SAMLAuthenticationSettings.class, name = "saml"),
        @JsonSubTypes.Type(value = OAuth2AuthenticationSettings.class, name = "oauth2")
})
@Schema(oneOf = {
        InheritedAuthenticationSettings.class,
        ADAuthenticationSettings.class,
        SAMLAuthenticationSettings.class,
        OAuth2AuthenticationSettings.class
})
public abstract class AuthenticationSettings implements SettingsHolder {

    public static final String USER_MODE = "user.mode";

    @Parameter(description = "Authentication mode.", example = "single")
    @SettingPropertyName(USER_MODE)
    @NotNull
    private UserMode userMode;

    @Override
    public void load(PropertiesHolder properties) {
        userMode = UserMode.fromValue(properties.getProperty(USER_MODE));
    }

    @Override
    public void store(PropertiesHolder properties) {
        properties.setProperty(USER_MODE, userMode.getValue());
    }

    @Override
    public void revert(PropertiesHolder properties) {
        properties.revertProperties(USER_MODE);
        load(properties);
    }

    public UserMode getUserMode() {
        return userMode;
    }

    public void setUserMode(UserMode userMode) {
        this.userMode = userMode;
    }

}
