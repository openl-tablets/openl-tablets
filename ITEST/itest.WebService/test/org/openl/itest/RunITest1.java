package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;

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
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class RunITest1 {

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
            String complexTypeName,
            boolean exists) throws XPathExpressionException {
        final String path = "/*[local-name()='application']/*[local-name()='grammars']/*[local-name()='schema']/*[local-name()='complexType' and @name='" + complexTypeName + "']";
        final Node node = (Node) xpath.evaluate(path, root, XPathConstants.NODE);
        if (exists) {
            assertEquals(complexTypeName, node.getAttributes().getNamedItem("name").getTextContent());
        } else {
            assertNull(node);
        }
    }

    private void validateComplexTypeWsdl(final Node root,
            final XPath xpath,
            String complexTypeName,
            boolean exists) throws XPathExpressionException {
        final String path = "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='" + complexTypeName + "']";
        final Node node = (Node) xpath.evaluate(path, root, XPathConstants.NODE);
        if (exists) {
            assertEquals(complexTypeName, node.getAttributes().getNamedItem("name").getTextContent());
        } else {
            assertNull(node);
        }
    }

    @Test
    public void testWadlSchemaSimple1() throws XPathExpressionException {
        String wadlBody = ITestUtils.getWadlBody(baseURI + "/REST/deployment1/simple1?_wadl");
        assertNotNull(wadlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wadlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        validateComplexTypeWadl(root, xpath, "codeStep", true);
        validateComplexTypeWadl(root, xpath, "calculationStep", true);
        validateComplexTypeWadl(root, xpath, "compoundStep", true);
        validateComplexTypeWadl(root, xpath, "simpleStep", true);

        // Variations
        validateComplexTypeWadl(root, xpath, "argumentReplacementVariation", true);
        validateComplexTypeWadl(root, xpath, "noVariation", true);
        validateComplexTypeWadl(root, xpath, "complexVariation", true);
        validateComplexTypeWadl(root, xpath, "jxPathVariation", true);
        validateComplexTypeWadl(root, xpath, "deepCloningVariation", true);
        validateComplexTypeWadl(root, xpath, "variationsResult", true);

        // Error
        validateComplexTypeWadl(root, xpath, "error", true);

        // Spreadsheet Result Return Type
        final String path = "/*[local-name()='application']/*[local-name()='resources']/*[local-name()='resource' and @path='/']/*[local-name()='resource' and @path='test']/*[local-name()='method']/*[local-name()='response']/*[local-name()='representation']";
        final Node node = (Node) xpath.evaluate(path, root, XPathConstants.NODE);
        assertEqualsIgnorePrefix("spreadsheetResult", node.getAttributes().getNamedItem("element").getTextContent());
    }

    @Test
    public void testWsdlSchemaSimple1() throws XPathExpressionException {
        String wsdlBody = ITestUtils.getWsdlBody(baseURI + "/deployment1/simple1?wsdl");
        assertNotNull(wsdlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wsdlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        validateComplexTypeWsdl(root, xpath, "CodeStep", true);
        validateComplexTypeWsdl(root, xpath, "CalculationStep", true);
        validateComplexTypeWsdl(root, xpath, "CompoundStep", true);
        validateComplexTypeWsdl(root, xpath, "SimpleStep", true);

        // Variations
        validateComplexTypeWsdl(root, xpath, "ArgumentReplacementVariation", true);
        validateComplexTypeWsdl(root, xpath, "NoVariation", true);
        validateComplexTypeWsdl(root, xpath, "ComplexVariation", true);
        validateComplexTypeWsdl(root, xpath, "JXPathVariation", true);
        validateComplexTypeWsdl(root, xpath, "DeepCloningVariation", true);
        validateComplexTypeWsdl(root, xpath, "VariationsResult", true);

        // Error
        validateComplexTypeWsdl(root, xpath, "Error", true);

        // Spreadsheet Result Return Type
        final String path = "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='testResponse']/*[local-name()='sequence']/*[local-name()='element']";
        final Node node = (Node) xpath.evaluate(path, root, XPathConstants.NODE);
        assertEqualsIgnorePrefix("SpreadsheetResult", node.getAttributes().getNamedItem("type").getTextContent());
    }

    @Test
    public void testWadlSchemaSimple2() throws XPathExpressionException {
        String wadlBody = ITestUtils.getWadlBody(baseURI + "/REST/deployment2/simple2?_wadl");
        assertNotNull(wadlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wadlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        validateComplexTypeWadl(root, xpath, "codeStep", false);
        validateComplexTypeWadl(root, xpath, "calculationStep", false);
        validateComplexTypeWadl(root, xpath, "compoundStep", false);
        validateComplexTypeWadl(root, xpath, "simpleStep", false);

        // Variations
        validateComplexTypeWadl(root, xpath, "argumentReplacementVariation", false);
        validateComplexTypeWadl(root, xpath, "noVariation", false);
        validateComplexTypeWadl(root, xpath, "complexVariation", false);
        validateComplexTypeWadl(root, xpath, "jxPathVariation", false);
        validateComplexTypeWadl(root, xpath, "deepCloningVariation", false);
        validateComplexTypeWadl(root, xpath, "variationsResult", false);

        // Error
        validateComplexTypeWadl(root, xpath, "error", true);

        // CSR
        validateComplexTypeWadl(root, xpath, "Calc", true);
        validateComplexTypeWadl(root, xpath, "HiddenCalc", true);
        validateComplexTypeWadl(root, xpath, "SprOneRow", true);

        // Spreadsheet Result Return Type
        final String path = "/*[local-name()='application']/*[local-name()='resources']/*[local-name()='resource' and @path='/']/*[local-name()='resource' and @path='calc']/*[local-name()='method']/*[local-name()='response']/*[local-name()='representation']";
        final Node node = (Node) xpath.evaluate(path, root, XPathConstants.NODE);
        assertEqualsIgnorePrefix("Calc", node.getAttributes().getNamedItem("element").getTextContent());
    }

    @Test
    public void testWsdlSchemaSimple2() throws XPathExpressionException {
        String wsdlBody = ITestUtils.getWsdlBody(baseURI + "/deployment2/simple2?wsdl");
        assertNotNull(wsdlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wsdlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        validateComplexTypeWsdl(root, xpath, "CodeStep", false);
        validateComplexTypeWsdl(root, xpath, "CalculationStep", false);
        validateComplexTypeWsdl(root, xpath, "CompoundStep", false);
        validateComplexTypeWsdl(root, xpath, "SimpleStep", false);

        // Variations
        validateComplexTypeWsdl(root, xpath, "ArgumentReplacementVariation", false);
        validateComplexTypeWsdl(root, xpath, "NoVariation", false);
        validateComplexTypeWsdl(root, xpath, "ComplexVariation", false);
        validateComplexTypeWsdl(root, xpath, "JXPathVariation", false);
        validateComplexTypeWsdl(root, xpath, "DeepCloningVariation", false);
        validateComplexTypeWsdl(root, xpath, "VariationsResult", false);

        // Error
        validateComplexTypeWsdl(root, xpath, "Error", true);

        // CSR
        validateComplexTypeWsdl(root, xpath, "Calc", true);
        validateComplexTypeWsdl(root, xpath, "HiddenCalc", true);
        validateComplexTypeWsdl(root, xpath, "SprOneRow", true);

        // Spreadsheet Result Return Type
        final String path = "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='calcResponse']/*[local-name()='sequence']/*[local-name()='element']";
        final Node node = (Node) xpath.evaluate(path, root, XPathConstants.NODE);
        assertEqualsIgnorePrefix("Calc", node.getAttributes().getNamedItem("type").getTextContent());
    }

    private void validateCSRElementNameWsdl(final Node root,
            final XPath xpath,
            String complexTypeName,
            String elementName) throws XPathExpressionException {
        final String path = "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='" + complexTypeName + "']/*[local-name()='sequence']/*[local-name()='element' and @name='" + elementName + "']";
        final Node node = (Node) xpath.evaluate(path, root, XPathConstants.NODE);
        assertNotNull(node);
    }

    private void validateCSRElementsCountWsdl(final Node root,
            final XPath xpath,
            String complexTypeName,
            int n) throws XPathExpressionException {
        final String path = "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='" + complexTypeName + "']/*[local-name()='sequence']/*[local-name()='element']";
        final NodeList nodes = (NodeList) xpath.evaluate(path, root, XPathConstants.NODESET);
        assertEquals(n, nodes.getLength());
    }

    private void validateCSRElementNameWadl(final Node root,
            final XPath xpath,
            String complexTypeName,
            String elementName) throws XPathExpressionException {
        final String path = "/*[local-name()='application']/*[local-name()='grammars']/*[local-name()='schema']/*[local-name()='complexType' and @name='" + complexTypeName + "']/*[local-name()='sequence']/*[local-name()='element' and @name='" + elementName + "']";
        final Node node = (Node) xpath.evaluate(path, root, XPathConstants.NODE);
        assertNotNull(node);
    }

    private void validateCSRElementsCountWadl(final Node root,
            final XPath xpath,
            String complexTypeName,
            int n) throws XPathExpressionException {
        final String path = "/*[local-name()='application']/*[local-name()='grammars']/*[local-name()='schema']/*[local-name()='complexType' and @name='" + complexTypeName + "']/*[local-name()='sequence']/*[local-name()='element']";
        final NodeList nodes = (NodeList) xpath.evaluate(path, root, XPathConstants.NODESET);
        assertEquals(n, nodes.getLength());
    }

    private void assertEqualsIgnorePrefix(String expected, String actual) {
        if (actual.indexOf(":") > 0) {
            assertEquals(expected, actual.substring(actual.indexOf(":") + 1));
        } else {
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testCSRAsteriskSimple2_Wadl() throws XPathExpressionException {
        String wadlBody = ITestUtils.getWadlBody(baseURI + "/REST/deployment2/simple2?_wadl");
        assertNotNull(wadlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wadlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        // CSR
        validateComplexTypeWadl(root, xpath, "Calc", true);
        validateComplexTypeWadl(root, xpath, "HiddenCalc", true);
        validateComplexTypeWadl(root, xpath, "SprOneRow", true);
        validateComplexTypeWadl(root, xpath, "SprOneColumn", true);
        validateComplexTypeWadl(root, xpath, "SprTwoTwo", true);
        validateComplexTypeWadl(root, xpath, "SprOneOne", true);
        validateComplexTypeWadl(root, xpath, "SprOneOneNoAsterisk", false);
        validateComplexTypeWadl(root, xpath, "SprTwoTwoIgnored1", false);
        validateComplexTypeWadl(root, xpath, "SprTwoTwoIgnored2", false);
        validateComplexTypeWadl(root, xpath, "DtRetSpr", false);
        validateComplexTypeWadl(root, xpath, "DtRetSpr2", false);
        validateComplexTypeWadl(root, xpath, "DtRetSpr3", false);

        validateCSRElementsCountWadl(root, xpath, "SprOneRow", 3);
        validateCSRElementNameWadl(root, xpath, "SprOneRow", "moreValues");
        validateCSRElementNameWadl(root, xpath, "SprOneRow", "Values");
        validateCSRElementNameWadl(root, xpath, "SprOneRow", "Values1");

        validateCSRElementsCountWadl(root, xpath, "SprOneColumn", 3);
        validateCSRElementNameWadl(root, xpath, "SprOneColumn", "moreValues");
        validateCSRElementNameWadl(root, xpath, "SprOneColumn", "Values");
        validateCSRElementNameWadl(root, xpath, "SprOneColumn", "Values1");

        validateCSRElementsCountWadl(root, xpath, "SprTwoTwo", 8);
        validateCSRElementNameWadl(root, xpath, "SprTwoTwo", "Formula2_Step3");
        validateCSRElementNameWadl(root, xpath, "SprTwoTwo", "Formula2_Step4");
        validateCSRElementNameWadl(root, xpath, "SprTwoTwo", "Formula_Step1");
        validateCSRElementNameWadl(root, xpath, "SprTwoTwo", "Formula_Step2");
        validateCSRElementNameWadl(root, xpath, "SprTwoTwo", "Values2_Step3");
        validateCSRElementNameWadl(root, xpath, "SprTwoTwo", "Values2_Step4");
        validateCSRElementNameWadl(root, xpath, "SprTwoTwo", "Values_Step1");
        validateCSRElementNameWadl(root, xpath, "SprTwoTwo", "Values_Step2");

        validateCSRElementsCountWadl(root, xpath, "SprOneOne", 1);
        validateCSRElementNameWadl(root, xpath, "SprOneOne", "Step1");
        // Validate CSR field
        final Node node1 = (Node) xpath.evaluate(
            "/*[local-name()='application']/*[local-name()='grammars']/*[local-name()='schema']/*[local-name()='complexType' and @name='Calc']/*[local-name()='sequence']/*[local-name()='element']",
            root,
            XPathConstants.NODE);
        String v1 = node1.getAttributes().getNamedItem("type").getTextContent();
        assertEqualsIgnorePrefix("SprOneRow", v1);

        // Spreadsheet Result Return Type
        final Node node2 = (Node) xpath.evaluate(
            "/*[local-name()='application']/*[local-name()='resources']/*[local-name()='resource' and @path='/']/*[local-name()='resource' and @path='calc']/*[local-name()='method']/*[local-name()='response']/*[local-name()='representation']",
            root,
            XPathConstants.NODE);
        assertEqualsIgnorePrefix("Calc", node2.getAttributes().getNamedItem("element").getTextContent());

        // Spreadsheet Result Return Type for dtRetSpr
        final Node node3 = (Node) xpath.evaluate(
            "/*[local-name()='application']/*[local-name()='resources']/*[local-name()='resource' and @path='/']/*[local-name()='resource' and @path='dtRetSpr']/*[local-name()='method']/*[local-name()='response']/*[local-name()='representation']",
            root,
            XPathConstants.NODE);
        assertNull(node3.getAttributes().getNamedItem("element"));

        // Spreadsheet Result Return Type for dtRetSpr2
        final Node node4 = (Node) xpath.evaluate(
            "/*[local-name()='application']/*[local-name()='resources']/*[local-name()='resource' and @path='/']/*[local-name()='resource' and @path='dtRetSpr2']/*[local-name()='method']/*[local-name()='response']/*[local-name()='representation']",
            root,
            XPathConstants.NODE);
        assertNull(node4.getAttributes().getNamedItem("element"));

        // Spreadsheet Result Return Type for dtRetSpr3
        final Node node5 = (Node) xpath.evaluate(
            "/*[local-name()='application']/*[local-name()='resources']/*[local-name()='resource' and @path='/']/*[local-name()='resource' and @path='dtRetSpr3']/*[local-name()='method']/*[local-name()='response']/*[local-name()='representation']",
            root,
            XPathConstants.NODE);
        assertNull(node5.getAttributes().getNamedItem("element"));

    }

    @Test
    public void testCSRAsteriskSimple2_Wsdl() throws XPathExpressionException {
        String wadlBody = ITestUtils.getWadlBody(baseURI + "/deployment2/simple2?wsdl");
        assertNotNull(wadlBody);

        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(wadlBody));
        final Node root = (Node) xpath.evaluate("/", inputSource, XPathConstants.NODE);

        // CSR
        validateComplexTypeWsdl(root, xpath, "Calc", true);
        validateComplexTypeWsdl(root, xpath, "HiddenCalc", true);
        validateComplexTypeWsdl(root, xpath, "SprOneRow", true);
        validateComplexTypeWsdl(root, xpath, "SprOneColumn", true);
        validateComplexTypeWsdl(root, xpath, "SprTwoTwo", true);
        validateComplexTypeWsdl(root, xpath, "SprOneOne", true);
        validateComplexTypeWsdl(root, xpath, "SprOneOneNoAsterisk", false);
        validateComplexTypeWsdl(root, xpath, "SprTwoTwoIgnored1", false);
        validateComplexTypeWsdl(root, xpath, "SprTwoTwoIgnored2", false);
        validateComplexTypeWsdl(root, xpath, "DtRetSpr", false);
        validateComplexTypeWsdl(root, xpath, "DtRetSpr2", false);
        validateComplexTypeWsdl(root, xpath, "DtRetSpr3", false);

        validateCSRElementsCountWsdl(root, xpath, "SprOneRow", 3);
        validateCSRElementNameWsdl(root, xpath, "SprOneRow", "moreValues");
        validateCSRElementNameWsdl(root, xpath, "SprOneRow", "Values");
        validateCSRElementNameWsdl(root, xpath, "SprOneRow", "Values1");

        validateCSRElementsCountWsdl(root, xpath, "SprOneColumn", 3);
        validateCSRElementNameWsdl(root, xpath, "SprOneColumn", "moreValues");
        validateCSRElementNameWsdl(root, xpath, "SprOneColumn", "Values");
        validateCSRElementNameWsdl(root, xpath, "SprOneColumn", "Values1");

        validateCSRElementsCountWsdl(root, xpath, "SprTwoTwo", 8);
        validateCSRElementNameWsdl(root, xpath, "SprTwoTwo", "Formula2_Step3");
        validateCSRElementNameWsdl(root, xpath, "SprTwoTwo", "Formula2_Step4");
        validateCSRElementNameWsdl(root, xpath, "SprTwoTwo", "Formula_Step1");
        validateCSRElementNameWsdl(root, xpath, "SprTwoTwo", "Formula_Step2");
        validateCSRElementNameWsdl(root, xpath, "SprTwoTwo", "Values2_Step3");
        validateCSRElementNameWsdl(root, xpath, "SprTwoTwo", "Values2_Step4");
        validateCSRElementNameWsdl(root, xpath, "SprTwoTwo", "Values_Step1");
        validateCSRElementNameWsdl(root, xpath, "SprTwoTwo", "Values_Step2");

        validateCSRElementsCountWsdl(root, xpath, "SprOneOne", 1);
        validateCSRElementNameWsdl(root, xpath, "SprOneOne", "Step1");
        // Validate CSR field
        final Node node1 = (Node) xpath.evaluate(
            "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='Calc']/*[local-name()='sequence']/*[local-name()='element']",
            root,
            XPathConstants.NODE);
        final String v1 = node1.getAttributes().getNamedItem("type").getTextContent();
        assertEqualsIgnorePrefix("SprOneRow", v1);

        // Spreadsheet Result Return Type
        final Node node2 = (Node) xpath.evaluate(
            "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='calcResponse']/*[local-name()='sequence']/*[local-name()='element']",
            root,
            XPathConstants.NODE);
        assertEqualsIgnorePrefix("Calc", node2.getAttributes().getNamedItem("type").getTextContent());

        // Spreadsheet Result Return Type for dtRetSpr
        final Node node3 = (Node) xpath.evaluate(
            "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='dtRetSprResponse']/*[local-name()='sequence']/*[local-name()='element']",
            root,
            XPathConstants.NODE);
        assertEqualsIgnorePrefix("anyType", node3.getAttributes().getNamedItem("type").getTextContent());

        // Spreadsheet Result Return Type for dtRetSpr2
        final Node node4 = (Node) xpath.evaluate(
            "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='dtRetSpr2Response']/*[local-name()='sequence']/*[local-name()='element']",
            root,
            XPathConstants.NODE);
        assertEqualsIgnorePrefix("ArrayOfAnyType", node4.getAttributes().getNamedItem("type").getTextContent());
        
        // Spreadsheet Result Return Type for dtRetSpr3
        final Node node5 = (Node) xpath.evaluate(
            "/*[local-name()='definitions']/*[local-name()='types']/*[local-name()='schema']/*[local-name()='complexType' and @name='dtRetSpr3Response']/*[local-name()='sequence']/*[local-name()='element']",
            root,
            XPathConstants.NODE);
        assertEqualsIgnorePrefix("ArrayOfSprOneOne", node5.getAttributes().getNamedItem("type").getTextContent());
    }

}
