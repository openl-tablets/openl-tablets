package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.text.SimpleDateFormat;

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
    public void testDefaultDateFormatConfiguration() throws IOException {

        RestTemplate rest = new RestClientFactory(baseURI + "/rules-defaultdateformat").create();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = rest.exchange("/getDate", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertNotBlank(response.getBody());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMddHHmmssZ");
        try {
            String v = response.getBody().trim();
            v = v.substring(1, v.length() - 1);
            simpleDateFormat.parse(v);
        } catch (Exception e) {
            fail("Failed to parse expected format.");
        }
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
        assertEquals("{\"@class\":\"org.openl.generated.beans.Cat\",\"name\":null,\"likesCream\":null,\"lives\":0}", response.getBody());
    }

    private static void assertNotBlank(String s) {
        if (s == null || s.trim().isEmpty()) {
            fail("String cannot be blank");
        }
    }

}
