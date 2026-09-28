package org.openl.rules.lang.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.rules.lang.xls.load.UnloadableLazyWorkbookLoader;
import org.openl.source.impl.PathSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;

class XlsWorkbookSourceCodeModuleTest {

    @TempDir
    Path folder;

    @Test
    void testUrlWithWhiteSpaces() throws MalformedURLException {
        var f = new File("test/rules/test xls/Test with spaces.xls");
        var module = new XlsWorkbookSourceCodeModule(
                new URLSourceCodeModule(f.toURI().toURL()));
        assertNotNull(module.getSourceFile());
    }

    @Test
    void testUrlWithWhiteSpaces2() {
        var module = new XlsWorkbookSourceCodeModule(
                new PathSourceCodeModule(Path.of("test/rules/test xls/Test with spaces.xls")));
        assertNotNull(module.getSourceFile());
    }

    @Test
    void testFileIsNotCorrupted() throws IOException {
        File tempFile = File.createTempFile("test", ".tmp");
        tempFile.deleteOnExit();
        try (var writer = new FileWriter(tempFile)) {
            writer.write("TEST");
        }

        var src = new URLSourceCodeModule(URLSourceCodeModule.toUrl(tempFile));
        Workbook workbook = mock(Workbook.class);
        when(workbook.getSpreadsheetVersion()).thenReturn(SpreadsheetVersion.EXCEL2007);
        doThrow(new OutOfMemoryError()).when(workbook).write(any(OutputStream.class));

        var module = new XlsWorkbookSourceCodeModule(src,
                new SimpleWorkbookLoader(workbook));
        assertThrows(OutOfMemoryError.class, module::save);

        assertEquals(4, tempFile.length(), "File should not cleared if there are no actual write operations");
    }

    /**
     * The sheets of one workbook are parsed by one thread while another saves a write into it, and every parsed
     * sheet asks to be told about a save — so the telling stands being added to while it goes round.
     */
    @Test
    void a_sheet_parsed_while_the_workbook_is_saved_is_told_about_the_next_save() throws IOException {
        var workbook = written();
        List<String> told = new ArrayList<>();
        workbook.addListener(saying(told, "parsed first"));
        workbook.addListener(new XlsWorkbookListener() {
            @Override
            public void beforeSave(XlsWorkbookSourceCodeModule saved) {
                saved.addListener(saying(told, "parsed during the save"));
            }

            @Override
            public void afterSave(XlsWorkbookSourceCodeModule saved) {
                // Nothing to say.
            }
        });

        workbook.save();
        workbook.save();

        assertTrue(told.contains("parsed during the save"), "the sheet parsed during a save is told about the next");
        assertEquals(2, told.stream().filter("parsed first"::equals).count());
    }

    /** A workbook of one cell, on a file of its own. */
    private XlsWorkbookSourceCodeModule written() throws IOException {
        var source = new PathSourceCodeModule(TestWorkbooks.writeOneCell(folder));
        return new XlsWorkbookSourceCodeModule(source, new UnloadableLazyWorkbookLoader(source));
    }

    /** A listener that writes down that it was told. */
    private static XlsWorkbookListener saying(List<String> told, String what) {
        return new XlsWorkbookListener() {
            @Override
            public void beforeSave(XlsWorkbookSourceCodeModule saved) {
                told.add(what);
            }

            @Override
            public void afterSave(XlsWorkbookSourceCodeModule saved) {
                // Nothing to say.
            }
        };
    }
}
