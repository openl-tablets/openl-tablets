package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

/**
 * Verifies the SAML 2.0 login and SP-initiated Single Logout lifecycle against a real Keycloak Identity
 * Provider. Mirrors the OAuth2 scenario in {@link OAuthTest#singleLogout()}.
 *
 * @author Yury Molchan
 */
@DisabledIfSystemProperty(named = "noDocker", matches = ".*")
class SamlTest extends AbstractKeycloakTest {

    private static final String ENTITY_ID = "openlstudio-saml";

    @Test
    void singleLogout() throws Exception {
        try (var keycloak = keycloak();
             var s3 = new S3MockContainer("latest")) {
            keycloak.start();
            s3.start();
            var authServerUrl = keycloak.getAuthServerUrl();
            try (var httpClient = studio(s3).start()) {
                initStudio(httpClient, authServerUrl);

                var browser = new SsoBrowser(httpClient.getBaseURL());

                // Unauthenticated access is challenged.
                assertProtected(browser, "/saml2/authenticate/webstudio");

                // Log in; the session resolves to admin.
                browser.loginViaSaml("admin", "admin");
                assertAdminSession(browser);

                // SP-initiated logout sends a SAML LogoutRequest to the IdP Single Logout Service.
                var logoutDestination = browser.logoutViaSaml();
                assertTrue(logoutDestination.startsWith(authServerUrl + "/realms/openlstudio/protocol/saml"),
                        "Logout must target the IdP Single Logout Service: " + logoutDestination);

                // Local session cleared; the IdP requires re-authentication.
                assertRestUnauthorized(browser);
                assertTrue(browser.samlChallengesForLogin(),
                        "Keycloak must require re-authentication after logout");
            }
        }
    }

    private void initStudio(org.openl.itest.core.HttpClient httpClient, String authServerUrl) {
        var samlConfig = (ObjectNode) httpClient.readTree("test-resources-saml/set-authentication-template.json");
        samlConfig.put("metadataUrl", authServerUrl + "/realms/openlstudio/protocol/saml/descriptor");
        samlConfig.put("entityId", ENTITY_ID);
        httpClient.postForObject("/rest/admin/settings/authentication", samlConfig);
    }
}
