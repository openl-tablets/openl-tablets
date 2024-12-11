package org.openl.rules.webstudio.web.admin.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonMerge;

import org.openl.config.PropertiesHolder;

public class NOPAuthenticationSettings extends AuthenticationSettings {

    @NotNull
    @Valid
    @JsonMerge
    private NOPUserSettings user;

    public NOPAuthenticationSettings() {
        user = new NOPUserSettings();
    }

    @Override
    public void load(PropertiesHolder properties) {
        super.load(properties);
        user.load(properties);
    }

    @Override
    public void store(PropertiesHolder properties) {
        super.store(properties);
        user.store(properties);
    }

    @Override
    public void revert(PropertiesHolder properties) {
        user.revert(properties);
        super.revert(properties);
    }

    public NOPUserSettings getUser() {
        return user;
    }

    public void setUser(NOPUserSettings user) {
        this.user = user;
    }
}
