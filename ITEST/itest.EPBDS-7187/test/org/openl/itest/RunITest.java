package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.openl.itest.core.RestClientFactory.json;

import java.io.StringReader;

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
        ResponseEntity<String> response = rest
            .postForEntity("/upcs/lowCase", json("{ `number`:1.0, `string`:`a`}"), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("true", response.getBody());
    }

    @Test
    public void testPost_UPCase() {
        ResponseEntity<String> response = rest
            .postForEntity("/upcs/UPCase", json("{ `STRING`:`B`, `NUMBER`:2.5}"), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("true", response.getBody());
    }

    @Test
    public void testPost_MixedCase() {
        ResponseEntity<String> response = rest
            .postForEntity("/upcs/MixedCase", json("{ `numBer`:3.0, `String`:`C`}"), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("true", response.getBody());
    }

    @Test
    public void testPost_eDGECase() {
        ResponseEntity<String> response = rest
            .postForEntity("/upcs/eDGECase", json("{ `sTring`:`d`, `NUMBer`:`NaN`}"), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("true", response.getBody());
    }

    @Test
    public void testGet_overload() {
        ResponseEntity<String> response = rest.getForEntity("/upcs/overload", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("true", response.getBody());
    }

    @Test
    public void testGet_overload_int() {
        ResponseEntity<String> response = rest.getForEntity("/upcs/overload2/1", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("true", response.getBody());
    }

    @Test
    public void testPost_overload_Integer_String() {
        ResponseEntity<String> response = rest
            .postForEntity("/upcs/overload4", json("{ `I`:1, `J`:`A`}"), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("true", response.getBody());
    }

    @Test
    public void testPost_overload_int_String() {
        ResponseEntity<String> response = rest
            .postForEntity("/upcs/overload3", json("{ `i`:1, `J`:`a`}"), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("true", response.getBody());
    }

    @Test
    public void testPost_overload_Bean() {
        ResponseEntity<String> response = rest.postForEntity("/upcs/overload1",
            json("{ `b` : {`I`:17, `BeanBName` : {`STR` : null, `In` : 0 } }}"),
            String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("true", response.getBody());
    }

    @Test
    public void testWadlSchemaForBean() throws XPathExpressionException {
        String wadlBody = ITestUtils.getWadlBody(baseURI + "/upcs?_wadl");
        assertNotNull(wadlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wadlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        String pathToType = "/*[local-name()='application']/*[local-name()='grammars']/*[local-name()='schema']/*[local-name()='complexType' and @name='Bean']";
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
        String wadlBody = ITestUtils.getWadlBody(baseURI + "/upcs?_wadl");
        assertNotNull(wadlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wadlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        String pathToType = "/*[local-name()='application']/*[local-name()='grammars']/*[local-name()='schema']/*[local-name()='complexType' and @name='BeanB']";
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
        String wadlBody = ITestUtils.getWadlBody(baseURI + "/upcs?_wadl");
        assertNotNull(wadlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wadlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        String pathToType = "/*[local-name()='application']/*[local-name()='grammars']/*[local-name()='schema']/*[local-name()='complexType' and @name='MixedCaseRequest']";
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

        String pathToMethod = "/*[local-name()='application']/*[local-name()='resources']/*[local-name()='resource']/*[local-name()='resource' and @path='MixedCase']";
        Node nodeForMethod = (Node) xpath.evaluate(pathToMethod, root, XPathConstants.NODE);
        assertNotNull(nodeForMethod);
        String content = pathToMethod + "/*[local-name()='method']/*[local-name()='request']/*[local-name()='representation']";
        Node nodeForMethodBody = (Node) xpath.evaluate(content, root, XPathConstants.NODE);
        String elementValue = nodeForMethodBody.getAttributes().getNamedItem("element").getTextContent();
        if (elementValue.indexOf(":") > 0) {
            assertEquals("MixedCaseRequest", elementValue.substring(elementValue.indexOf(":") + 1));
        } else {
            assertEquals("MixedCaseRequest", elementValue);
        }
        assertNotNull(nodeForMethodBody);
    }

    @Test
    public void testWadlSchemaForEDGECase() throws XPathExpressionException {
        String wadlBody = ITestUtils.getWadlBody(baseURI + "/upcs?_wadl");
        assertNotNull(wadlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wadlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        String pathToType = "/*[local-name()='application']/*[local-name()='grammars']/*[local-name()='schema']/*[local-name()='complexType' and @name='EDGECaseRequest']";
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

        String pathToMethod = "/*[local-name()='application']/*[local-name()='resources']/*[local-name()='resource']/*[local-name()='resource' and @path='eDGECase']";
        Node nodeForMethod = (Node) xpath.evaluate(pathToMethod, root, XPathConstants.NODE);
        assertNotNull(nodeForMethod);
        String content = pathToMethod + "/*[local-name()='method']/*[local-name()='request']/*[local-name()='representation']";
        Node nodeForMethodBody = (Node) xpath.evaluate(content, root, XPathConstants.NODE);
        String elementValue = nodeForMethodBody.getAttributes().getNamedItem("element").getTextContent();
        if (elementValue.indexOf(":") > 0) {
            assertEquals("EDGECaseRequest", elementValue.substring(elementValue.indexOf(":") + 1));
        } else {
            assertEquals("EDGECaseRequest", elementValue);
        }
        assertNotNull(nodeForMethodBody);
    }

}
