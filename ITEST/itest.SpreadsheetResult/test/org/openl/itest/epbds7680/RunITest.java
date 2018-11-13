package org.openl.itest.epbds7680;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.Predicate;
import net.minidev.json.JSONArray;
import org.hamcrest.core.IsInstanceOf;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.JsonPath;

public class RunITest {

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
        rest = new RestClientFactory(baseURI + "/wadl-and-spreadsheetresult").create();
    }

    @Test
    public void test_jsonResponse_SpreadsheetResult_OK() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("i", 100);
        requestBody.put("j", "foo");

        ResponseEntity<String> response = rest.exchange("/tiktak",
                HttpMethod.POST, RestClientFactory.request(requestBody), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        DocumentContext documentContext = JsonPath.using(Configuration.defaultConfiguration()).parse(response.getBody());

        assertEquals(2, documentContext.read("$.results.length()"));
        assertEquals(1, documentContext.read("$.results[0].length()"));
        assertEquals(1, documentContext.read("$.results[1].length()"));
        assertEquals(100, documentContext.read("$.results[0][0]"));
        assertEquals("foo", documentContext.read("$.results[1][0]"));
        assertEquals(1, documentContext.read("$.columnNames.length()"));
        assertEquals("calc", documentContext.read("$.columnNames[0]"));
        assertEquals(2, documentContext.read("$.rowNames.length()"));
        assertEquals("INT", documentContext.read("$.rowNames[0]"));
        assertEquals("String", documentContext.read("$.rowNames[1]"));

        assertDoesNotExist(documentContext, "$.height");
        assertDoesNotExist(documentContext, "$.width");
        assertDoesNotExist(documentContext, "$.rowTitles");
        assertDoesNotExist(documentContext, "$.columnTitles");
        assertDoesNotExist(documentContext, "$.logicalTable");

        assertEquals(3, documentContext.read("$.length()"));
    }

    public static void assertDoesNotExist(DocumentContext documentContext, String expression) {
        try {
            documentContext.read("$.height");
            fail("The result for path '" + expression + "' must not be present!");
        } catch (PathNotFoundException unused) {
            //OK
        }
    }

}
