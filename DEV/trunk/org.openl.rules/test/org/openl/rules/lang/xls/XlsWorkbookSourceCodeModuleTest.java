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
            XlsWorkbookSourceCodeModule module = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(new File("test/rules/test xls/Test with spaces.xls").toURL()));
            assertNotNull(module.sourceFile);
        } catch (MalformedURLException e) {
            assertFalse(true);
        }
    }
}
