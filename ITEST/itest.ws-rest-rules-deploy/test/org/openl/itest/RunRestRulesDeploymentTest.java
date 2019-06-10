package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class RunRestRulesDeploymentTest {

    private static JettyServer server;
    private static String baseURI;

    private RestTemplate rest;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer();
        baseURI = server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Before
    public void before() {
        rest = new RestClientFactory(baseURI).create();
    }

    @Test
    public void testDeployRules() {
        assertEquals(HttpStatus.NOT_FOUND, sendHelloRequest().getStatusCode());

        ResponseEntity<String> response = doDeploy("/rules-to-deploy.zip", HttpMethod.POST);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        String body = pingDeployedService();
        assertEquals("Hello, Vlad", body);

        // should not be updated
        response = doDeploy("/rules-to-deploy_v2.zip", HttpMethod.POST);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        body = pingDeployedService();
        assertEquals("Hello, Vlad", body);

        // should be updated
        response = doDeploy("/rules-to-deploy_v2.zip", HttpMethod.PUT);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        body = pingDeployedService();
        assertEquals("Hello, Mr. Vlad", body);
    }

    private String pingDeployedService() {
        ResponseEntity<String> response = sendHelloRequest();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        return body;
    }

    private ResponseEntity<String> sendHelloRequest() {
        return rest.exchange("/REST/deployed-rules/hello",
            HttpMethod.POST,
            RestClientFactory.request("{\"name\": \"Vlad\"}"),
            String.class);
    }

    private ResponseEntity<String> doDeploy(String rules, HttpMethod method) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/zip");
        return rest.exchange("/rules/deploy", method, new HttpEntity<>(new ClassPathResource(rules), headers), String.class);
    }

}
