package org.openl.rules.webstudio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.install.KeyPairCertUtils;

class MigratorSecretsTest {

    private static final String LOCAL_KEY = "security.saml.local-key";
    private static final String LOCAL_CERTIFICATE = "security.saml.local-certificate";

    @AfterEach
    void forgetTheSettings() {
        Props.setEnvironment(null);
    }

    @Test
    void generatesTheKeyPairAgainWhenTheKeyCannotBeRead() {
        var props = generated(Map.of(LOCAL_KEY, "rUcXIEWOplL61t+83kqiy4", LOCAL_CERTIFICATE, "certificate"));

        assertNotNull(props.get(LOCAL_KEY));
        assertNotEquals("rUcXIEWOplL61t+83kqiy4", props.get(LOCAL_KEY));
        assertNotEquals("certificate", props.get(LOCAL_CERTIFICATE));
    }

    @Test
    void keepsAReadableKeyPair() {
        var pair = KeyPairCertUtils.generateCertificate();

        assertTrue(generated(Map.of(LOCAL_KEY, pair.getKey(), LOCAL_CERTIFICATE, pair.getValue())).isEmpty());
    }

    @Test
    void takesTheSecretsStoredInPlainTextUnlessAMigrationRemovesThem() {
        var stored = Map.of("security.oauth2.client-secret", "client-secret-value",
                "repository.design.password", "password-value",
                "security.saml.keystore-password", "keystore-password-value",
                "mail.password", "ENC(Cc7uqRnKsEouhPWI6DKf7x9yO5t/oiLfF1MOL2nbV64=) ",
                "user.mode", "oauth2");
        var props = new HashMap<String, String>();
        props.put("security.saml.keystore-password", null);

        Migrator.plainSecretsTo710(stored, props);

        var expected = new HashMap<String, String>();
        expected.put("security.oauth2.client-secret", "client-secret-value");
        expected.put("repository.design.password", "password-value");
        expected.put("security.saml.keystore-password", null);
        assertEquals(expected, props);
    }

    private static Map<String, String> generated(Map<String, Object> keyPair) {
        var settings = new HashMap<>(keyPair);
        settings.put("user.mode", "saml");
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("settings", settings));
        Props.setEnvironment(environment);
        var props = new HashMap<String, String>();
        Migrator.generateSamlKeyPair(props);
        return props;
    }
}
