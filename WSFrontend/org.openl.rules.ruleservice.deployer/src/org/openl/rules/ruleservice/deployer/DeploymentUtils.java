package org.openl.rules.ruleservice.deployer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openl.util.IOUtils;
import org.xml.sax.InputSource;

class DeploymentUtils {

    private DeploymentUtils() {
    }

    static final String API_VERSION_SEPARATOR = "_V";

    static String getProjectName(InputStream stream) {
        return evaluateXPath(stream, "/project/name");
    }

    static String getApiVersion(InputStream stream) {
        return evaluateXPath(stream, "/version");
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

    static Map<String, byte[]> unzip(InputStream in) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zipStream = new ZipInputStream(in)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                String name = zipEntry.getName();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                IOUtils.copyAndClose(new ZippedFileInputStream(zipStream), outputStream);
                entries.put(name, outputStream.toByteArray());
            }
        }
        return entries;
    }

    static ByteArrayOutputStream archiveAsZip(Map<String, byte[]> zipEntries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> entry : zipEntries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipos.putNextEntry(zipEntry);
                IOUtils.copy(new ByteArrayInputStream(entry.getValue()), zipos);
                zipos.closeEntry();
            }
        }
        return baos;
    }

}
