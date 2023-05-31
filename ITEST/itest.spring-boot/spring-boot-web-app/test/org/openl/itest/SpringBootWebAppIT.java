package org.openl.itest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

public class SpringBootWebAppIT {

    private static RestTemplate restTemplate;

    @BeforeAll
    static void setup() {
        restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new RootUriTemplateHandler("http://localhost:" + System.getProperty("test.server.port")));
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected void handleError(ClientHttpResponse response, HttpStatus statusCode) {
                // Skip handling
            }
        });
    }

    @Test
    public void indexPage() {
        String response = restTemplate.getForObject("/", String.class);
        assertThat(response).contains("OpenL Tablets Rule Services");
    }

    @Test
    public void favicon() {
        byte[] response = restTemplate.getForObject("/favicon.ico", byte[].class);
        assertThat(response).hasSize(1086);
    }

    @Test
    public void adminInfo() {
        String response = restTemplate.getForObject("/admin/ui/info", String.class);
        assertThat(response).contains(
                "\"urls\":{\"RESTFUL\":\"openl-rules-rs\"},\"hasManifest\":true,\"deploymentName\":\"itest.spring-boot.openl-rules");
    }

    @Test
    public void openAPI() {
        String response = restTemplate.getForObject("/openl-rules-rs/openapi.json", String.class);
        assertThat(response).contains("SayHello");
    }

    @Test
    public void postEmpty() {
        String response = restTemplate.postForObject("/openl-rules-rs/SayHello", "", String.class);
        assertThat(response).contains("Hello, World!");
    }

    @Test
    public void postMister() {
        String response = restTemplate.postForObject("/openl-rules-rs/SayHello", "Mister", String.class);
        assertThat(response).contains("Hello, Mister!");
    }

    @Test
    public void deployZipFromOpenLFolder() {
        String response = restTemplate
                .postForObject("/REST/deployed-rules/hello", new Request1("John Smith"), String.class);
        assertThat(response).contains("Hello, John Smith");
    }

    @Test
    public void postPassVocabulary() {
        var response = restTemplate.postForObject("/openl-rules-rs/ValidateVocabulary", "pass", String.class);
        assertThat(response).isEqualTo("World");
    }

    @Test
    public void postFailVocabulary() throws Exception {
        var response = restTemplate.postForObject("/openl-rules-rs/ValidateVocabulary", "fail", String.class);
        var json = new ObjectMapper().readTree(response);
        assertThat(json.get("code").asText()).isEqualTo("USR001");
        assertThat(json.get("message").asText()).isEqualTo("User Error");
    }

    @Test
    public void postUnknownVocabulary() throws Exception {
        var response = restTemplate.postForObject("/openl-rules-rs/ValidateVocabulary", "unknown", String.class);
        var json = new ObjectMapper().readTree(response);
        assertThat(json.get("code").asText()).isEqualTo("VALIDATION");
        assertThat(json.get("message").asText()).startsWith("Object 'unknown' is outside of valid domain 'Vocabulary'");
    }

    public static class Request1 {
        public String name;

        public Request1(String name) {
            this.name = name;
        }
    }
}
