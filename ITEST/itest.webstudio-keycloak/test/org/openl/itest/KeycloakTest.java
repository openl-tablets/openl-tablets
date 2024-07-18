package org.openl.itest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

public class KeycloakTest {

    private static final KeycloakContainer KEYCLOAK_CONTAINER = new KeycloakContainer("quay.io/keycloak/keycloak:25.0")
            .withRealmImportFile("/keycloak/openlstudio-realm.json");

    private static final String CLIENT_ID = "openlstudio";
    private static final String CLIENT_SECRET = "kXo86nuTdOYQzPZ7k09G7vQmqeDNNZoM";
    private static Map<String, String> config;

    @BeforeAll
    public static void initialize() {
        KEYCLOAK_CONTAINER.start();
        config = new HashMap<>();
        config.put("security.oauth2.issuer-uri", KEYCLOAK_CONTAINER.getAuthServerUrl() + "realms/openlstudio");
        config.put("security.oauth2.client-id", CLIENT_ID);
        config.put("security.oauth2.client-secret", CLIENT_SECRET);
    }

    @AfterAll
    public static void destroy() {
        KEYCLOAK_CONTAINER.stop();
    }

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void smoke() throws Exception {
        JettyServer server = null;
        try {
            server = JettyServer.start("oauth2", config);
            var httpClient = server.client();
            httpClient.localEnv.put("ADMIN_ACCESS_TOKEN", getAccessTokenForUser("admin", "admin"));
            httpClient.localEnv.put("USER1_ACCESS_TOKEN", getAccessTokenForUser("user1", "user1"));
            httpClient.localEnv.put("GUEST_ACCESS_TOKEN", getAccessTokenForUser("guest", "guest"));
            httpClient.localEnv.put("UNKNOWN_ACCESS_TOKEN", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
            httpClient.test("test-resources-smoke");
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    private String getAccessTokenForUser(String username, String password) throws URISyntaxException, IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(new URI(KEYCLOAK_CONTAINER.getAuthServerUrl() + "realms/openlstudio/protocol/openid-connect/token"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=password&scope=openid profile email" +
                        "&client_id=" + CLIENT_ID +
                        "&client_secret=" + CLIENT_SECRET +
                        "&username=" + username +
                        "&password=" + password))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == HttpStatus.SC_OK) {
            String responseBody = response.body();
            var responseNode = mapper.readTree(responseBody);
            return responseNode.get("access_token").asText();
        } else {
            throw new RuntimeException("Failed to get Access Token: " + response.statusCode() + " - " + response.body());
        }
    }

}
