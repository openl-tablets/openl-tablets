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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

public class KeycloakLagTest {

    private static final KeycloakContainer KEYCLOAK_CONTAINER = new KeycloakContainer("quay.io/keycloak/keycloak:24.0")
            .withRealmImportFile("/keycloak/openlstudio-realm.json");

    private static final String CLIENT_ID = "openlstudio";
    private static final String CLIENT_SECRET = "kXo86nuTdOYQzPZ7k09G7vQmqeDNNZoM";
    private static Map<String, String> config;

    @BeforeAll
    public static void initialize() {
        config = new HashMap<>();
        config.put("security.oauth2.client-id", CLIENT_ID);
        config.put("security.oauth2.client-secret", CLIENT_SECRET);
    }

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void smoke() throws Exception {
        JettyServer server = null;
        try {
            KEYCLOAK_CONTAINER.start();
            config.put("security.oauth2.issuer-uri", KEYCLOAK_CONTAINER.getAuthServerUrl() + "realms/openlstudio");
            var adminAccessToken = getAccessTokenForUser("admin", "admin");
            server = JettyServer.start("oauth2", config);
            KEYCLOAK_CONTAINER.stop(); // stop Keycloak to simulate the lag
            var httpClient = server.client();
            httpClient.localEnv.put("ADMIN_ACCESS_TOKEN", adminAccessToken);
            httpClient.test("test-resources-negative");
        } finally {
            try {
                if (server != null) {
                    server.stop();
                }
            } finally {
                // just make sure that the container is stopped
                KEYCLOAK_CONTAINER.stop();
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
