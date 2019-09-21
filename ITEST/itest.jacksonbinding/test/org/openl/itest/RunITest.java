package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class RunITest {

    private static JettyServer server;
    private static String baseURI;

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
    }

    @Test
    public void testSerializationInclusionConfiguration() {

        RestTemplate rest = new RestClientFactory(baseURI + "/rules-serializationInclusion").create();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = rest.exchange("/getObject", HttpMethod.GET, entity, String.class);
        String body = response.getBody();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"num\":22,\"str\":\"test\",\"intArr\":[],\"ldt\":\"2019-02-20T06:30:00\",\"ld\":\"2019-08-16\",\"lt\":\"10:15:30\",\"zdt\":\"2019-12-02T11:15:30-05:00\"}", body);

    }

    @Test
    public void testDefaultDateFormatConfiguration() {

        RestTemplate rest = new RestClientFactory(baseURI + "/rules-defaultdateformat").create();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = rest.exchange("/getDate", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("\"190223000000\"", response.getBody());
    }

    @Test
    public void testDisableDefaultTyping() {

        RestTemplate rest = new RestClientFactory(baseURI + "/rules-disabledefaulttyping").create();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = rest.exchange("/getObject", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"value\":2}", response.getBody());

    }

    @Test
    public void testSmartDefaultTyping() {
        RestTemplate rest = new RestClientFactory(baseURI + "/rules-smartdefaulttyping").create();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = rest.exchange("/myCat", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"@class\":\"org.openl.generated.beans.Cat\",\"name\":null,\"likesCream\":null,\"lives\":0}",
            response.getBody());
    }

    private static void assertNotBlank(String s) {
        if (s == null || s.trim().isEmpty()) {
            fail("String cannot be blank");
        }
    }

}
