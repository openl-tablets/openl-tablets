package org.openl.itest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(classes = {SpringBootWebApp.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
class SpringBootWebAppTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void indexPage() {
        String response = restTemplate.getForObject("/", String.class);
        assertThat(response).contains("OpenL Tablets Rule Services");
    }

    @Test
    void favicon() {
        byte[] response = restTemplate.getForObject("/favicon.ico", byte[].class);
        assertThat(response).hasSize(15086);
    }

    @Test
    void adminInfo() {
        String response = restTemplate.getForObject("/admin/ui/info", String.class);
        assertThat(response).contains(
                "\"urls\":{\"RESTFUL\":\"openl-rules-rs\"},\"hasManifest\":true,\"deploymentName\":\"itest.spring-boot.openl-rules");
    }

    @Test
    void openAPI() {
        String response = restTemplate.getForObject("/openl-rules-rs/openapi.json", String.class);
        assertThat(response).contains("SayHello");
    }

    @Test
    void postEmpty() {
        String response = restTemplate.postForObject("/openl-rules-rs/SayHello", "", String.class);
        assertThat(response).contains("Hello, World!");
    }

    @Test
    void postMister() {
        String response = restTemplate.postForObject("/openl-rules-rs/SayHello", "Mister", String.class);
        assertThat(response).contains("Hello, Mister!");
    }

    @Test
    void deployZipFromOpenLFolder() {
        String response = restTemplate
                .postForObject("/deployed-rules/hello", "John Smith", String.class);
        assertThat(response).contains("Hello, John Smith");
    }

    @Test
    void postPassVocabulary() {
        var response = restTemplate.postForObject("/openl-rules-rs/ValidateVocabulary", "pass", String.class);
        assertThat(response).isEqualTo("World");
    }

    @Test
    void postFailVocabulary() throws Exception {
        var response = restTemplate.postForObject("/openl-rules-rs/ValidateVocabulary", "fail", String.class);
        var json = new ObjectMapper().readTree(response);
        assertThat(json.get("code").asText()).isEqualTo("USR001");
        assertThat(json.get("message").asText()).isEqualTo("User Error");
    }

    @Test
    void postUnknownVocabulary() throws Exception {
        var response = restTemplate.postForObject("/openl-rules-rs/ValidateVocabulary", "unknown", String.class);
        var json = new ObjectMapper().readTree(response);
        assertThat(json.get("code").asText()).isEqualTo("VALIDATION");
        assertThat(json.get("message").asText()).startsWith("Object 'unknown' is outside of valid domain 'Vocabulary'");
    }
}
