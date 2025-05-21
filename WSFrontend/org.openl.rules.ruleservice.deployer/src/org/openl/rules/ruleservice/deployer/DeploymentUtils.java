package org.openl.rules.ruleservice.deployer;

import java.io.InputStream;
import jakarta.xml.xpath.XPath;
import jakarta.xml.xpath.XPathExpression;
import jakarta.xml.xpath.XPathExpressionException;
import jakarta.xml.xpath.XPathFactory;

import org.xml.sax.InputSource;

final class DeploymentUtils {

    private DeploymentUtils() {
    }

    static final String API_VERSION_SEPARATOR = "_V";

    static String getProjectName(InputStream stream) {
        return evaluateXPath(stream, "/project/name");
    }

    static String getApiVersion(InputStream stream) {
        return evaluateXPath(stream, "/rules-deploy/version");
    }

    static String evaluateXPath(InputStream stream, String expression) {
        try {
            InputSource inputSource = new InputSource(stream);
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression xPathExpression = xPath.compile(expression);
            return xPathExpression.evaluate(inputSource);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

}
