package org.openl.itest;

import static org.junit.Assert.*;

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
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
        rest = new RestClientFactory(baseURI).create();
    }

    @Test
    public void testPost_lowCase() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("number", 1d);
        requestBody.put("string", "a");

        assertReturnsTrue(requestBody, "/upcs/lowCase");
    }

    @Test
    public void testPost_UPCase() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("STRING", "B");
        requestBody.put("NUMBER", 2.5);

        assertReturnsTrue(requestBody, "/upcs/UPCase");
    }

    @Test
    public void testPost_MixedCase() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("numBer", 3d);
        requestBody.put("String", "C");

        assertReturnsTrue(requestBody, "/upcs/MixedCase");
    }

    @Test
    public void testPost_eDGECase() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sTring", "d");
        requestBody.put("NUMBer", Double.NaN);

        assertReturnsTrue(requestBody, "/upcs/eDGECase");
    }

    @Test
    public void testGet_overload() {
        Map<String, Object> requestBody = new HashMap<>();

        String url = "/upcs/overload";
        assertReturnsTrue(requestBody, url, HttpMethod.GET);
    }

    @Test
    public void testGet_overload_int() {
        Map<String, Object> requestBody = new HashMap<>();

        String url = "/upcs/overload2/1";
        assertReturnsTrue(requestBody, url, HttpMethod.GET);
    }

    @Test
    public void testPost_overload_Integer_String() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("I", 1);
        requestBody.put("J", "A");

        String url = "/upcs/overload4";
        assertReturnsTrue(requestBody, url);
    }

    @Test
    public void testPost_overload_int_String() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("i", 1);
        requestBody.put("J", "a");

        String url = "/upcs/overload3";
        assertReturnsTrue(requestBody, url);
    }

    @Test
    public void testPost_overload_Bean() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        Object bean = Class.forName("Bean").getDeclaredConstructor().newInstance();
        requestBody.put("b", bean);

        String url = "/upcs/overload1";
        assertReturnsTrue(requestBody, url);
    }

    @Test
    public void testWadlSchemaForBean() throws XPathExpressionException {
        String result = getWadl();

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(result));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        String pathToType = "/application/grammars/*[local-name()='schema']/*[local-name()='complexType' and @name='Bean']";
        final Node nodeForType = (Node) xpath.evaluate(pathToType, root, XPathConstants.NODE);
        assertEquals("Bean", nodeForType.getAttributes().getNamedItem("name").getTextContent());

        final String pathToSequence = pathToType + "/*[local-name()='sequence']";
        Node sequence = (Node) xpath.evaluate(pathToSequence, root, XPathConstants.NODE);
        NodeList elements = (NodeList) xpath.evaluate(pathToSequence + "/*", root, XPathConstants.NODESET);
        assertEquals(2, elements.getLength());

        Node element = (Node) xpath.evaluate("*[@name='beanBName']", sequence, XPathConstants.NODE);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("tns:BeanB", element.getAttributes().getNamedItem("type").getTextContent());

        element = (Node) xpath.evaluate("*[@name='I']", sequence, XPathConstants.NODE);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("xs:int", element.getAttributes().getNamedItem("type").getTextContent());
        assertEquals("17", element.getAttributes().getNamedItem("default").getTextContent());
    }

    @Test
    public void testWadlSchemaForBeanB() throws XPathExpressionException {
        String result = getWadl();

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(result));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        String pathToType = "/application/grammars/*[local-name()='schema']/*[local-name()='complexType' and @name='BeanB']";
        final Node nodeForType = (Node) xpath.evaluate(pathToType, root, XPathConstants.NODE);
        assertEquals("BeanB", nodeForType.getAttributes().getNamedItem("name").getTextContent());

        final String pathToSequence = pathToType + "/*[local-name()='sequence']";
        Node sequence = (Node) xpath.evaluate(pathToSequence, root, XPathConstants.NODE);
        NodeList elements = (NodeList) xpath.evaluate(pathToSequence + "/*", root, XPathConstants.NODESET);
        assertEquals(2, elements.getLength());

        Node element = (Node) xpath.evaluate("*[@name='In']", sequence, XPathConstants.NODE);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("xs:int", element.getAttributes().getNamedItem("type").getTextContent());

        element = (Node) xpath.evaluate("*[@name='STR']", sequence, XPathConstants.NODE);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("xs:string", element.getAttributes().getNamedItem("type").getTextContent());
    }

    @Test
    public void testWadlSchemaForMixedCase() throws XPathExpressionException {
        String result = getWadl();

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(result));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        String pathToType = "/application/grammars/*[local-name()='schema']/*[local-name()='complexType' and @name='MixedCaseRequest']";
        final Node nodeForType = (Node) xpath.evaluate(pathToType, root, XPathConstants.NODE);
        assertEquals("MixedCaseRequest", nodeForType.getAttributes().getNamedItem("name").getTextContent());

        final String pathToSequence = pathToType + "/*[local-name()='sequence']";
        Node sequence = (Node) xpath.evaluate(pathToSequence, root, XPathConstants.NODE);
        NodeList elements = (NodeList) xpath.evaluate(pathToSequence + "/*", root, XPathConstants.NODESET);
        assertEquals(2, elements.getLength());

        Node element = (Node) xpath.evaluate("*[@name='numBer']", sequence, XPathConstants.NODE);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("xs:double", element.getAttributes().getNamedItem("type").getTextContent());

        element = (Node) xpath.evaluate("*[@name='String']", sequence, XPathConstants.NODE);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("xs:string", element.getAttributes().getNamedItem("type").getTextContent());

        String pathToMethod = "/application/resources/resource/resource[@path='MixedCase']";
        Node nodeForMethod = (Node) xpath.evaluate(pathToMethod, root, XPathConstants.NODE);
        assertNotNull(nodeForMethod);
        String content = pathToMethod + "/method/request/representation[@element='MixedCaseRequest']";
        Node nodeForMethodBody = (Node) xpath.evaluate(content, root, XPathConstants.NODE);
        assertNotNull(nodeForMethodBody);
    }

    @Test
    public void testWadlSchemaForEDGECase() throws XPathExpressionException {
        String result = getWadl();

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(result));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        String pathToType = "/application/grammars/*[local-name()='schema']/*[local-name()='complexType' and @name='EDGECaseRequest']";
        final Node nodeForType = (Node) xpath.evaluate(pathToType, root, XPathConstants.NODE);
        assertEquals("EDGECaseRequest", nodeForType.getAttributes().getNamedItem("name").getTextContent());

        final String pathToSequence = pathToType + "/*[local-name()='sequence']";
        Node sequence = (Node) xpath.evaluate(pathToSequence, root, XPathConstants.NODE);
        NodeList elements = (NodeList) xpath.evaluate(pathToSequence + "/*", root, XPathConstants.NODESET);
        assertEquals(2, elements.getLength());

        Node element = (Node) xpath.evaluate("*[@name='NUMBer']", sequence, XPathConstants.NODE);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("xs:double", element.getAttributes().getNamedItem("type").getTextContent());

        element = (Node) xpath.evaluate("*[@name='sTring']", sequence, XPathConstants.NODE);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("xs:string", element.getAttributes().getNamedItem("type").getTextContent());

        String pathToMethod = "/application/resources/resource/resource[@path='eDGECase']";
        Node nodeForMethod = (Node) xpath.evaluate(pathToMethod, root, XPathConstants.NODE);
        assertNotNull(nodeForMethod);
        String content = pathToMethod + "/method/request/representation[@element='EDGECaseRequest']";
        Node nodeForMethodBody = (Node) xpath.evaluate(content, root, XPathConstants.NODE);
        assertNotNull(nodeForMethodBody);
    }

    private void assertReturnsTrue(Map<String, Object> requestBody, String url) {
        assertReturnsTrue(requestBody, url, HttpMethod.POST);
    }

    private void assertReturnsTrue(Map<String, Object> requestBody, String url, HttpMethod httpMethod) {
        ResponseEntity<Boolean> response = rest.exchange(url,
                httpMethod,
                RestClientFactory.request(requestBody),
                Boolean.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Boolean result = response.getBody();
        assertNotNull(result);
        assertTrue(result);
    }

    private String getWadl() {
        ResponseEntity<String> response = rest.getForEntity("/upcs?_wadl", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String result = response.getBody();
        assertNotNull(result);
        // Cleanup
        return ITestUtil.cleanupXml(result);
    }

}
