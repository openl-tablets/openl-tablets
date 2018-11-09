package org.openl.itest.epbds7654;

import org.apache.cxf.binding.soap.SoapFault;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.core.SoapClientFactory;
import org.openl.itest.epbds7654.project.DayDiffService;
import org.openl.itest.responsedto.ErrorResponse;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RunITest {

    private static JettyServer server;
    private static String baseURI;

    private DayDiffService soapClient;
    private RestTemplate rest;

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
        soapClient = new SoapClientFactory<>(baseURI + "/EPBDS-7654", DayDiffService.class).createProxy();
        rest = new RestClientFactory(baseURI + "/REST/EPBDS-7654").create();
    }

    @Test
    public void testRestResponse() {
        ResponseEntity<Integer> successResponse = rest.exchange("/getDayDiff", HttpMethod.POST, RestClientFactory.request("2018-11-01"), Integer.class);
        assertEquals(HttpStatus.OK, successResponse.getStatusCode());
        assertEquals((Integer) 7, successResponse.getBody());

        ResponseEntity<ErrorResponse> errorResponse = rest.exchange("/getDayDiff", HttpMethod.POST, RestClientFactory.request("123"), ErrorResponse.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse.getStatusCode());
        ErrorResponse error = errorResponse.getBody();
        assertNotNull(error);
        assertEquals(ExceptionType.RULES_RUNTIME.name(), error.getType());
        assertTrue(error.getMessage() != null && error.getMessage().contains("Cause: Unparseable date: \"123\""));
    }

    @Test
    public void testSoapResponse() {
        final Integer actual = soapClient.getDayDiff("2018-11-01");
        assertEquals((Integer) 7, actual);

        try {
            soapClient.getDayDiff("123");
            fail("Oops... Must be failed!");
        } catch (SoapFault e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getStatusCode());
            assertTrue(e.getMessage() != null && e.getMessage().contains("Cause: Unparseable date: \"123\""));
            assertEquals(ExceptionType.RULES_RUNTIME.name(), e.getDetail().getElementsByTagName("type").item(0).getTextContent());
        }
    }
}
