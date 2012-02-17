package org.openl.rules.lang.xls;

import java.io.File;
import java.net.MalformedURLException;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.source.impl.URLSourceCodeModule;

public class XlsWorkbookSourceCodeModuleTest{

    @Test
    public void testUrlWithWhiteSpaces() {
        try {
            File f = new File("test/rules/test xls/Test with spaces.xls");
            XlsWorkbookSourceCodeModule module = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(f.toURI().toURL()));
            assertNotNull(module.getSourceFile());
        } catch (MalformedURLException e) {
            assertFalse(true);
        }
    }
}
