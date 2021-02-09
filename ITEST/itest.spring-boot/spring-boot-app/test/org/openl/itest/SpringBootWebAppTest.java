package org.openl.itest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(classes = { SpringBootWebApp.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
public class SpringBootWebAppTest {

    @Autowired
    private TestRestTemplate restTemplate;

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
        assertThat(response).contains("\"urls\":{\"RESTFUL\":\"openl-rules-rs\"},\"hasManifest\":true,\"status\":\"DEPLOYED\"");
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
        String response = restTemplate.postForObject("/REST/deployed-rules/hello", new Request1("John Smith"), String.class);
        assertThat(response).contains("Hello, John Smith");
    }

    public static class Request1 {
        public String name;

        public Request1(String name) {
            this.name = name;
        }
    }
}
