package org.openl.studio.settings.converter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.openl.rules.webstudio.web.Props;
import org.openl.spring.env.DisablePropertySource;
import org.openl.studio.common.exception.BadRequestException;

class ReadOnlySettingsTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String ANSWERED = """
            {
              "name": {"value": "Design", "readOnly": true},
              "uri": "/repositories/design",
              "settings": {
                "basePath": {"value": "DESIGN/rules", "readOnly": true},
                "connectionTimeout": {"value": 120, "readOnly": true},
                "login": {"readOnly": true},
                "password": {"secret": true, "readOnly": true},
                "token": {"secret": true, "value": "***"}
              }
            }
            """;

    private static void check(String requested) throws Exception {
        ReadOnlySettings.check(JSON.readTree(ANSWERED), JSON.readTree(requested));
    }

    private static Object[] refused(String requested) {
        var error = assertThrows(BadRequestException.class, () -> check(requested));
        assertEquals("openl.error.400.settings.read-only.message", error.getErrorCode());
        return error.getArgs();
    }

    @Test
    void acceptsTheSettingsAsTheyAreAnswered() {
        assertDoesNotThrow(() -> check("""
                {"name": "Design", "uri": "/other", "settings": {"basePath": "DESIGN/rules", "connectionTimeout": 120,
                 "login": "", "token": "new"}}
                """));
    }

    @Test
    void acceptsTheSettingsTheRequestLeavesOut() {
        assertDoesNotThrow(() -> check("{}"));
        assertDoesNotThrow(() -> check("{\"settings\": {}}"));
    }

    @Test
    void comparesTheValuesAsText() {
        assertDoesNotThrow(() -> check("{\"settings\": {\"connectionTimeout\": \"120\"}}"));
    }

    @Test
    void takesAnEmptyValueForNoValue() {
        assertDoesNotThrow(() -> check("{\"settings\": {\"login\": null}}"));
        assertDoesNotThrow(() -> check("{\"settings\": {\"login\": \"\"}}"));
    }

    @Test
    void acceptsTheValueOfAWrappedRequest() {
        assertDoesNotThrow(() -> check("{\"name\": {\"value\": \"Design\", \"readOnly\": true}}"));
    }

    @Test
    void refusesEveryChangedReadOnlySettingByItsPath() {
        assertArrayEquals(new Object[]{"name, settings.basePath, settings.connectionTimeout, settings.login"},
                refused("""
                        {"name": "Rules", "settings": {"basePath": "DESIGN/rules/", "connectionTimeout": 30,
                         "login": "admin"}}
                        """));
    }

    @Test
    void takesAResetReadOnlySettingForUnchanged() {
        assertDoesNotThrow(() -> check("{\"settings\": {\"connectionTimeout\": null}}"));
        assertDoesNotThrow(() -> check("{\"settings\": {\"connectionTimeout\": {\"readOnly\": true}}}"));
    }

    @Test
    void refusesAReadOnlySecretTheRequestSets() {
        assertArrayEquals(new Object[]{"settings.password"}, refused("{\"settings\": {\"password\": \"secret\"}}"));
    }

    @Test
    void refusesAChangedValueOfAWrappedRequest() {
        assertArrayEquals(new Object[]{"name"}, refused("{\"name\": {\"value\": \"Rules\", \"readOnly\": true}}"));
    }

    @Test
    void comparesTheValueOfASecretTheEnvironmentSets() throws Exception {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(DisablePropertySource.PROPS_NAME,
                Map.of("_sys_disable_mail.password", "true")));
        Props.setEnvironment(environment);
        try {
            var answered = ReadOnlySettings.answerOf(JSON, new MailSettings());

            assertEquals("s3cret", answered.path("password").path("value").asText());
            assertTrue(JSON.valueToTree(new MailSettings()).path("password").path("value").isNull());
            var unchanged = JSON.readTree("{\"password\": \"s3cret\"}");
            var changed = JSON.readTree("{\"password\": \"other\"}");
            assertDoesNotThrow(() -> ReadOnlySettings.check(answered, unchanged));
            assertThrows(BadRequestException.class, () -> ReadOnlySettings.check(answered, changed));
        } finally {
            Props.setEnvironment(null);
        }
    }

    /** A setting model with a secret setting. */
    static class MailSettings {

        @SettingPropertyName(value = "mail.password", secret = true)
        public String getPassword() {
            return "s3cret";
        }
    }

    @Test
    void walksOnlyTheGroupsBothSidesHold() throws Exception {
        JsonNode answered = JSON.readTree(ANSWERED);

        assertDoesNotThrow(() -> ReadOnlySettings.check(answered, JSON.readTree("{\"settings\": \"none\"}")));
    }
}
