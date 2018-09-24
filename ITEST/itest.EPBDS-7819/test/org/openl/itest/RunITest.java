package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
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
import org.openl.rules.calc.SpreadsheetResult;
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
    public void testSpreadsheetResultWadlSchema() throws XPathExpressionException {
        ResponseEntity<String> response = rest.getForEntity("/wadl-and-spreadsheetresult?_wadl", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String result = response.getBody();
        assertNotNull(result);
        //cleanup xml
        result = result.replaceAll("\\\\\"", "\"").replaceAll(">\\\\n\\s*<", "><");

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        InputSource inputSource = new InputSource(new StringReader(result.substring(1, result.length() - 1)));

        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);
        String pathToSpreadsheetResultSchema = "/application/grammars/*[local-name()='schema']";
        int i = findSpreadsheetResultSchemaPosition(root, xpath, pathToSpreadsheetResultSchema);
        pathToSpreadsheetResultSchema += "[" + (i + 1) + "]";

        final Node spreadsheetResultSchemaNode = (Node) xpath.evaluate(pathToSpreadsheetResultSchema + "/*[local-name()='element']", root, XPathConstants.NODE);
        assertEquals("spreadsheetResult", spreadsheetResultSchemaNode.getAttributes().getNamedItem("name").getTextContent());

        final String pathToComplexType = pathToSpreadsheetResultSchema + "/*[local-name()='complexType']";
        final Node spreadsheetResultComplexTypeNode = (Node) xpath.evaluate(pathToComplexType, root, XPathConstants.NODE);
        assertEquals("spreadsheetResult", spreadsheetResultComplexTypeNode.getAttributes().getNamedItem("name").getTextContent());

        final String pathToSequence = pathToComplexType + "/*[local-name()='sequence']";
        Node element = (Node) xpath.evaluate(pathToSequence, root, XPathConstants.NODE);
        assertEquals(3, element.getChildNodes().getLength());

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='columnNames']", root, XPathConstants.NODE);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("xs:string", element.getAttributes().getNamedItem("type").getTextContent());

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='results']", root, XPathConstants.NODE);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("ns1:anyTypeArray", element.getAttributes().getNamedItem("type").getTextContent());

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='rowNames']", root, XPathConstants.NODE);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("xs:string", element.getAttributes().getNamedItem("type").getTextContent());
    }

    @Test
    public void testPost_tiktakMethod_shouldReturnSpreadsheetResult() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("i", 100);
        requestBody.put("j", "foo");

        ResponseEntity<SpreadsheetResult> response = rest.exchange("/wadl-and-spreadsheetresult/tiktak",
            HttpMethod.POST,
            RestClientFactory.request(requestBody),
            SpreadsheetResult.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        SpreadsheetResult result = response.getBody();
        assertNotNull(result);
        assertEquals(100, result.getFieldValue("$calc$INT"));
        assertEquals("foo", result.getFieldValue("$calc$String"));
    }

    private int findSpreadsheetResultSchemaPosition(Node root, XPath xpath, String path) throws XPathExpressionException {
        NodeList nodeList = (NodeList) xpath.evaluate(path, root, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node schema = nodeList.item(i);
            NodeList childNodes = schema.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                Node childElem = childNodes.item(j);
                if (childElem.hasAttributes()) {
                    Node attrNode = childElem.getAttributes().getNamedItem("name");
                    if (attrNode != null && "spreadsheetResult".equals(attrNode.getNodeValue())) {
                        return i;
                    }
                }
            }
        }
        fail("Cannot find schema for SpreadsheetResult");
        return -1;
    }

}
