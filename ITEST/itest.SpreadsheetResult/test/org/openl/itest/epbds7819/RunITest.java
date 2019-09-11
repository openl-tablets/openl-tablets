package org.openl.itest.epbds7819;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.StringReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.MainService;
import org.openl.itest.core.ITestUtils;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.core.SoapClientFactory;
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
    private MainService soapService;

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
        rest = new RestClientFactory(baseURI + "/REST/wadl-and-spreadsheetresult").create();
        soapService = new SoapClientFactory<>(baseURI + "/wadl-and-spreadsheetresult", MainService.class).createProxy();
    }

    @Test
    public void testSpreadsheetResultWadlSchema() throws XPathExpressionException {
        String wadlBody = ITestUtils.getWadlBody(baseURI + "/REST/wadl-and-spreadsheetresult?_wadl");
        assertNotNull(wadlBody);

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        InputSource inputSource = new InputSource(new StringReader(wadlBody));

        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);
        String pathToSpreadsheetResultSchema = "/*[local-name()='application']/*[local-name()='grammars']/*[local-name()='schema']";
        int i = findSpreadsheetResultSchemaPosition(root, xpath, pathToSpreadsheetResultSchema);
        pathToSpreadsheetResultSchema += "[" + (i + 1) + "]";

        final Node spreadsheetResultSchemaNode = (Node) xpath
            .evaluate(pathToSpreadsheetResultSchema + "/*[local-name()='element']", root, XPathConstants.NODE);
        assertEquals("spreadsheetResult",
            spreadsheetResultSchemaNode.getAttributes().getNamedItem("name").getTextContent());

        final String pathToComplexType = pathToSpreadsheetResultSchema + "/*[local-name()='complexType']";
        final Node spreadsheetResultComplexTypeNode = (Node) xpath
            .evaluate(pathToComplexType, root, XPathConstants.NODE);
        assertEquals("spreadsheetResult",
            spreadsheetResultComplexTypeNode.getAttributes().getNamedItem("name").getTextContent());

        final String pathToSequence = pathToComplexType + "/*[local-name()='sequence']";

        Node element = (Node) xpath.evaluate(pathToSequence + "/*[@name='columnNames']", root, XPathConstants.NODE);
        assertNotNull(element);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("xs:string", element.getAttributes().getNamedItem("type").getTextContent());

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='results']", root, XPathConstants.NODE);
        assertNotNull(element);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("ns1:anyTypeArray", element.getAttributes().getNamedItem("type").getTextContent());

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='rowNames']", root, XPathConstants.NODE);
        assertNotNull(element);
        assertEquals("xs:element", element.getNodeName());
        assertEquals("xs:string", element.getAttributes().getNamedItem("type").getTextContent());

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='height']", root, XPathConstants.NODE);
        assertNull(element);

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='width']", root, XPathConstants.NODE);
        assertNull(element);

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='rowTitles']", root, XPathConstants.NODE);
        assertNull(element);

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='columnTitles']", root, XPathConstants.NODE);
        assertNull(element);

        element = (Node) xpath.evaluate(pathToSequence + "/*[@name='logicalTable']", root, XPathConstants.NODE);
        assertNull(element);

        NodeList elements = (NodeList) xpath.evaluate(pathToSequence + "/*", root, XPathConstants.NODESET);
        assertEquals(3, elements.getLength());
    }

    @Test
    public void testPost_tiktakMethod_shouldReturnSpreadsheetResult() {
        ResponseEntity<SpreadsheetResult> response = rest
            .exchange("/tiktak", HttpMethod.POST, RestClientFactory.json("{`i`:100, `j`:`foo`}"), SpreadsheetResult.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        SpreadsheetResult result = response.getBody();
        assertNotNull(result);
        assertEquals(100, result.getFieldValue("$calc$INT"));
        assertEquals("foo", result.getFieldValue("$calc$String"));
        assertNull(result.getLogicalTable());
    }

    @Test
    public void testSoap_tiktakMethod_shouldReturnSpreadsheetResult() {
        SpreadsheetResult result = soapService.tiktak(100, "foo");
        assertNotNull(result);
        assertEquals(100, result.getFieldValue("$calc$INT"));
        assertEquals("foo", result.getFieldValue("$calc$String"));
    }

    private int findSpreadsheetResultSchemaPosition(Node root,
            XPath xpath,
            String path) throws XPathExpressionException {
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
