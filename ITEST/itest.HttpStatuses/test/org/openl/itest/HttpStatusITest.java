package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.responsedto.ErrorResponse;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class HttpStatusITest {
    private static JettyServer server;
    private static String baseURI;
    private static HttpClient client;

    private RestTemplate rest;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer(true);
        baseURI = server.start();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Before
    public void before() {
        rest = new RestClientFactory(baseURI + "/REST/").create();
    }

    @Test
    public void test_rest_USER_ERROR() {
        ResponseEntity<ErrorResponse> response = rest.getForEntity("http-statuses-test/throwUserException",
            ErrorResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User error!", response.getBody().getMessage());
        assertEquals(ExceptionType.USER_ERROR.name(), response.getBody().getType());
        assertNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_RULES_RUNTIME() {
        ResponseEntity<ErrorResponse> response = rest.getForEntity("http-statuses-test/throwOpenLException",
            ErrorResponse.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
            "Failure in the method: public static java.lang.Double java.lang.Double.valueOf(java.lang.String) throws java.lang.NumberFormatException on the target: java.lang.Double with values: [sdsd]. Cause: For input string: \"sdsd\"",
            response.getBody().getMessage());
        assertEquals(ExceptionType.RULES_RUNTIME.name(), response.getBody().getType());
        assertNotNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_COMPULATION() {
        ResponseEntity<ErrorResponse> response = rest.exchange(
            "http-statuses-lazycompilation-test/throwCompilationError",
            HttpMethod.POST,
            RestClientFactory.request("{ \"lob\": \"module1\"}"),
            ErrorResponse.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to load lazy method.", response.getBody().getMessage());
        assertEquals(ExceptionType.COMPILATION.name(), response.getBody().getType());
        assertNotNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_RULES_RUNTIME_validation_exception() {
        ResponseEntity<ErrorResponse> response = rest.getForEntity("http-statuses-test/throwValidationException",
            ErrorResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Object 'FOUR' is outside of valid domain 'StringType'. Valid values: [ONE, TWO, THREE]",
            response.getBody().getMessage());
        assertEquals(ExceptionType.VALIDATION.name(), response.getBody().getType());
        assertNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_JSON_EXCEPTION() {
        ResponseEntity<ErrorResponse> response = rest.exchange("http-statuses-test/hello",
            HttpMethod.POST,
            RestClientFactory.request("{sdssddsd"),
            ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Should contains JsonParseException",
            response.getBody().getMessage().contains("JsonParseException"));
        assertEquals(ExceptionType.BAD_REQUEST.name(), response.getBody().getType());
        assertNotNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_UNSUPPORTED_MEDIA_TYPE() {
        client.post("/REST/http-statuses-test/hello", "/statuses-415.resp.txt!", 415, "/statuses-415.resp.txt");
    }

    @Test
    public void test_rest_NOT_ALLOWED() {
        client.post("/REST/http-statuses-test/throwNFE", "/statuses-405.resp.txt!", 405, "/statuses-405.resp.txt");
    }

    @Test
    public void test_rest_NPE() {
        ResponseEntity<ErrorResponse> response = rest.getForEntity("http-statuses-test/throwNPE", ErrorResponse.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ExceptionType.RULES_RUNTIME.name(), response.getBody().getType());
        assertNotNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_NFE() {
        ResponseEntity<ErrorResponse> response = rest.getForEntity("http-statuses-test/throwNFE", ErrorResponse.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ExceptionType.RULES_RUNTIME.name(), response.getBody().getType());
        assertNotNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_NOT_FOUND() {
        client.get("/REST/http-statuses-test/hKllo", 404, "/statuses-404.resp.txt");
    }

    @Test
    public void test_soap_USER_ERROR() {
        client.post("/http-statuses-test", "/statuses-userError.req.xml", 500, "/statuses-userError.resp.xml");
    }

    @Test
    public void test_soap_RULES_RUNTIME() {
        client.post("/http-statuses-test", "/statuses-rulesRuntime.req.xml", 500, "/statuses-rulesRuntime.resp.xml");
    }

    @Test
    public void test_soap_RULES_RUNTIME_validation_exception() {
        client.post("/http-statuses-test", "/statuses-validation.req.xml", 500, "/statuses-validation.resp.xml");
    }

    @Test
    public void test_soap_COMPULATION() {
        client.post("/http-statuses-lazycompilation-test",
            "/statuses-lazycompilation.req.xml",
            500,
            "/statuses-lazycompilation.resp.xml");
    }

    @Test
    public void test_soap_NPE() {
        client.post("/http-statuses-test", "/statuses-npe.req.xml", 500, "/statuses-npe.resp.xml");
    }

    @Test
    public void test_soap_NFE() {
        client.post("/http-statuses-test", "/statuses-nfe.req.xml", 500, "/statuses-nfe.resp.xml");
    }
}
