package org.openl.itest;

import org.apache.cxf.binding.soap.SoapFault;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.core.SoapClientFactory;
import org.openl.itest.responsedto.ErrorResponse;
import org.openl.itest.rules.TestHttpStatusService;
import org.openl.itest.rules.TestLazyCompilationService;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSErrorResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HttpStatusITest {
    private static JettyServer server;
    private static String baseURI;

    private RestTemplate rest;

    private TestHttpStatusService httpStatusSoap;
    private TestLazyCompilationService lazyCompilationSoap;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer(true);
        baseURI = server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Before
    public void before() {
        rest = new RestClientFactory(baseURI + "/REST/").create();
        lazyCompilationSoap = new SoapClientFactory<>(baseURI + "/http-statuses-lazycompilation-test", TestLazyCompilationService.class).createProxy();
        httpStatusSoap = new SoapClientFactory<>(baseURI + "/http-statuses-test", TestHttpStatusService.class).createProxy();
    }

    @Test
    public void test_rest_USER_ERROR() {
        ResponseEntity<ErrorResponse> response = rest.getForEntity("http-statuses-test/throwUserException", ErrorResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User error!", response.getBody().getMessage());
        assertEquals(ExceptionType.USER_ERROR.name(), response.getBody().getType());
        assertNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_RULES_RUNTIME() {
        ResponseEntity<ErrorResponse> response = rest.getForEntity("http-statuses-test/throwOpenLException", ErrorResponse.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failure in the method: public static java.lang.Double java.lang.Double.valueOf(java.lang.String) throws java.lang.NumberFormatException on the target: java.lang.Double with values: [sdsd]. Cause: For input string: \"sdsd\"", response.getBody().getMessage());
        assertEquals(ExceptionType.RULES_RUNTIME.name(), response.getBody().getType());
        assertNotNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_COMPULATION() {
        ResponseEntity<ErrorResponse> response = rest.exchange("http-statuses-lazycompilation-test/throwCompilationError", HttpMethod.POST, RestClientFactory.request("{ \"lob\": \"module1\"}"), ErrorResponse.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to load lazy method.", response.getBody().getMessage());
        assertEquals(ExceptionType.COMPILATION.name(), response.getBody().getType());
        assertNotNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_RULES_RUNTIME_validation_exception() {
        ResponseEntity<ErrorResponse> response = rest.getForEntity("http-statuses-test/throwValidationException", ErrorResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Object 'FOUR' is outside of valid domain 'StringType'. Valid values: [ONE, TWO, THREE]", response.getBody().getMessage());
        assertEquals(ExceptionType.VALIDATION.name(), response.getBody().getType());
        assertNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_JSON_EXCEPTION() {
        ResponseEntity<ErrorResponse> response = rest.exchange("http-statuses-test/hello", HttpMethod.POST, RestClientFactory.request("{sdssddsd"), ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Should contains JsonParseException", response.getBody().getMessage().contains("JsonParseException"));
        assertEquals(ExceptionType.BAD_REQUEST.name(), response.getBody().getType());
        assertNotNull(response.getBody().getDetails());
    }

    @Test
    public void test_rest_UNSUPPORTED_MEDIA_TYPE() {
        ResponseEntity<String> response = rest.postForEntity("http-statuses-test/hello","{sdssddsd", String.class);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
    }

    @Test
    public void test_rest_NPE() {
        ResponseEntity<String> response = rest.getForEntity("http-statuses-test/throwNPE", String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void test_rest_NOT_FOUND() {
        ResponseEntity<String> response = rest.getForEntity("http-statuses-test/hKllo", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void test_soap_USER_ERROR() {
        try {
            httpStatusSoap.throwUserException();
        } catch (SoapFault e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getStatusCode());
            assertEquals("User error!", e.getMessage());
            assertEquals(ExceptionType.USER_ERROR.name(), e.getDetail().getElementsByTagName("type").item(0).getTextContent());
        }
    }

    @Test
    public void test_soap_RULES_RUNTIME() {
        try {
            httpStatusSoap.throwOpenLException();
        } catch (SoapFault e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getStatusCode());
            assertEquals("Failure in the method: public static java.lang.Double java.lang.Double.valueOf(java.lang.String) throws java.lang.NumberFormatException on the target: java.lang.Double with values: [sdsd]. Cause: For input string: \"sdsd\"", e.getMessage());
            assertEquals(ExceptionType.RULES_RUNTIME.name(), e.getDetail().getElementsByTagName("type").item(0).getTextContent());
        }
    }

    @Test
    public void test_soap_RULES_RUNTIME_validation_exception() {
        try {
            httpStatusSoap.throwValidationException();
        } catch (SoapFault e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getStatusCode());
            assertEquals("Object 'FOUR' is outside of valid domain 'StringType'. Valid values: [ONE, TWO, THREE]", e.getMessage());
            assertEquals(ExceptionType.VALIDATION.name(), e.getDetail().getElementsByTagName("type").item(0).getTextContent());
        }
    }

    @Test
    public void test_soap_COMPULATION() {
        try {
            IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
            context.setLob("module1");
            lazyCompilationSoap.throwCompilationError(context);
        } catch (SoapFault e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getStatusCode());
            assertEquals("Failed to load lazy method.", e.getMessage());
            assertEquals(ExceptionType.COMPILATION.name(), e.getDetail().getElementsByTagName("type").item(0).getTextContent());
        }
    }
}
