package org.openl.rules.lang.xls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Paths;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.PathSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;

public class XlsWorkbookSourceCodeModuleTest {

    @Test
    public void testUrlWithWhiteSpaces() throws MalformedURLException {
        File f = new File("test/rules/test xls/Test with spaces.xls");
        XlsWorkbookSourceCodeModule module = new XlsWorkbookSourceCodeModule(
            new URLSourceCodeModule(f.toURI().toURL()));
        assertNotNull(module.getSourceFile());
    }

    @Test
    public void testUrlWithWhiteSpaces2() {
        XlsWorkbookSourceCodeModule module = new XlsWorkbookSourceCodeModule(
                new PathSourceCodeModule(Paths.get("test/rules/test xls/Test with spaces.xls")));
        assertNotNull(module.getSourceFile());
    }

    @Test
    public void testFileIsNotCorrupted() throws IOException {
        File tempFile = File.createTempFile("test", ".tmp");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("TEST");
        }

        IOpenSourceCodeModule src = new URLSourceCodeModule(URLSourceCodeModule.toUrl(tempFile));
        Workbook workbook = mock(Workbook.class);
        when(workbook.getSpreadsheetVersion()).thenReturn(SpreadsheetVersion.EXCEL2007);
        doThrow(new OutOfMemoryError()).when(workbook).write(any(OutputStream.class));

        try {
            XlsWorkbookSourceCodeModule module = new XlsWorkbookSourceCodeModule(src,
                new SimpleWorkbookLoader(workbook));
            module.save();
        } catch (OutOfMemoryError ignored) {
        }

        assertEquals("File should not cleared if there are no actual write operations", 4, tempFile.length());
    }
}
