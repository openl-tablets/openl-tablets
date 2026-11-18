package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;

@DisabledIfSystemProperty(named = "noDocker", matches = ".*")
class EncryptedSecretsTest extends AbstractKeycloakTest {

    private static final String ACCESS_KEY = "s3-access-key-value";
    private static final String SECRET_KEY = "s3-secret-key-value";
    private static final String LOCAL_KEY = "security.saml.local-key=";

    @Test
    void storesTheSecretsEncrypted(@TempDir Path home, @TempDir Path homeWithoutKey) throws Exception {
        try (var keycloak = keycloak();
             var s3 = new S3MockContainer("latest")) {
            keycloak.start();
            s3.start();
            var authServerUrl = keycloak.getAuthServerUrl();
            try (var client = studio(s3, home).withInitParam("secret.key", "FirstKey").start()) {
                client.postForObject("/rest/admin/settings/repos/batch", Map.of("production", Map.of("createOrUpdate",
                        List.of(Map.of("id", "production-s3",
                                "name", "Deployment S3",
                                "type", "repo-aws-s3",
                                "settings", Map.of("serviceEndpoint", s3.getHttpEndpoint(),
                                        "bucketName", "openl-test-deploy",
                                        "regionName", "us-east-1",
                                        "accessKey", ACCESS_KEY,
                                        "secretKey", SECRET_KEY))))));
                OAuthTest.initStudio(client, authServerUrl);

                var stored = stored(home);
                assertTrue(stored.contains("security.oauth2.client-secret=ENC("), stored);
                assertTrue(stored.contains("repository.production-s3.access-key=ENC("), stored);
                assertTrue(stored.contains("repository.production-s3.secret-key=ENC("), stored);
                for (var secret : List.of(OAuthTest.CLIENT_SECRET, ACCESS_KEY, SECRET_KEY)) {
                    assertFalse(stored.contains(secret), stored);
                }
                assertSecretsWork(client);
            }
            try (var client = studio(s3, home).withInitParam("secret.key", "FirstKey").start()) {
                assertSecretsWork(client);
            }
            try (var client = studio(s3, homeWithoutKey).start()) {
                OAuthTest.initStudio(client, authServerUrl);

                var stored = stored(homeWithoutKey);
                assertTrue(stored.contains("security.oauth2.client-secret=" + OAuthTest.CLIENT_SECRET), stored);
            }
            var settingsFile = homeWithoutKey.resolve(".properties");
            Files.writeString(settingsFile, stored(homeWithoutKey).replaceAll("(?m)^\\.version=.*$", ".version=7.0.0"));
            try (var client = studio(s3, homeWithoutKey).withInitParam("secret.key", "FirstKey").start()) {
                var stored = stored(homeWithoutKey);
                assertTrue(stored.contains("security.oauth2.client-secret=ENC("), stored);
                assertFalse(stored.contains(OAuthTest.CLIENT_SECRET), stored);
                assertOAuth2SignIn(client);
            }
        }
    }

    @Test
    void generatesTheSamlKeyAgainForAnotherSecretKey(@TempDir Path home) throws Exception {
        try (var keycloak = keycloak();
             var s3 = new S3MockContainer("latest")) {
            keycloak.start();
            s3.start();
            String firstKey;
            try (var client = studio(s3).withInitParam("openl.home", home.toString())
                    .withInitParam("secret.key", "FirstKey")
                    .start()) {
                SamlTest.initStudio(client, keycloak.getAuthServerUrl());

                firstKey = localKey(home);
                assertTrue(firstKey.startsWith(LOCAL_KEY + "ENC("), firstKey);
                assertSamlSignIn(client);
            }
            var log = new ByteArrayOutputStream();
            try (var client = logged(studio(s3).withInitParam("openl.home", home.toString())
                    .withInitParam("secret.key", "SecondKey"), log)) {
                assertTrue(log.toString().contains("Cannot decrypt 'security.saml.local-key' with the current"
                        + " secret.key"), "The undecryptable SAML key must be logged as a warning");
                assertTrue(log.toString().contains("The SAML key pair of the service provider is incomplete or cannot"
                        + " be read"), "The regenerated SAML key pair must be logged as a warning");
                var secondKey = localKey(home);
                assertTrue(secondKey.startsWith(LOCAL_KEY + "ENC("), secondKey);
                assertNotEquals(firstKey, secondKey);
                assertSamlSignIn(client);
            }
        }
    }

    private static JettyServer studio(S3MockContainer s3, Path home) {
        return JettyServer.get()
                .withInitParam("openl.home", home.toString())
                .withInitParam("repository.production-s3.service-endpoint", s3.getHttpEndpoint());
    }

    private static HttpClient logged(JettyServer studio, ByteArrayOutputStream log) {
        var console = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                console.write(b);
                log.write(b);
            }
        }, true));
        try {
            return studio.start();
        } finally {
            System.setErr(console);
        }
    }

    private void assertSecretsWork(HttpClient client) throws Exception {
        var browser = assertOAuth2SignIn(client);
        assertEquals(HttpStatus.SC_OK, browser.get("/rest/deployments?repository=production-s3").statusCode());
    }

    private SsoBrowser assertOAuth2SignIn(HttpClient client) throws Exception {
        var browser = new SsoBrowser(client.getBaseURL());
        browser.loginViaOAuth2("admin", "admin");
        assertAdminSession(browser);
        return browser;
    }

    private void assertSamlSignIn(HttpClient client) throws Exception {
        var browser = new SsoBrowser(client.getBaseURL());
        browser.loginViaSaml("admin", "admin");
        assertAdminSession(browser);
    }

    private static String stored(Path home) throws IOException {
        return Files.readString(home.resolve(".properties"));
    }

    private static String localKey(Path home) throws IOException {
        return stored(home).lines().filter(line -> line.startsWith(LOCAL_KEY)).findFirst().orElse("");
    }
}
