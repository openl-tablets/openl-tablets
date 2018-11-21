package org.openl.itest;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.responsedto.ErrorResponse;
import org.openl.itest.service.internal.MyType;
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
        rest = new RestClientFactory(baseURI + "/v1/string/toNumber").create();

    }

    @Test
    @Ignore //FIXME: when EPBDS-7975 will be fixed
    public void call_parse_interfaceMethod_shouldBeCalledSuccessfully_OK() {
        ResponseEntity<Integer> response = rest
            .exchange("/parse", HttpMethod.POST, RestClientFactory.request("1001"), Integer.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Integer) 1001, response.getBody());
    }

    @Test
    @Ignore //FIXME: when EPBDS-7975 will be fixed
    public void call_parse_interfaceMethod_shouldReturn_UNPROCESSABLE_ENTITY() {
        ResponseEntity<Parse2Dto> response = rest
                .exchange("/parse", HttpMethod.POST, RestClientFactory.request("B"), Parse2Dto.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        Parse2Dto body = response.getBody();
        assertNotNull(body);
        assertContains(body.getBody(), "not acceptable");
    }

    @Test
    @Ignore //FIXME: when EPBDS-7975 will be fixed
    public void call_parse1_interfaceMethod_thenInvokeBeforeInterceptorAndThrowAnException_thenInvokeAfterInterceptorAndWrapException_OK() {
        ResponseEntity<MyType> response = rest
            .exchange("/parse1", HttpMethod.POST, RestClientFactory.request("throwBeforeCall"), MyType.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        MyType body = response.getBody();
        assertNotNull(body);
        assertEquals("ERROR", body.getStatus());
        assertEquals(-1, body.getCode());
    }

    @Test
    @Ignore //FIXME: when EPBDS-7975 will be fixed
    public void call_parse1_interfaceMethod_thenInvokeAfterInterceptorAndWrapResponse_OK() {
        ResponseEntity<MyType> response = rest
            .exchange("/parse1", HttpMethod.POST, RestClientFactory.request("11"), MyType.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        MyType body = response.getBody();
        assertNotNull(body);
        assertEquals("PARSED", body.getStatus());
        assertEquals(11, body.getCode());
    }

    @Test
    public void call_parse2_interfaceMethod_thenInvokeAfterInterceptorAndWrapException_NOT_ACCEPTABLE() {
        ResponseEntity<Parse2Dto> response = rest
            .exchange("/parse2", HttpMethod.POST, RestClientFactory.request("A"), Parse2Dto.class);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
        Parse2Dto body = response.getBody();
        assertNotNull(body);
        assertContains(body.getBody(), "A is not valid");
    }

    @Test
    public void call_parse2_interfaceMethod_thenInvokeAfterInterceptorAndWrapResponse_OK() {
        ResponseEntity<Parse2Dto> response = rest
            .exchange("/parse2", HttpMethod.POST, RestClientFactory.request("11"), Parse2Dto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Parse2Dto body = response.getBody();
        assertNotNull(body);
        assertEquals("21", body.getBody());
    }

    @Test
    public void call_parse3_interfaceMethod_usingCustomEndpoint_OK() {
        ResponseEntity<Integer> response = rest.getForEntity("/parse/111", Integer.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Integer) 211, response.getBody());
    }

    @Test
    public void call_parse3_interfaceMethod_usingCustomEndpoint_errorResponseMustBeReturned_UNPROCESSABLE_ENTITY() {
        ResponseEntity<ErrorResponse> response = rest.getForEntity("/parse/A", ErrorResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("A is not valid", body.getMessage());
        assertEquals(ExceptionType.USER_ERROR.name(), body.getType());
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_OK() throws XPathExpressionException {
        ResponseEntity<String> response = rest.postForEntity("/parseX", "11", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertMyTypeEquals(new MyType("PARSED", 111), response.getBody());
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_shouldReturnError_OK() throws XPathExpressionException {
        ResponseEntity<String> response = rest.postForEntity("/parseX", "A", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertMyTypeEquals(new MyType("ERROR", -1), response.getBody());
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_shouldThrowErrorInBeforeInterceptor_OK() throws XPathExpressionException {
        ResponseEntity<String> response = rest.postForEntity("/parseX", "throwBeforeCall", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertMyTypeEquals(new MyType("ERROR", -1), response.getBody());
    }

    @Test
    public void call_parse4_interfaceMethod_usingCustomEndpointAndHeaders_shouldReturn_UNSUPPORTED_MEDIA_TYPE() {
        ResponseEntity<String> response = rest
            .exchange("/parseX", HttpMethod.POST, RestClientFactory.request("throwBeforeCall"), String.class);
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());

        assertEquals("WebApplicationException has been caught, status: 415, message: HTTP 415 Unsupported Media Type",
            response.getBody());
    }

    @Test
    public void call_virtual_interfaceMethod_OK() {
        ResponseEntity<Double> response = rest
            .exchange("/virtual", HttpMethod.POST, RestClientFactory.request("1001"), Double.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Double) 1501.5d, response.getBody());
    }

    @Test
    public void call_virtual_interfaceMethod_UNPROCESSABLE_ENTITY() {
        ResponseEntity<ErrorResponse> response = rest
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
        ResponseEntity<Double> response = rest
            .exchange("/virtual2", HttpMethod.POST, RestClientFactory.request(requestBody), Double.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Double) 1666.5d, response.getBody());
    }

    @Test
    public void call_pong_interfaceMethod_OK() {
        ResponseEntity<MyType> response = rest
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
        ResponseEntity<ReturnTypeDto> response = rest
            .exchange("/process", HttpMethod.POST, RestClientFactory.request(requestBody), ReturnTypeDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ReturnTypeDto body = response.getBody();

        assertEquals("bar", body.getName());
        assertEquals((Double) 10d, body.getAge());
    }

    @Test
    public void call__notExcludedBecauseof_p__rulesMethod_OK() {
        ResponseEntity<Integer> response = rest
            .exchange("/notExcludedBecauseof_p_", HttpMethod.POST, RestClientFactory.request("1001"), Integer.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Integer) (-999), response.getBody());
    }

    @Test
    public void call_excluded_rulesMethod_NOT_FOUND() {
        ResponseEntity<String> response = rest
            .exchange("/excluded", HttpMethod.POST, RestClientFactory.request("1001"), String.class);

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
