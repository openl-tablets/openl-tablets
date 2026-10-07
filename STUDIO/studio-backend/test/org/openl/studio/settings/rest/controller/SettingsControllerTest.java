package org.openl.studio.settings.rest.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests for {@link SettingsController}.
 *
 * @author Yury Molchan
 */
class SettingsControllerTest {

    private static final BooleanSupplier MAIL_DISABLED = () -> false;

    private boolean isPersonalAccessTokenEnabled(String userMode) {
        var environment = new MockEnvironment();
        if (userMode != null) {
            environment.setProperty("user.mode", userMode);
        }
        var settings = new SettingsController(environment, MAIL_DISABLED).getSettings();
        return settings.getSupportedFeatures().getPersonalAccessToken();
    }

    @Test
    void personalAccessTokenEnabledForEveryAuthenticatedMode() {
        assertTrue(isPersonalAccessTokenEnabled("oauth2"));
        assertTrue(isPersonalAccessTokenEnabled("saml"));
        assertTrue(isPersonalAccessTokenEnabled("ad"));
        assertTrue(isPersonalAccessTokenEnabled("multi"));
    }

    @Test
    void personalAccessTokenEnabledForAnyModeOtherThanSingle() {
        // The feature is gated by "user.mode != single", so any non-single mode enables it.
        assertTrue(isPersonalAccessTokenEnabled("some-future-mode"));
    }

    @Test
    void personalAccessTokenDisabledForSingleMode() {
        assertFalse(isPersonalAccessTokenEnabled("single"));
    }

    @Test
    void personalAccessTokenDisabledWhenModeIsNotConfigured() {
        assertFalse(isPersonalAccessTokenEnabled(null));
    }
}
