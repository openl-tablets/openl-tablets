package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.openl.itest.core.RestClientFactory.request;

import javax.xml.ws.WebServiceException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.core.SoapClientFactory;
import org.openl.itest.core.JettyServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class SmockITest {

    private static JettyServer server;
    private static String baseURI;

    private RestTemplate rest;
    private Service soap;

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
        rest = new RestClientFactory(baseURI + "/REST/simple/").create();
        soap = new SoapClientFactory<>(baseURI + "/simple/", Service.class).createProxy();
    }

    @Test
    public void testPingRest() {
        ResponseEntity<String> response = rest.getForEntity("ping", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Pong!", response.getBody());
    }

    @Test
    public void testTwiceRest() {
        ResponseEntity<String> response = rest.postForEntity("twice", request("6"), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("12", response.getBody());
    }

    @Test
    public void testMulRest() {
        ResponseEntity<Integer> response = rest.postForEntity("mul", request("{ \"x\": 7, \"y\": 3 }"), Integer.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Integer.valueOf(21), response.getBody());
    }

    @Test
    public void test404Rest() {
        ResponseEntity<String> response = rest.postForEntity("absent", "", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testPingSoap() {
        assertEquals("Pong!", soap.ping());
    }

    @Test
    public void testTwiceSoap() {
        assertEquals(Integer.valueOf(12), soap.twice(6));
    }

    @Test
    public void testMulSoap() {
        assertEquals(Integer.valueOf(21), soap.mul(7, 3));
    }

    @Test(expected = WebServiceException.class)
    public void test404Soap() {
        soap.absent();
        fail();
    }
}
