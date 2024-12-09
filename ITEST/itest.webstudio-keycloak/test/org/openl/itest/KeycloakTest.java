package org.openl.itest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

public class KeycloakTest {

    private static final String CLIENT_ID = "openlstudio";
    private static final String CLIENT_SECRET = "kXo86nuTdOYQzPZ7k09G7vQmqeDNNZoM";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void smoke() throws Exception {
        try (var keycloack = new KeycloakContainer("quay.io/keycloak/keycloak:25.0")) {
            keycloack
                    .withRealmImportFile("/keycloak/openlstudio-realm.json")
                    .start();
            var authServerUrl = keycloack.getAuthServerUrl();
            try (var httpClient = JettyServer.get()
                        .withProfile("oauth2")
                        .withInitParam("security.oauth2.client-id", CLIENT_ID)
                        .withInitParam("security.oauth2.client-secret", CLIENT_SECRET)
                        .withInitParam("security.oauth2.issuer-uri", authServerUrl + "realms/openlstudio")
                        .start()) {
                httpClient.localEnv.put("ADMIN_ACCESS_TOKEN", getAccessTokenForUser(authServerUrl, "admin", "admin"));
                httpClient.localEnv.put("USER1_ACCESS_TOKEN", getAccessTokenForUser(authServerUrl, "user1", "user1"));
                httpClient.localEnv.put("GUEST_ACCESS_TOKEN", getAccessTokenForUser(authServerUrl, "guest", "guest"));
                httpClient.localEnv.put("UNKNOWN_ACCESS_TOKEN", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
                httpClient.test("test-resources-smoke");

                // stop Keycloak to simulate the lag
                keycloack.stop();
                httpClient.test("test-resources-negative");
            }
        }
    }

    private String getAccessTokenForUser(String authServerUrl, String username, String password) throws URISyntaxException, IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(new URI(authServerUrl + "realms/openlstudio/protocol/openid-connect/token"))
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
