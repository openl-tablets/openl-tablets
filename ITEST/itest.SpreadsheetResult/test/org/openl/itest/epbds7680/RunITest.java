package org.openl.itest.epbds7680;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Collections;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.ITestUtils;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.core.XmlMimeInterceptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class RunITest {

    private static final String SOAP_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:tiktak xmlns:ns1=\"http://itest.openl.org/\"><ns1:arg0>100</ns1:arg0><ns1:arg1>foo</ns1:arg1></ns1:tiktak></soap:Body></soap:Envelope>";

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
        rest = new RestClientFactory(baseURI).create();
    }

    @Test
    public void test_jsonResponse_SpreadsheetResult_OK() {
        ResponseEntity<String> response = rest.exchange("/REST/wadl-and-spreadsheetresult/tiktak",
            HttpMethod.POST,
            RestClientFactory.json("{`i`:100, `j`:`foo`}"),
            String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        DocumentContext documentContext = JsonPath.using(Configuration.defaultConfiguration())
            .parse(response.getBody());

        assertEquals((Integer) 2, documentContext.read("$.results.length()"));
        assertEquals((Integer) 1, documentContext.read("$.results[0].length()"));
        assertEquals((Integer) 1, documentContext.read("$.results[1].length()"));
        assertEquals((Integer) 100, documentContext.read("$.results[0][0]"));
        assertEquals("foo", documentContext.read("$.results[1][0]"));
        assertEquals((Integer) 1, documentContext.read("$.columnNames.length()"));
        assertEquals("calc", documentContext.read("$.columnNames[0]"));
        assertEquals((Integer) 2, documentContext.read("$.rowNames.length()"));
        assertEquals("INT", documentContext.read("$.rowNames[0]"));
        assertEquals("String", documentContext.read("$.rowNames[1]"));

        assertEquals((Integer) 3, documentContext.read("$.length()"));
    }

    @Test
    public void test_validate_SpreadsheetResultTypeInWsdlSchema() throws XPathExpressionException {
        
        String wsdlBody = ITestUtils.getWsdlBody(baseURI + "/wadl-and-spreadsheetresult?wsdl");
        assertNotNull(wsdlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wsdlBody));

        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);
        final String spreadsheetResultTypePath = "/*[local-name()='definitions']/*[local-name()='types']/*/*[local-name()='complexType'][@name='SpreadsheetResult']";
        Node spreadsheetResultTypeNode = (Node) xpath.evaluate(spreadsheetResultTypePath, root, XPathConstants.NODE);
        assertNotNull(spreadsheetResultTypeNode);

        final String pathToSequence = spreadsheetResultTypePath + "/*[local-name()='sequence']";

        Node element = (Node) xpath.evaluate(pathToSequence + "/*[@name='columnNames']", root, XPathConstants.NODE);
        assertEquals("xsd:element", element.getNodeName());
        assertEndsWith(":ArrayOfString", element.getAttributes().getNamedItem("type").getTextContent());

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='results']", root, XPathConstants.NODE);
        assertNotNull(element);
        assertEquals("xsd:element", element.getNodeName());
        assertEndsWith(":ArrayOfArrayOfAnyType", element.getAttributes().getNamedItem("type").getTextContent());

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='rowNames']", root, XPathConstants.NODE);
        assertNotNull(element);
        assertEquals("xsd:element", element.getNodeName());
        assertEndsWith(":ArrayOfString", element.getAttributes().getNamedItem("type").getTextContent());

        NodeList elements = (NodeList) xpath.evaluate(pathToSequence + "/*", root, XPathConstants.NODESET);
        assertEquals(3, elements.getLength());
    }

    @Test
    public void test_soapResponse_spreadsheetResult() throws XPathExpressionException {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(new XmlMimeInterceptor()));
        final ResponseEntity<String> response = restTemplate.exchange(baseURI + "/wadl-and-spreadsheetresult",
            HttpMethod.POST,
            new HttpEntity<>(SOAP_REQUEST),
            String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String result = response.getBody();
        assertNotNull(result);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(result));

        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);
        final String spreadsheetResultTypePath = "/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='tiktakResponse']/*[local-name()='return']";
        Node spreadsheetResultTypeNode = (Node) xpath.evaluate(spreadsheetResultTypePath, root, XPathConstants.NODE);
        assertNotNull(spreadsheetResultTypeNode);

        assertEndsWith(":SpreadsheetResult",
            spreadsheetResultTypeNode.getAttributes()
                .getNamedItemNS("http://www.w3.org/2001/XMLSchema-instance", "type")
                .getTextContent());

        Node element = (Node) xpath
            .evaluate(spreadsheetResultTypePath + "/*[local-name()='columnNames']", root, XPathConstants.NODE);
        assertNotNull(element);

        element = (Node) xpath
            .evaluate(spreadsheetResultTypePath + "/*[local-name()='results']", root, XPathConstants.NODE);
        assertNotNull(element);

        element = (Node) xpath
            .evaluate(spreadsheetResultTypePath + "/*[local-name()='rowNames']", root, XPathConstants.NODE);
        assertNotNull(element);

        NodeList elements = (NodeList) xpath.evaluate(spreadsheetResultTypePath + "/*", root, XPathConstants.NODESET);
        assertEquals(3, elements.getLength());
    }

    private static void assertEndsWith(String expected, String actual) {
        assertNotNull(actual);
        String msg = String.format("An actual value '%s' must end with '%s'", actual, expected);
        assertTrue(msg, actual.endsWith(expected));
    }

}
