package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.app.SpringBootApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(classes = {SpringBootApp.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenLExampleTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Value("${example.auth.username}") 
    private String username;
    
    @Value("${example.auth.password}") 
    private String password;
    
    @Test
    void indexPage() {
        String response = restTemplate.getForObject("/", String.class);
        assertThat(response).contains("OpenL Tablets Rule Services");
    }
    
    @Test
    void simpleProjectTest() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth(username , password)
                .getForEntity("/example-simple/Ping", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("Pong");
    }
    
    @Test
    void projectWithDependenciesTest() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth(username , password)
                .getForEntity("/example-main/Ping", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("Pong!");
    }
}
