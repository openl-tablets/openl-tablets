package org.openl.itest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

@SpringBootTest(classes = { SpringBootWebApp.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
public class SpringBootWebAppTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void indexPage() throws Exception {
        String response = restTemplate.getForObject("http://localhost:" + port + "/", String.class);
        assertThat(response).contains("OpenL Tablets Rule Services");
    }

    @Test
    public void favicon() throws Exception {
        byte[] response = restTemplate.getForObject("http://localhost:" + port + "/favicon.ico", byte[].class);
        assertThat(response).hasSize(1086);
    }

    @Test
    public void adminInfo() throws Exception {
        String response = restTemplate.getForObject("http://localhost:" + port + "/admin/ui/info", String.class);
        assertThat(response).contains("\"urls\":{\"RESTFUL\":\"openl-rules-rs\"},\"hasManifest\":true,\"status\":\"DEPLOYED\"");
    }

    @Test
    public void openAPI() throws Exception {
        String response = restTemplate.getForObject("http://localhost:" + port + "/openl-rules-rs/openapi.json", String.class);
        assertThat(response).contains("SayHello");
    }

    @Test
    public void postEmpty() throws Exception {
        String response = restTemplate.postForObject("http://localhost:" + port + "/openl-rules-rs/SayHello", "", String.class);
        assertThat(response).contains("Hello, World!");
    }


    @Test
    public void postMister() throws Exception {
        String response = restTemplate.postForObject("http://localhost:" + port + "/openl-rules-rs/SayHello", "Mister", String.class);
        assertThat(response).contains("Hello, Mister!");
    }
}
