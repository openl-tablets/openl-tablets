package org.openl.rules.webstudio.web.repository.upload.zip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeNoException;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

public class ZipCharsetDetectorTest {
    private static final String TEST_RULES_XML = "test-resources/upload/zip/test-rules-xml.zip";
    private static final String TEST_RULES_XML_UTF_8 = "test-resources/upload/zip/test-rules-xml-utf-8.zip";
    private static final String TEST_WORKSPACE = "test-resources/upload/zip/test-workspace.zip";

    @Test
    public void detectCharsetUsingRulesXml() throws Exception {
        assumeCharsetSupported("IBM866");
        MockEnvironment mockEnvironment = new MockEnvironment();
        String charsetNames = "windows-1252, windows-1251, IBM866";
        mockEnvironment.setProperty("zip.charsets.support", charsetNames);
        ZipCharsetDetector detector = new ZipCharsetDetector(null, mockEnvironment);
        Charset charset = detector.detectCharset(new ZipFromFile(new File(TEST_RULES_XML)));

        assertEquals(Charset.forName("IBM866"), charset);
    }

    @Test
    public void detectCharsetUsingWorkspace() throws Exception {
        assumeCharsetSupported("IBM866");

        // Check the case when some files can be renamed/deleted/added and some can stay with same name.
        // In testing zip the file main.xls is absent but added core.xls. Still can detect charset despite that
        // all file names aren't equal.
        MockEnvironment mockEnvironment = new MockEnvironment();
        String charsetNames = "IBM437, windows-1251, IBM866";
        mockEnvironment.setProperty("zip.charsets.support", charsetNames);
        ZipCharsetDetector detector = new ZipCharsetDetector(null, mockEnvironment);

        Collection<String> filesInWorkspace = Arrays.asList("datatypes.xls", "main.xls", "Основное.xls");
        Charset charset = detector.detectCharset(new ZipFromFile(new File(TEST_WORKSPACE)), filesInWorkspace);

        assertEquals(Charset.forName("IBM866"), charset);
    }

    @Test
    public void noNeedToDetectCharset() throws Exception {
        // This file can be unzipped using UTF-8
        MockEnvironment mockEnvironment = new MockEnvironment();
        String charsetNames = "windows-1252, IBM866";
        mockEnvironment.setProperty("zip.charsets.support", charsetNames);
        ZipCharsetDetector detector = new ZipCharsetDetector(null, mockEnvironment);
        Charset charset = detector.detectCharset(new ZipFromFile(new File(TEST_RULES_XML_UTF_8)));
        assertEquals(StandardCharsets.UTF_8, charset);
    }

    @Test
    public void emptyCharsetList() throws Exception {
        // Forget to set charset list.
        MockEnvironment mockEnvironment = new MockEnvironment();
        ZipCharsetDetector detector = new ZipCharsetDetector(null, mockEnvironment);

        // Still can detect files with UTF-8 encoding
        assertEquals(StandardCharsets.UTF_8, detector.detectCharset(new ZipFromFile(new File(TEST_RULES_XML_UTF_8))));
        // Return null if cannot detect charset
        assertNull(detector.detectCharset(new ZipFromFile(new File(TEST_RULES_XML))));
    }

    @Test
    public void skipNotExistingCharset() throws Exception {
        assumeCharsetSupported("IBM437");

        // If some charset does not exist in JVM, don't fail, just skip it.
        MockEnvironment mockEnvironment = new MockEnvironment();
        String charsetNames = "not-exist-in-current-jvm, IBM437";
        mockEnvironment.setProperty("zip.charsets.support", charsetNames);
        ZipCharsetDetector detector = new ZipCharsetDetector(null, mockEnvironment);
        Charset charset = detector.detectCharset(new ZipFromFile(new File(TEST_RULES_XML)));

        assertEquals(Charset.forName("IBM437"), charset);
    }

    private void assumeCharsetSupported(String charsetName) {
        try {
            Charset.forName(charsetName);
        } catch (UnsupportedCharsetException e) {
            assumeNoException("Necessary for test encoding is absent. Skip it.", e);
        }
    }
}