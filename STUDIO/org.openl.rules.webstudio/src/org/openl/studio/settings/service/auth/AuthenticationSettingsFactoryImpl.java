package org.openl.studio.settings.service.auth;

import lombok.RequiredArgsConstructor;

import org.openl.config.InMemoryProperties;
import org.openl.rules.webstudio.web.admin.security.ADAuthenticationSettings;
import org.openl.rules.webstudio.web.admin.security.AuthenticationSettings;
import org.openl.rules.webstudio.web.admin.security.InheritedAuthenticationSettings;
import org.openl.rules.webstudio.web.admin.security.NOPAuthenticationSettings;
import org.openl.rules.webstudio.web.admin.security.OAuth2AuthenticationSettings;
import org.openl.rules.webstudio.web.admin.security.SAMLAuthenticationSettings;
import org.openl.rules.webstudio.web.admin.security.UserMode;

@RequiredArgsConstructor
public class AuthenticationSettingsFactoryImpl implements AuthenticationSettingsFactory {

    private final InMemoryProperties properties;

    @Override
    public AuthenticationSettings initialize() {
        var userMode = UserMode.fromValue(properties.getProperty(AuthenticationSettings.USER_MODE));
        return create(userMode);
    }

    @Override
    public AuthenticationSettings create(UserMode userMode) {
        return switch (userMode) {
            case SINGLE -> new NOPAuthenticationSettings();
            case MULTI -> new InheritedAuthenticationSettings();
            case AD -> new ADAuthenticationSettings();
            case SAML -> new SAMLAuthenticationSettings();
            case OAUTH2 -> new OAuth2AuthenticationSettings();
        };
    }
}
