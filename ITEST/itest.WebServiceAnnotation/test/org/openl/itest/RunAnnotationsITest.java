package org.openl.itest;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.core.SoapClientFactory;
import org.openl.itest.responsedto.ErrorResponse;
import org.openl.itest.service.internal.MyType;
import org.openl.itest.serviceclass.MyService;
import org.openl.itest.serviceclass.internal.Response;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class RunAnnotationsITest {

    private static JettyServer server;
    private static String baseURI;

    private RestTemplate annotationClassRest;
    private RestTemplate serviceClassRest;
    private RestTemplate serviceClassNegativeRest;
    private MyService soapClient;
    private RestTemplate runTestServiceRest;

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
        annotationClassRest = new RestClientFactory(baseURI + "/v1/string/toNumber").create();
        serviceClassRest = new RestClientFactory(baseURI + "/REST/ws-serviceclass-positive").create();
        serviceClassNegativeRest = new RestClientFactory(baseURI + "/REST/ws-serviceclass-negative").create();
        soapClient = new SoapClientFactory<>(baseURI + "/ws-serviceclass-positive", MyService.class).createProxy();
        runTestServiceRest = new RestClientFactory(baseURI + "/REST/rules-tests-and-run-tables").create();
    }

    @Test
    public void call_parse_interfaceMethod_shouldBeCalledSuccessfully_OK() {
        //parse method was bound to the "/parse1" endpoint
        ResponseEntity<Integer> response = annotationClassRest
            .exchange("/parse1", HttpMethod.POST, RestClientFactory.request("1001"), Integer.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Integer) 1001, response.getBody());
    }

    @Test
    public void call_parse_interfaceMethod_shouldReturn_UNPROCESSABLE_ENTITY() {
        ResponseEntity<ErrorResponse> response = annotationClassRest
                .exchange("/parse1", HttpMethod.POST, RestClientFactory.request("B"), ErrorResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("not acceptable", body.getMessage());
        assertEquals(ExceptionType.USER_ERROR.name(), body.getType());
    }

    @Test
    public void call_parse1_interfaceMethod_shouldReturn_NOT_FOUND() {
        ResponseEntity<String> response = annotationClassRest
                .exchange("/parse11", HttpMethod.POST, RestClientFactory.request("B"), String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void call_parse2_interfaceMethod_thenInvokeAfterInterceptorAndWrapException_NOT_ACCEPTABLE() {
        ResponseEntity<Parse2Dto> response = annotationClassRest
            .exchange("/parse2", HttpMethod.POST, RestClientFactory.request("A"), Parse2Dto.class);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
        Parse2Dto body = response.getBody();
        assertNotNull(body);
        assertContains(body.getBody(), "A is not valid");
    }

    @Test
    public void call_parse2_interfaceMethod_thenInvokeAfterInterceptorAndWrapResponse_OK() {
        ResponseEntity<Parse2Dto> response = annotationClassRest
            .exchange("/parse2", HttpMethod.POST, RestClientFactory.request("11"), Parse2Dto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Parse2Dto body = response.getBody();
        assertNotNull(body);
        assertEquals("21", body.getBody());
    }

    @Test
    public void call_parse3_interfaceMethod_usingCustomEndpoint_OK() {
        ResponseEntity<Integer> response = annotationClassRest.getForEntity("/parse/111", Integer.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Integer) 211, response.getBody());
    }

    @Test
    public void call_parse3_interfaceMethod_usingCustomEndpoint_errorResponseMustBeReturned_UNPROCESSABLE_ENTITY() {
        ResponseEntity<ErrorResponse> response = annotationClassRest.getForEntity("/parse/A", ErrorResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("A is not valid", body.getMessage());
        assertEquals(ExceptionType.USER_ERROR.name(), body.getType());
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_OK() throws XPathExpressionException {
        ResponseEntity<String> response = annotationClassRest.postForEntity("/parseX", "11", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertMyTypeEquals(new MyType("PARSED", 111), response.getBody());
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_shouldReturnError_OK() throws XPathExpressionException {
        ResponseEntity<String> response = annotationClassRest.postForEntity("/parseX", "A", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertMyTypeEquals(new MyType("ERROR", -1), response.getBody());
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_shouldThrowErrorInBeforeInterceptor_OK() throws XPathExpressionException {
        ResponseEntity<String> response = annotationClassRest.postForEntity("/parseX", "throwBeforeCall", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertMyTypeEquals(new MyType("ERROR", -1), response.getBody());
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_shouldReturn_UNSUPPORTED_MEDIA_TYPE() {
        ResponseEntity<String> response = annotationClassRest
            .exchange("/parseX", HttpMethod.POST, RestClientFactory.request("throwBeforeCall"), String.class);
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());

        assertEquals("WebApplicationException has been caught, status: 415, message: HTTP 415 Unsupported Media Type",
            response.getBody());
    }

    @Test
    public void call_virtual_interfaceMethod_OK() {
        ResponseEntity<Double> response = annotationClassRest
            .exchange("/virtual", HttpMethod.POST, RestClientFactory.request("1001"), Double.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Double) 1501.5d, response.getBody());
    }

    @Test
    public void call_virtual_interfaceMethod_UNPROCESSABLE_ENTITY() {
        ResponseEntity<ErrorResponse> response = annotationClassRest
            .exchange("/virtual", HttpMethod.POST, RestClientFactory.request("A"), ErrorResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("A is not valid", body.getMessage());
        assertEquals(ExceptionType.USER_ERROR.name(), body.getType());
    }

    @Test
    public void call_virtual2_interfaceMethod_withNamedParameters_OK() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("first", 1111);
        requestBody.put("second", "FOO");
        ResponseEntity<Double> response = annotationClassRest
            .exchange("/virtual2", HttpMethod.POST, RestClientFactory.request(requestBody), Double.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Double) 1666.5d, response.getBody());
    }

    @Test
    public void call_pong_interfaceMethod_OK() {
        ResponseEntity<MyType> response = annotationClassRest
            .exchange("/ping", HttpMethod.POST, RestClientFactory.request(new MyType("GOOD", 101)), MyType.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        MyType body = response.getBody();
        assertEquals("pong", body.getStatus());
        assertEquals(-101, body.getCode());
    }

    @Test
    public void call_process_rulesMethod_OK() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("nick", "bar");
        requestBody.put("month", 120);
        ResponseEntity<ReturnTypeDto> response = annotationClassRest
            .exchange("/process", HttpMethod.POST, RestClientFactory.request(requestBody), ReturnTypeDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ReturnTypeDto body = response.getBody();

        assertEquals("bar", body.getName());
        assertEquals((Double) 10d, body.getAge());
    }

    @Test
    public void call__notExcludedBecauseof_p__rulesMethod_OK() {
        ResponseEntity<Integer> response = annotationClassRest
            .exchange("/notExcludedBecauseof_p_", HttpMethod.POST, RestClientFactory.request("1001"), Integer.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Integer) (-999), response.getBody());
    }

    @Test
    public void call_excluded_rulesMethod_NOT_FOUND() {
        ResponseEntity<String> response = annotationClassRest
            .exchange("/excluded", HttpMethod.POST, RestClientFactory.request("1001"), String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void test_doSomething_REST() {
        ResponseEntity<Long> response = serviceClassRest
                .exchange("/doSomething", HttpMethod.POST, RestClientFactory.request("1001"), Long.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Long) 1001L, response.getBody());
    }

    @Test
    public void test_doSomething_SOAP() {
        assertEquals((Long) 1001L, soapClient.doSomething("1001"));
    }

    @Test
    public void test_doArray_REST() {
        ResponseEntity<long[]> response = serviceClassRest
                .getForEntity("/doArray", long[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().length);
    }

    @Test
    public void test_doArray_SOAP() {
        assertEquals(3, soapClient.doArray().length);
    }

    @Test
    public void test_voidMethod_REST() {
        ResponseEntity<Void> response = serviceClassRest.getForEntity("/voidMethod", Void.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void test_voidMethod_SOAP() {
        soapClient.voidMethod();
    }

    @Test
    public void test_voidMethodWithAfterReturnInterceptor_REST() {
        ResponseEntity<Response> response = serviceClassRest.getForEntity("/voidMethodWithAfterReturnInterceptor", Response.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Response body = response.getBody();
        assertNotNull(body);
        assertEquals("PASSED", body.getStatus());
        assertEquals(0, body.getCode());
    }

    @Test
    public void test_voidMethodWithAfterReturnInterceptor_SOAP() {
        Response body = soapClient.voidMethodWithAfterReturnInterceptor();
        assertNotNull(body);
        assertEquals("PASSED", body.getStatus());
        assertEquals(0, body.getCode());
    }

    @Test
    public void test_longMethodWithAfterReturnInterceptor_REST() {
        ResponseEntity<Response> response = serviceClassRest
                .exchange("/longMethodWithAfterReturnInterceptor", HttpMethod.POST, RestClientFactory.request("1111"), Response.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Response body = response.getBody();
        assertNotNull(body);
        assertEquals("SUCCESS", body.getStatus());
        assertEquals(1111, body.getCode());
    }

    @Test
    public void test_longMethodWithAfterReturnInterceptor_SOAP() {
        Response body = soapClient.longMethodWithAfterReturnInterceptor("1111");
        assertNotNull(body);
        assertEquals("SUCCESS", body.getStatus());
        assertEquals(1111, body.getCode());
    }

    @Test
    public void test_longMethodWithUpcast_REST() {
        ResponseEntity<Long> response = serviceClassRest
                .exchange("/longMethodWithUpcast", HttpMethod.POST, RestClientFactory.request("1111"), Long.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Long) 1111L, response.getBody());
    }

    @Test
    public void test_longMethodWithUpcast_SOAP() {
        final Number expected = BigDecimal.valueOf(1111L);
        assertEquals(expected, soapClient.longMethodWithUpcast("1111"));
    }

    @Test
    public void test_aroundLongMethod_REST() {
        ResponseEntity<Response> response = serviceClassRest
                .exchange("/aroundLongMethod", HttpMethod.POST, RestClientFactory.request("1111"), Response.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Response body = response.getBody();
        assertNotNull(body);
        assertEquals("SUCCESS", body.getStatus());
        assertEquals(1111, body.getCode());
    }

    @Test
    public void test_aroundLongMethod_SOAP() {
        Response body = soapClient.aroundLongMethod("1111");
        assertNotNull(body);
        assertEquals("SUCCESS", body.getStatus());
        assertEquals(1111, body.getCode());
    }

    @Test
    public void test_aroundVoidMethod_REST() {
        ResponseEntity<Response> response = serviceClassRest.getForEntity("/aroundVoidMethod", Response.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Response body = response.getBody();
        assertNotNull(body);
        assertEquals("SUCCESS", body.getStatus());
        assertEquals(0, body.getCode());
    }

    @Test
    public void test_aroundVoidMethod_SOAP() {
        Response body = soapClient.aroundVoidMethod();
        assertNotNull(body);
        assertEquals("SUCCESS", body.getStatus());
        assertEquals(0, body.getCode());
    }

    @Test
    public void test_doSomething_negative_REST() {
        ResponseEntity<String> response = serviceClassNegativeRest.exchange("/doSomething", HttpMethod.POST, RestClientFactory.request("1001"), String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void test_runTestTables_calculatePremium_REST() {
        ResponseEntity<SpreadsheetResult> response = runTestServiceRest.exchange("/calculatePremium", HttpMethod.POST,
                RestClientFactory.request("{\"covName\": \"Cov1\", \"amount\": 1000}"), SpreadsheetResult.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SpreadsheetResult body = response.getBody();
        assertNotNull(body);
        assertEquals((Double) 0.1, body.getFieldValue("$Formula$Factor"));
        assertEquals((Double) 100., body.getFieldValue("$Formula$Premium"));
        assertEquals((Double) 0., body.getFieldValue("$Formula$Discount"));
        assertEquals((Double) 0., body.getFieldValue("$Formula$DiscountAmt"));
        assertEquals((Double) 100., body.getFieldValue("$Formula$FinalPremium"));


    }

    @Test
    public void test_runTestTables_calculatePremium_ValidationError_REST() {
        ResponseEntity<ErrorResponse> response = runTestServiceRest.exchange("/calculatePremium", HttpMethod.POST,
                RestClientFactory.request("{\"covName\": \"Cov100\", \"amount\": 1000}"), ErrorResponse.class);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Object 'Cov100' is outside of valid domain 'CoverageName'. Valid values: [Cov1, Cov2, Cov3, Cov4]", body.getMessage());
        assertEquals(ExceptionType.VALIDATION.name(), body.getType());
    }

    @Test
    public void test_runTestTables_calculatePremiumTest_REST() {
        ResponseEntity<Void> response = runTestServiceRest.getForEntity("/calculatePremiumTest", Void.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void test_runTestTables_getFactor_REST() {
        ResponseEntity<String> response = runTestServiceRest.exchange("/getFactor", HttpMethod.POST,
                RestClientFactory.request("Cov1"), String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void test_runTestTables_getDiscount_REST() {
        ResponseEntity<String> response = runTestServiceRest.exchange("/getDiscount", HttpMethod.POST,
                RestClientFactory.request("100"), String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void test_runTestTables_calculatePremiumRun_REST() {
        ResponseEntity<Void> response = runTestServiceRest.getForEntity("/calculatePremiumRun", Void.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private void assertMyTypeEquals(MyType expected, String xml) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(ITestUtil.cleanupXml(xml)));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        Node statusNode = (Node) xpath.evaluate("/myType/status", root, XPathConstants.NODE);
        assertNotNull(statusNode);
        assertEquals(expected.getStatus(), statusNode.getTextContent());

        Node codeNode = (Node) xpath.evaluate("/myType/code", root, XPathConstants.NODE);
        assertNotNull(statusNode);
        assertEquals(String.valueOf(expected.getCode()), codeNode.getTextContent());
    }

    private void assertContains(String text, String expected) {
        assertNotNull(text);
        assertTrue(text, text.contains(expected));
    }

    private static class Parse2Dto {

        private String body;

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    private static class ReturnTypeDto {

        private String name;
        private Double age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getAge() {
            return age;
        }

        public void setAge(Double age) {
            this.age = age;
        }
    }

}
