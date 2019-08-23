package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.ITestUtils;
import org.openl.itest.core.JettyServer;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class RunITest {

    private static JettyServer server;
    private static String baseURI;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer();
        baseURI = server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    private void validateComplexTypeWadl(final Node root,
            final XPath xpath,
            String name) throws XPathExpressionException {
        final String pathToType = "/*[local-name()='application']/*[local-name()='grammars']/*[local-name()='schema']/*[local-name()='complexType' and @name='" + name + "']";
        final Node nodeForType = (Node) xpath.evaluate(pathToType, root, XPathConstants.NODE);
        assertEquals(name, nodeForType.getAttributes().getNamedItem("name").getTextContent());
    }

    private void validateComplexTypeWsdl(final Node root,
            final XPath xpath,
            String name) throws XPathExpressionException {
        final String pathToType = "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='" + name + "']";
        final Node nodeForType = (Node) xpath.evaluate(pathToType, root, XPathConstants.NODE);
        assertEquals(name, nodeForType.getAttributes().getNamedItem("name").getTextContent());
    }

    @Test
    public void testWadlSchema() throws XPathExpressionException {
        String wadlBody = ITestUtils.getWadlBody(baseURI + "/REST/deployments/simple1?_wadl");
        assertNotNull(wadlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wadlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        validateComplexTypeWadl(root, xpath, "codeStep");
        validateComplexTypeWadl(root, xpath, "calculationStep");
        validateComplexTypeWadl(root, xpath, "compoundStep");
        validateComplexTypeWadl(root, xpath, "simpleStep");

        // Variations
        validateComplexTypeWadl(root, xpath, "argumentReplacementVariation");
        validateComplexTypeWadl(root, xpath, "noVariation");
        validateComplexTypeWadl(root, xpath, "complexVariation");
        validateComplexTypeWadl(root, xpath, "jxPathVariation");
        validateComplexTypeWadl(root, xpath, "deepCloningVariation");
        validateComplexTypeWadl(root, xpath, "variationsResult");

        // Spreadsheet Result Return Type
        final String pathToType = "/*[local-name()='application']/*[local-name()='resources']/*[local-name()='resource' and @path='/']/*[local-name()='resource' and @path='test']/*[local-name()='method']/*[local-name()='response']/*[local-name()='representation']";
        final Node nodeForType = (Node) xpath.evaluate(pathToType, root, XPathConstants.NODE);
        String elementValue = nodeForType.getAttributes().getNamedItem("element").getTextContent();
        if (elementValue.indexOf(":") > 0) {
            assertEquals("spreadsheetResult", elementValue.substring(elementValue.indexOf(":") + 1));
        } else {
            assertEquals("spreadsheetResult", elementValue);
        }
    }

    @Test
    public void testWsdlSchema() throws XPathExpressionException {
        String wsdlBody = ITestUtils.getWsdlBody(baseURI + "/deployments/simple1?wsdl");
        assertNotNull(wsdlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wsdlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        validateComplexTypeWsdl(root, xpath, "CodeStep");
        validateComplexTypeWsdl(root, xpath, "CalculationStep");
        validateComplexTypeWsdl(root, xpath, "CompoundStep");
        validateComplexTypeWsdl(root, xpath, "SimpleStep");

        // Variations
        validateComplexTypeWsdl(root, xpath, "ArgumentReplacementVariation");
        validateComplexTypeWsdl(root, xpath, "NoVariation");
        validateComplexTypeWsdl(root, xpath, "ComplexVariation");
        validateComplexTypeWsdl(root, xpath, "JXPathVariation");
        validateComplexTypeWsdl(root, xpath, "DeepCloningVariation");
        validateComplexTypeWsdl(root, xpath, "VariationsResult");

        // Spreadsheet Result Return Type
        final String pathToType = "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='testResponse']/*[local-name()='sequence']/*[local-name()='element']";
        final Node nodeForType = (Node) xpath.evaluate(pathToType, root, XPathConstants.NODE);
        String typeValue = nodeForType.getAttributes().getNamedItem("type").getTextContent();
        if (typeValue.indexOf(":") > 0) {
            assertEquals("SpreadsheetResult", typeValue.substring(typeValue.indexOf(":") + 1));
        } else {
            assertEquals("SpreadsheetResult", typeValue);
        }
    }
}
