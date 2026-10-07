package org.openl.studio.settings.service.auth;

import java.util.function.Supplier;

import org.openl.rules.webstudio.web.admin.security.AuthenticationSettings;
import org.openl.rules.webstudio.web.admin.security.UserMode;

public interface AuthenticationSettingsFactory extends Supplier<AuthenticationSettings> {

    /**
     * Factory method to instantiate appropriate UserModeSettings subclass based on 'user.mode' property.
     *
     * @return an instance of AuthenticationSettings
     */
    AuthenticationSettings initialize();

    @Override
    default AuthenticationSettings get() {
        return initialize();
    }

    AuthenticationSettings create(UserMode userMode);
}
