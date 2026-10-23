package org.openl.rules.lang.xls;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.source.IOpenSourceCodeModule;
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
        assertFalse(module.isSavedSinceRead(), "a save that failed leaves the file as it was read");
    }

    /** What was compiled from a workbook answers for its file until a write is saved into it. */
    @Test
    void a_workbook_is_saved_since_it_was_read_once_a_save_writes_it() throws IOException {
        var workbook = written();
        assertFalse(workbook.isSavedSinceRead(), "a workbook just read stands as its file does");

        workbook.save();

        assertTrue(workbook.isSavedSinceRead());
    }

    /** A save of the look alone, such as the formatting of the tables, leaves what was compiled answering. */
    @Test
    void a_save_of_the_look_leaves_the_workbook_as_it_was_read() throws IOException {
        var workbook = written();

        workbook.saveLook();

        assertFalse(workbook.isSavedSinceRead());
        workbook.save();
        workbook.saveLook();
        assertTrue(workbook.isSavedSinceRead(), "a write saved before is not forgotten");
    }

    /**
     * The sheets of one workbook are parsed by one thread while another saves a write into it, and every parsed
     * sheet asks to be told about a save — so the telling stands being added to while it goes round.
     */
    @Test
    void a_sheet_parsed_while_the_workbook_is_saved_is_told_about_the_next_save() throws IOException {
        var workbook = written();
        var told = new ArrayList<String>();
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

    /** A file another program wrote since the workbook was read is not overwritten. */
    @Test
    void a_save_refuses_a_file_changed_since_it_was_read() throws IOException {
        var workbook = written();
        var file = workbook.getSourceFile().toPath();
        Files.write(file, new byte[]{1, 2, 3});

        var refused = assertThrows(WorkbookChangedOutsideException.class, workbook::save);

        assertEquals(file.toFile(), refused.getFile());
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(file), "the change of the other program stays");
        assertFalse(workbook.isSavedSinceRead());
    }

    /** The file is told by the content the workbook was read from: a change keeping its size and time is told too. */
    @Test
    void a_save_refuses_a_change_that_keeps_the_size_and_the_time_of_the_file() throws IOException {
        var workbook = written();
        var file = workbook.getSourceFile().toPath();
        var content = Files.readAllBytes(file);
        var time = Files.getLastModifiedTime(file);
        content[content.length - 1] ^= 1;
        Files.write(file, content);
        Files.setLastModifiedTime(file, time);

        assertThrows(WorkbookChangedOutsideException.class, workbook::save);
        assertArrayEquals(content, Files.readAllBytes(file));
    }

    /**
     * A workbook unloaded and read again from a file another program changed keeping its size and time is not saved:
     * the tables compiled before stand for the content read first.
     */
    @Test
    void a_workbook_read_again_after_a_change_keeping_the_size_and_the_time_is_not_saved() throws IOException {
        var workbook = written();
        var file = workbook.getSourceFile().toPath();
        var content = Files.readAllBytes(file);
        var time = Files.getLastModifiedTime(file);
        content[content.length - 1] ^= 1;
        Files.write(file, content);
        Files.setLastModifiedTime(file, time);

        var first = new WeakReference<>(workbook.getWorkbook());
        workbook.getWorkbookLoader().setCanUnload(true);
        for (var attempt = 0; attempt < 50 && first.get() != null; attempt++) {
            System.gc();
        }
        assumeTrue(first.get() == null, "the workbook is unloaded");
        assertNotNull(workbook.getWorkbook());

        assertThrows(WorkbookChangedOutsideException.class, workbook::save);
        assertArrayEquals(content, Files.readAllBytes(file), "the change of the other program stays");
    }

    /**
     * A workbook read from a file another program wrote after the compilation is not saved: the tables compiled
     * before stand for another file than the one in memory.
     */
    @Test
    void a_workbook_read_after_a_change_of_its_file_is_not_saved() throws IOException {
        var source = new PathSourceCodeModule(TestWorkbooks.writeOneCell(folder));
        var workbook = new XlsWorkbookSourceCodeModule(source);
        var other = TestWorkbooks.writeOneCell(Files.createDirectory(folder.resolve("other")));
        Files.copy(other, workbook.getSourceFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.setLastModifiedTime(workbook.getSourceFile().toPath(), FileTime.fromMillis(1_000_000));

        assertNotNull(workbook.getWorkbook());

        assertThrows(WorkbookChangedOutsideException.class, workbook::save);
    }

    /** A change made while the workbook is written is not overwritten: the file is compared right before it is replaced. */
    @Test
    void a_change_made_while_the_workbook_is_written_is_not_overwritten() throws IOException {
        var workbook = written();
        var file = workbook.getSourceFile().toPath();
        workbook.addListener(new XlsWorkbookListener() {
            @Override
            public void beforeSave(XlsWorkbookSourceCodeModule saved) {
                try {
                    Files.write(file, new byte[]{4, 5, 6});
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public void afterSave(XlsWorkbookSourceCodeModule saved) {
                // Nothing to say.
            }
        });

        assertThrows(WorkbookChangedOutsideException.class, workbook::save);

        assertArrayEquals(new byte[]{4, 5, 6}, Files.readAllBytes(file), "the change of the other program stays");
        assertEquals(List.of(file), filesIn(folder), "nothing of the save is left beside the file");
    }

    /** The workbook replaces its file whole and leaves nothing beside it. */
    @Test
    void a_save_replaces_the_file_whole() throws IOException {
        var workbook = written();
        var file = workbook.getSourceFile().toPath();
        workbook.getWorkbook().getSheetAt(0).getRow(0).getCell(0).setCellValue("saved");

        workbook.save();

        assertEquals(List.of(file), filesIn(folder));
        try (var read = WorkbookFactory.create(file.toFile())) {
            assertEquals("saved", read.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        }
    }

    /** What the workbook saved itself is what the next save expects to find. */
    @Test
    void a_workbook_saves_over_its_own_save() throws IOException {
        var workbook = written();
        workbook.save();

        workbook.save();

        assertTrue(workbook.isSavedSinceRead());
    }

    /** A workbook read again after its own save is read from what it saved. */
    @Test
    void a_workbook_read_again_after_its_own_save_saves_over_it() throws IOException {
        var source = new PathSourceCodeModule(TestWorkbooks.writeOneCell(folder));
        var workbook = new XlsWorkbookSourceCodeModule(source);
        assertNotNull(workbook.getWorkbook());
        workbook.save();
        workbook.getWorkbookLoader().setCanUnload(true);

        assertNotNull(workbook.getWorkbook());
        workbook.save();

        assertTrue(workbook.isSavedSinceRead());
    }

    /** A file deleted since the workbook was read is written again. */
    @Test
    void a_save_writes_a_file_deleted_since_it_was_read() throws IOException {
        var workbook = written();
        var file = workbook.getSourceFile().toPath();
        Files.delete(file);

        workbook.save();

        assertTrue(Files.size(file) > 0);
    }

    /** A workbook read from something other than a file, such as a branch of a merge, is read as it is. */
    @Test
    void a_workbook_without_a_file_is_read_as_it_is() throws IOException {
        var content = Files.readAllBytes(TestWorkbooks.writeOneCell(folder));
        var source = mock(IOpenSourceCodeModule.class);
        when(source.getFileUri()).thenReturn("rules/Bank Rating.xlsx");
        when(source.getUri()).thenReturn("rules/Bank Rating.xlsx");
        when(source.getByteStream()).thenAnswer(call -> new ByteArrayInputStream(content));

        var workbook = new XlsWorkbookSourceCodeModule(source);

        assertEquals(TestWorkbooks.CELL, workbook.getWorkbook().getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
    }

    /** A workbook of one cell, on a file of its own, read through one snapshot of it as OpenL reads it. */
    private XlsWorkbookSourceCodeModule written() throws IOException {
        var source = new PathSourceCodeModule(TestWorkbooks.writeOneCell(folder));
        var workbook = new XlsWorkbookSourceCodeModule(source);
        assertNotNull(workbook.getWorkbook());
        return workbook;
    }

    private static List<Path> filesIn(Path folder) throws IOException {
        try (var files = Files.list(folder)) {
            return files.filter(Files::isRegularFile).sorted().toList();
        }
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
