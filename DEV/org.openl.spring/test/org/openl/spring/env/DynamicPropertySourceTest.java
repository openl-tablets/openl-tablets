package org.openl.spring.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

class DynamicPropertySourceTest {

    private static final String CLIENT_SECRET = "security.oauth2.client-secret";
    private static final String CIPHER = "AES/CBC/PKCS5Padding";

    @TempDir
    private Path home;

    @AfterEach
    void forgetTheSource() {
        DynamicPropertySource.register(null);
    }

    @Test
    void storesThePasswordsAndTheSecretsEncrypted() throws IOException {
        var settings = settings(Map.of("secret.key", "MyMasterPassword", "secret.cipher", CIPHER));
        DynamicPropertySource.register(settings);

        settings.save(Map.of(CLIENT_SECRET, "oauth2-client-secret-value",
                "repository.design.password", "MyDbPassword",
                "user.mode", "multi"), CLIENT_SECRET::equals);

        var stored = stored();
        assertTrue(stored.contains(CLIENT_SECRET + "=ENC("), stored);
        assertTrue(stored.contains("repository.design.password=ENC("), stored);
        assertTrue(stored.contains("user.mode=multi"), stored);
        assertEquals("oauth2-client-secret-value", settings.getProperty(CLIENT_SECRET));
    }

    @Test
    void storesTheSecretsAsTypedWithoutASecretKey() throws IOException {
        var settings = settings(Map.of());

        settings.save(Map.of(CLIENT_SECRET, "oauth2-client-secret-value"), CLIENT_SECRET::equals);

        assertTrue(stored().contains(CLIENT_SECRET + "=oauth2-client-secret-value"), stored());
        assertEquals("oauth2-client-secret-value", settings.getProperty(CLIENT_SECRET));
    }

    @Test
    void storesTheSecretKeyAsTypedUnderEitherOfItsNames() throws IOException {
        var settings = settings(Map.of("secret.key", "MyMasterPassword", "secret.cipher", CIPHER));

        settings.save(Map.of("secret.key", "NewKey", "repository.encode.decode.key", "FormerKey"), name -> true);

        assertTrue(stored().contains("secret.key=NewKey"), stored());
        assertTrue(stored().contains("repository.encode.decode.key=FormerKey"), stored());
    }

    @Test
    void readsASecretAsEmptyWithAnotherSecretKeyOrWithoutOne() throws IOException {
        settings(Map.of("secret.key", "MyMasterPassword", "secret.cipher", CIPHER))
                .save(Map.of(CLIENT_SECRET, "oauth2-client-secret-value"), CLIENT_SECRET::equals);

        assertEquals("", readWith(Map.of("secret.key", "AnotherKey", "secret.cipher", CIPHER)));
        assertEquals("", readWith(Map.of()));
    }

    @Test
    void storesNothingWhenASecretCannotBeEncrypted() {
        var settings = settings(Map.of("secret.key", "MyMasterPassword", "secret.cipher", "Unknown"));

        assertThrows(IOException.class, () -> settings.save(Map.of(CLIENT_SECRET, "oauth2-client-secret-value",
                "user.mode", "multi"), CLIENT_SECRET::equals));

        assertFalse(Files.exists(home.resolve("webstudio.properties")));
        assertNull(settings.getProperty(CLIENT_SECRET));
    }

    @Test
    void keepsTheSettingsBeforeABrokenUnicodeEscapeAndLogsTheFailure() throws IOException {
        Files.writeString(home.resolve("webstudio.properties"), "user.mode=multi\npath=C:\\users\n");

        var settings = settings(Map.of());

        assertEquals("multi", settings.getProperty("user.mode"));
        assertNull(settings.getProperty("path"));
    }

    private DynamicPropertySource settings(Map<String, Object> secret) {
        var properties = new HashMap<>(secret);
        properties.put("openl.home.shared", home.toString());
        var sources = new MutablePropertySources();
        sources.addFirst(new MapPropertySource("test", properties));
        return new DynamicPropertySource("webstudio", new FirewallPropertyResolver(sources));
    }

    private String readWith(Map<String, Object> secret) {
        var settings = settings(secret);
        DynamicPropertySource.register(settings);
        return settings.getProperty(CLIENT_SECRET);
    }

    private String stored() throws IOException {
        return Files.readString(home.resolve("webstudio.properties"));
    }
}
