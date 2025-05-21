package org.openl.rules.webstudio.web.admin.security;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.openl.config.PropertiesHolder;

public class NOPAuthenticationSettings extends AuthenticationSettings {

    @NotNull
    @Valid
    private NOPUserSettings user;

    @Override
    public void load(PropertiesHolder properties) {
        super.load(properties);
        user = new NOPUserSettings();
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
