package org.openl.rules.webstudio.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.openl.spring.env.DisablePropertySource;

class PropsTest {

    @Test
    void readsWhetherAPropertyIsDisabledPastTheKeyPatterns() {
        // Answers nothing, as openl.config.key-pattern.allowed does for a key it does not list.
        var environment = new StandardEnvironment() {
            @Override
            public String getProperty(String key) {
                return null;
            }
        };
        environment.getPropertySources().addFirst(new MapPropertySource(DisablePropertySource.PROPS_NAME,
                Map.of("_sys_disable_user.mode", "true", "_sys_disable_mail.url", "false")));

        assertTrue(Props.isDisabled(environment, "user.mode"));
        assertFalse(Props.isDisabled(environment, "mail.url"));
        assertFalse(Props.isDisabled(environment, "mail.username"));
    }

    @Test
    void readsWhetherAPropertyIsDisabledFromAnEnvironmentWithoutTheSource() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test",
                Map.of("_sys_disable_user.mode", "true")));

        assertTrue(Props.isDisabled(environment, "user.mode"));
        assertFalse(Props.isDisabled(environment, "mail.url"));
    }

    @Test
    void takesNothingForDisabledWithoutAnEnvironment() {
        assertFalse(Props.isDisabled(null, "user.mode"));
    }
}
