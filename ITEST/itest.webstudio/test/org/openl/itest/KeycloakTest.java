package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import org.openl.itest.core.JettyServer;

@DisabledIfSystemProperty(named = "noDocker", matches = ".*")
public class KeycloakTest {

    private static final String CLIENT_ID = "openlstudio";
    private static final String CLIENT_SECRET = "kXo86nuTdOYQzPZ7k09G7vQmqeDNNZoM";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("resource")
    private static KeycloakContainer createKeycloakContainer() {
        return new KeycloakContainer("quay.io/keycloak/keycloak:latest")
                .withRealmImportFile("/openlstudio-realm.json");
    }

    @Test
    public void smoke() throws Exception {
        try (var keycloack = createKeycloakContainer()) {
            keycloack.start();
            var authServerUrl = keycloack.getAuthServerUrl();
            var bearerTokens = retrieveBearerAccessTokens(authServerUrl);
            try (var httpClient = JettyServer.get()
                        .withProfile("oauth2")
                        .start()) {
                initStudio(httpClient, authServerUrl);

                httpClient.localEnv.putAll(bearerTokens);
                httpClient.test("test-resources-oauth2");

                // stop Keycloak to simulate the lag
                keycloack.stop();
                httpClient.test("test-resources-oauth2-lag");
            }
        }
    }

    @Test
    public void smokePat() throws Exception {
        try (var keycloack = createKeycloakContainer()) {
            keycloack.start();
            var authServerUrl = keycloack.getAuthServerUrl();
            var bearerTokens = retrieveBearerAccessTokens(authServerUrl);
            try (var httpClient = JettyServer.get()
                    .withProfile("oauth2")
                    .start()) {
                initStudio(httpClient, authServerUrl);

                httpClient.localEnv.putAll(bearerTokens);
                httpClient.test("test-resources-pat/000-setup");

                // Create PATs for users
                var adminPat = getPersonalAccessTokenForUser(httpClient,
                        bearerTokens.get("ADMIN_ACCESS_TOKEN"),
                        new CreatePersonalAccessTokenRequest("admin-pat", Date.from(Instant.now().plusSeconds(3600))));
                assertEquals("admin", adminPat.loginName());
                httpClient.localEnv.put("ADMIN_PAT", adminPat.token());
                httpClient.localEnv.put("ADMIN_PAT_PUBLIC_ID", adminPat.publicId());

                var user1Pat = getPersonalAccessTokenForUser(httpClient,
                        bearerTokens.get("USER1_ACCESS_TOKEN"),
                        new CreatePersonalAccessTokenRequest("user1-pat", Date.from(Instant.now().plusSeconds(3600))));
                assertEquals("user1", user1Pat.loginName());
                httpClient.localEnv.put("USER1_PAT", user1Pat.token());
                httpClient.localEnv.put("USER1_PAT_PUBLIC_ID", user1Pat.publicId());

                var user1Pat2 = getPersonalAccessTokenForUser(httpClient,
                        bearerTokens.get("USER1_ACCESS_TOKEN"),
                        new CreatePersonalAccessTokenRequest("user1-pat-second", Date.from(Instant.now().plusSeconds(3600))));
                assertEquals("user1", user1Pat2.loginName());
                httpClient.localEnv.put("USER1_PAT_2", user1Pat2.token());
                httpClient.localEnv.put("USER1_PAT_PUBLIC_ID_2", user1Pat2.publicId());

                var guestPat = getPersonalAccessTokenForUser(httpClient,
                        bearerTokens.get("GUEST_ACCESS_TOKEN"),
                        new CreatePersonalAccessTokenRequest("guest-pat", Date.from(Instant.now().plusSeconds(3600))));
                assertEquals("guest", guestPat.loginName());
                httpClient.localEnv.put("GUEST_PAT", guestPat.token());
                httpClient.localEnv.put("GUEST_PAT_PUBLIC_ID", guestPat.publicId());

                // Continue tests with PATs
                httpClient.test("test-resources-pat/100-pat-tests");
            }
        }
    }

    private Map<String, String> retrieveBearerAccessTokens(String authServerUrl) throws URISyntaxException, IOException, InterruptedException {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("ADMIN_ACCESS_TOKEN", getAccessTokenForUser(authServerUrl, "admin", "admin"));
        tokens.put("USER1_ACCESS_TOKEN", getAccessTokenForUser(authServerUrl, "user1", "user1"));
        tokens.put("GUEST_ACCESS_TOKEN", getAccessTokenForUser(authServerUrl, "guest", "guest"));
        tokens.put("UNKNOWN_ACCESS_TOKEN", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
        return tokens;
    }

    private void initStudio(org.openl.itest.core.HttpClient httpClient, String authServerUrl) {
        var oauth2Config = (ObjectNode) httpClient.readTree("test-resources-oauth2/set-authentication-template.json");
        oauth2Config.put("issuerUri", authServerUrl + "/realms/openlstudio");
        oauth2Config.put("clientSecret", CLIENT_SECRET);
        oauth2Config.put("clientId", CLIENT_ID);
        httpClient.postForObject("/rest/admin/settings/authentication", oauth2Config);
    }

    private String getAccessTokenForUser(String authServerUrl, String username, String password) throws URISyntaxException, IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(new URI(authServerUrl + "/realms/openlstudio/protocol/openid-connect/token"))
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

    private PersonalAccessTokenResponse getPersonalAccessTokenForUser(org.openl.itest.core.HttpClient httpClient, String bearerToken, CreatePersonalAccessTokenRequest tokenData) {
        var patResponse = httpClient.postForObject("/rest/users/personal-access-tokens",
                tokenData,
                PersonalAccessTokenResponse.class,
                HttpStatus.SC_CREATED,
                HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        assertEquals(tokenData.name(), patResponse.name());
        assertTrue(patResponse.createdAt().before(tokenData.expiresAt()));
        assertEquals(tokenData.expiresAt(), patResponse.expiresAt());
        return patResponse;
    }

    public record PersonalAccessTokenResponse(
            String publicId,
            String name,
            String loginName,
            String token,
            @JsonDeserialize(using = UtcDateDeserializer.class)
            Date createdAt,
            @JsonDeserialize(using = UtcDateDeserializer.class)
            Date expiresAt) {
    }

    public record CreatePersonalAccessTokenRequest(
            String name,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            Date expiresAt) {
    }

    public static class UtcDateDeserializer extends JsonDeserializer<Date> {

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            Instant instant = Instant.parse(p.getValueAsString());
            return Date.from(instant);
        }
    }

}
