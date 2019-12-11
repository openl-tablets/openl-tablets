package org.openl.itest;

import java.util.Locale;
import java.util.TimeZone;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.openl.itest.core.RestClientFactory.request;

public class RunITest {

    private static JettyServer server;
    private static String baseURI;
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    @BeforeClass
    public static void setUp() throws Exception {
        Locale.setDefault(Locale.US);

        // set +2 as default
        TimeZone defaultTimeZone = TimeZone.getTimeZone("Europe/Helsinki");
        TimeZone.setDefault(defaultTimeZone);

        server = new JettyServer();
        baseURI = server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stop();
        } finally {
            Locale.setDefault(DEFAULT_LOCALE);
            TimeZone.setDefault(DEFAULT_TIMEZONE);
        }
    }

    @Before
    public void before() {
    }

    @Test
    public void testDefaultDateFormatConfiguration() {

        RestTemplate rest = new RestClientFactory(baseURI + "/rules-defaultdateformat").create();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = rest.exchange("/getDate", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("\"23/02/2019 00:00:00\"", response.getBody());

        ResponseEntity<DateWrapper> response2 = rest.postForEntity("/spr", request("{\"date1\":1514768645000,\"date2\":\"2019-01-01\",\"date3\":\"2019-11-11T03:03:03.000\",\"date4\":\"29/12/2019 04:04:04\"}"), DateWrapper.class);
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        DateWrapper body = response2.getBody();
        assertEquals( "01/01/2018 03:04:05", body.date1);
        assertEquals( "01/01/2019 00:00:00", body.date2);
        assertEquals( "11/11/2019 03:03:03", body.date3);
        assertEquals( "29/12/2019 04:04:04", body.date4);
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

    public static class DateWrapper {

        public String date1;
        public String date2;
        public String date3;
        public String date4;

    }

}
