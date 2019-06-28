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

    private static final String SINGLE_DEPLOYMENT_ENDPOINT = "/REST/deployed-rules/hello";
    private static final String MULTIPLE_DEPLOYMENT_ENDPOINT = "/REST/project1/sayHello";

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
        assertEquals(HttpStatus.NOT_FOUND, sendHelloRequest(SINGLE_DEPLOYMENT_ENDPOINT).getStatusCode());

        ResponseEntity<String> response = doDeploy("/rules-to-deploy.zip", HttpMethod.POST);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        String body = pingDeployedService(SINGLE_DEPLOYMENT_ENDPOINT);
        assertEquals("Hello, Vlad", body);

        // should not be updated
        response = doDeploy("/rules-to-deploy_v2.zip", HttpMethod.POST);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        body = pingDeployedService(SINGLE_DEPLOYMENT_ENDPOINT);
        assertEquals("Hello, Vlad", body);

        // should be updated
        response = doDeploy("/rules-to-deploy_v2.zip", HttpMethod.PUT);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        body = pingDeployedService(SINGLE_DEPLOYMENT_ENDPOINT);
        assertEquals("Hello, Mr. Vlad", body);
    }

    @Test
    public void testDeployRules_multipleDeployment() {
        assertEquals(HttpStatus.NOT_FOUND, sendHelloRequest(MULTIPLE_DEPLOYMENT_ENDPOINT).getStatusCode());

        ResponseEntity<String> response = doDeploy("/multiple-deployment_v1.zip", HttpMethod.POST);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        String body = pingDeployedService(MULTIPLE_DEPLOYMENT_ENDPOINT);
        //TODO: looks like we have a bug. because response looks wrong. It should return "Hello, Vlad! v1"
        assertEquals("Hello, {\"name\": \"Vlad\"}! v1", body);

        // should not be updated
        response = doDeploy("/multiple-deployment_v2.zip", HttpMethod.POST);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        body = pingDeployedService(MULTIPLE_DEPLOYMENT_ENDPOINT);
        assertEquals("Hello, {\"name\": \"Vlad\"}! v1", body);

        // should not be updated
        response = doDeploy("/multiple-deployment_v2.zip", HttpMethod.PUT);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        body = pingDeployedService(MULTIPLE_DEPLOYMENT_ENDPOINT);
        assertEquals("Hello, Mr. {\"name\": \"Vlad\"}! v2", body);
    }

    private String pingDeployedService(String endpoint) {
        ResponseEntity<String> response = sendHelloRequest(endpoint);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        return body;
    }

    private ResponseEntity<String> sendHelloRequest(String endpoint) {
        return rest.exchange(endpoint,
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
