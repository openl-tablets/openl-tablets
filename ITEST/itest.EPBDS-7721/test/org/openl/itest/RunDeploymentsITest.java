package org.openl.itest;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;

public class RunDeploymentsITest {

    private static JettyServer server1;
    private static String server1BaseURI;

    private RestTemplate rest1;

    @BeforeClass
    public static void setUp() throws Exception {
        server1 = new JettyServer();
        server1BaseURI = server1.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stop();
    }

    @Before
    public void before() {
        rest1 = new RestClientFactory(server1BaseURI).create();
    }

    @Test
    public void server1PingIncludedProjects_OK() {
        assertOK(rest1.getForEntity("/openl-project-1/ping", String.class));
        assertOK(rest1.getForEntity("/openl-project-2/ping", String.class));
        assertOK(rest1.getForEntity("/extra-project/ping", String.class));
    }

    @Test
    public void server1PingExcludedProject_shouldNotBeAccessible() {
        assertNotFound(rest1.getForEntity("/excluded-project/ping", String.class));
    }

    private void assertOK(ResponseEntity<String> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("pong", response.getBody());
    }

    private void assertNotFound(ResponseEntity<String> response) {
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
