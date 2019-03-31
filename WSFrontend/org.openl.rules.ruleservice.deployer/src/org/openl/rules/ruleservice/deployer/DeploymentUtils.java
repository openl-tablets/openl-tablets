package org.openl.rules.ruleservice.deployer;

import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.InputStream;

class DeploymentUtils {
    
    private DeploymentUtils() {
    }

    public static final String API_VERSION_SEPARATOR = "_V";

    public static String getProjectName(InputStream stream) {
        return evaluateXPath(stream, "/project/name");
    }

    public static String getApiVersion(InputStream stream) {
        return evaluateXPath(stream, "/version");
    }

    public static String evaluateXPath(InputStream stream, String expression) {
        try {
            InputSource inputSource = new InputSource(stream);
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression xPathExpression = xPath.compile(expression);
            return xPathExpression.evaluate(inputSource);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    public static String getFileName(String filePath) {
        String[] pathTokens = filePath.split(File.pathSeparator);
        return pathTokens[pathTokens.length - 1];
    }
}
