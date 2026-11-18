package org.openl.rules.webstudio.web.admin.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.openl.config.InMemoryProperties;
import org.openl.rules.webstudio.web.install.KeyPairCertUtils;

class SAMLAuthenticationSettingsTest {

    private static final String LOCAL_KEY = "security.saml.local-key";
    private static final String LOCAL_CERTIFICATE = "security.saml.local-certificate";

    @Test
    void generatesTheKeyPairAgainWhenTheKeyCannotBeRead() {
        var properties = stored(Map.of(LOCAL_KEY, "", LOCAL_CERTIFICATE, "certificate"));

        assertFalse(properties.getProperty(LOCAL_KEY).isBlank());
        assertNotEquals("certificate", properties.getProperty(LOCAL_CERTIFICATE));
    }

    @Test
    void keepsAReadableKeyPair() {
        var pair = KeyPairCertUtils.generateCertificate();

        var properties = stored(Map.of(LOCAL_KEY, pair.getKey(), LOCAL_CERTIFICATE, pair.getValue()));

        assertEquals(pair.getKey(), properties.getProperty(LOCAL_KEY));
        assertEquals(pair.getValue(), properties.getProperty(LOCAL_CERTIFICATE));
    }

    private static InMemoryProperties stored(Map<String, Object> keyPair) {
        var settings = new HashMap<>(keyPair);
        settings.put("user.mode", "saml");
        settings.put("security.administrators", "admin");
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("settings", settings));
        var properties = new InMemoryProperties(environment);
        var saml = new SAMLAuthenticationSettings();
        saml.load(properties);
        saml.store(properties);
        return properties;
    }
}
