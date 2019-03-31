package org.openl.rules.lang.xls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.util.FileUtils;

public class XlsWorkbookSourceCodeModuleTest {

    private static final String TEST_FILE_NAME = "testCorruptedFile.xls";
    private static final String TEXT = "test";

    @Before
    public void init() throws Exception {
        OutputStream os = new FileOutputStream(TEST_FILE_NAME);
        os.write(TEXT.getBytes());
        os.close();
    }

    @Test
    public void testUrlWithWhiteSpaces() throws MalformedURLException {
        File f = new File("test/rules/test xls/Test with spaces.xls");
        XlsWorkbookSourceCodeModule module = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(f.toURI().toURL()));
        assertNotNull(module.getSourceFile());
    }

    @Test
    public void testFileIsNotCorrupted() throws IOException {
        IOpenSourceCodeModule src = mock(IOpenSourceCodeModule.class);
        Workbook workbook = mock(Workbook.class);
        when(workbook.getSpreadsheetVersion()).thenReturn(SpreadsheetVersion.EXCEL2007);
        doThrow(new OutOfMemoryError()).when(workbook).write(any(OutputStream.class));

        try {
            XlsWorkbookSourceCodeModule module = new XlsWorkbookSourceCodeModule(src, new SimpleWorkbookLoader(workbook));
            module.saveAs(TEST_FILE_NAME);
        } catch (OutOfMemoryError ignored) {
        }

        assertEquals("File should not cleared if there are no actual write operations",
            TEXT.getBytes().length,
            new File(TEST_FILE_NAME).length());
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(new File(TEST_FILE_NAME));
    }
}
