package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.apache.http.HttpStatus;

import org.openl.itest.core.JettyServer;

/**
 * Shared setup for integration tests that drive OpenL Studio against a Keycloak Identity Provider: the
 * realm-backed Keycloak container, the Studio server wired to an S3 design repository, and the
 * login-lifecycle assertions reused by the OAuth2 and SAML tests.
 *
 * @author Yury Molchan
 */
abstract class AbstractKeycloakTest {

    protected final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("resource")
    protected static KeycloakContainer keycloak() {
        return new KeycloakContainer("quay.io/keycloak/keycloak:latest")
                .withRealmImportFile("/openlstudio-realm.json");
    }

    protected static JettyServer studio(String profile, S3MockContainer s3) {
        return JettyServer.get()
                .withProfile(profile)
                .withInitParam("repository.production-s3.service-endpoint", s3.getHttpEndpoint())
                .withInitParam("repository.production-s3.access-key", "access key")
                .withInitParam("repository.production-s3.secret-key", "secret key");
    }

    /**
     * Asserts an unauthenticated client is challenged: the home page redirects to {@code loginEntry} and the
     * REST API answers 401.
     */
    protected void assertProtected(SsoBrowser browser, String loginEntry) throws Exception {
        var landing = browser.get("/");
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, landing.statusCode());
        assertTrue("Expected a redirect to the login entry " + loginEntry + " but was: "
                        + landing.headers().firstValue("Location"),
                landing.headers().firstValue("Location").orElseThrow().endsWith(loginEntry));
        assertRestUnauthorized(browser);
    }

    /**
     * Asserts the session resolves to the {@code admin} user with the administrator authority.
     */
    protected void assertAdminSession(SsoBrowser browser) throws Exception {
        var profile = browser.get("/rest/users/profile");
        assertEquals(HttpStatus.SC_OK, profile.statusCode());
        var user = mapper.readTree(profile.body());
        assertEquals("admin", user.get("username").asText());
        assertTrue("admin must be mapped to an administrator", user.get("administrator").asBoolean());
    }

    /**
     * Asserts that the REST API rejects the current client as unauthenticated.
     */
    protected void assertRestUnauthorized(SsoBrowser browser) throws Exception {
        assertEquals(HttpStatus.SC_UNAUTHORIZED, browser.get("/rest/users/profile").statusCode());
    }
}
