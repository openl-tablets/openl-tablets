package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.openl.studio.projects.service.tables.TableTestProjects.row;
import static org.openl.studio.projects.service.tables.TableTestProjects.styleNameAt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.service.tables.format.ExcelTableFormatter;

/**
 * Covers the Excel files an upload brings into a project, formatted with the OpenL table style once they are compiled.
 *
 * @author Yury Molchan
 */
class UploadFormattingTest {

    private static final String SHEET = "Rules";
    private static final String WORKBOOK = SHEET + ".xlsx";

    @TempDir
    Path dir;

    @Test
    void formatsTheTablesOfAnUploadedWorkbookInItsStage() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, UploadFormattingTest::person);

        var formatted = uploads(true).formatStaged(dir, List.of(WORKBOOK, "notes.txt"));

        assertEquals(Set.of(WORKBOOK), formatted);
        assertEquals("Title", styleAt(1, 1));
        assertEquals("Calculation", styleAt(2, 1), "The type of a field");
        assertEquals("Heading 4", styleAt(2, 2), "The name of a field");
    }

    @Test
    void leavesTheStageAsItIsWhileFormattingOnSaveIsOff() throws IOException {
        TableTestProjects.projectModel(dir, SHEET, UploadFormattingTest::person);
        var uploads = uploads(false);

        assertFalse(uploads.formats(List.of(WORKBOOK)));
        assertEquals(Set.of(), uploads.formatStaged(dir, List.of(WORKBOOK)));
        assertNull(styleAt(1, 1));
    }

    @Test
    void formatsOnlyTheXlsxFilesOfAnUpload() {
        var uploads = uploads(true);

        assertTrue(uploads.formats(List.of("notes.txt", "rules/Rules.XLSX")));
        assertFalse(uploads.formats(List.of("notes.txt", "Old.xls", "rules.xml")));
    }

    @Test
    void writesAWorkbookThatCannotBeCompiledAsItWasUploaded() throws IOException {
        var broken = "Not a workbook".getBytes(StandardCharsets.UTF_8);
        Files.write(dir.resolve(WORKBOOK), broken);

        assertEquals(Set.of(), uploads(true).formatStaged(dir, List.of(WORKBOOK)));
        assertArrayEquals(broken, Files.readAllBytes(dir.resolve(WORKBOOK)));
    }

    @Test
    void passesAnUploadWithNoWorkbookOnAsItIs() {
        var upload = Map.of("notes.txt", "Hello".getBytes(StandardCharsets.UTF_8));

        assertSame(upload, uploads(true).formatStaged(null, "", upload, false));
    }

    @Test
    void formatsTheUploadedWorkbookOfAnOpenedProjectAndKeepsItsCompile() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, UploadFormattingTest::person);
        TableTestProjects.table(model, "Person");

        uploads(true).formatOpened(model.getAllTableSyntaxNodes(), dir, List.of(WORKBOOK));

        assertEquals("Title", styleAt(1, 1));
        assertFalse(model.isWrittenSinceCompiled(), "Only the look of the workbook is written");
    }

    @Test
    void leavesTheWorkbooksOfAnOpenedProjectTheUploadDidNotBring() throws IOException {
        var model = TableTestProjects.projectModel(dir, SHEET, UploadFormattingTest::person);
        TableTestProjects.table(model, "Person");

        uploads(true).formatOpened(model.getAllTableSyntaxNodes(), dir, List.of("Other.xlsx"));
        uploads(false).formatOpened(model.getAllTableSyntaxNodes(), dir, List.of(WORKBOOK));

        assertNull(styleAt(1, 1));
    }

    @Test
    void leavesATableGatheredFromPartialTablesAsItIs() throws IOException {
        var compiled = TableTestProjects.projectModel(dir, SHEET, UploadFormattingTest::person);
        var person = TableTestProjects.table(compiled, "Person").getSyntaxNode();
        // A Datatype gathered from partial tables of the uploaded workbook stands on a grid made of them.
        var part = mock(TableSyntaxNode.class);
        when(part.getXlsSheetSourceCodeModule()).thenReturn(person.getXlsSheetSourceCodeModule());
        var partGrid = mock(IGridTable.class);
        var parts = mock(CompositeGrid.class);
        when(partGrid.getGrid()).thenReturn(parts);
        when(part.getGridTable()).thenReturn(partGrid);
        uploads(true).formatOpened(List.of(part, person), dir, List.of(WORKBOOK));

        verify(partGrid, never()).edit();
        assertEquals("Title", styleAt(1, 1), "The other tables are formatted");
    }

    @Test
    void tellsWhetherAnUploadWroteTheWorkbookOfAModuleAlone() throws IOException {
        var module = TableTestProjects.projectModel(dir, SHEET, UploadFormattingTest::person).getModuleInfo();
        var uploads = uploads(true);

        assertTrue(uploads.compiledIn(module, dir, List.of(WORKBOOK, "notes.txt")));
        assertFalse(uploads.compiledIn(module, dir, List.of(WORKBOOK, "Other.xlsx")), "Another workbook");
        assertFalse(uploads.compiledIn(null, dir, List.of(WORKBOOK)), "No module is open");
    }

    @Test
    void writesAnUploadedPathLeavingTheProjectAsItWasUploaded() {
        var current = mock(AProjectFolder.class);
        when(current.getArtefacts()).thenReturn(List.of());
        var upload = Map.of("../escape.xlsx", "Not a workbook".getBytes(StandardCharsets.UTF_8));

        assertSame(upload, uploads(true).formatStaged(current, "", upload, false));
        // The stage is a folder of the temporary folder, so a path leaving it would land there.
        assertFalse(Files.exists(Path.of(System.getProperty("java.io.tmpdir"), "escape.xlsx")));
    }

    @Test
    void leavesATableOnNoGridAsItIs() {
        assertFalse(UploadFormatting.format(new ExcelTableFormatter(), mock(IOpenLTable.class)));
    }

    /** The uploads of a session that formats tables on save, or does not. */
    static UploadFormatting uploads(boolean onSave) {
        var formatting = mock(TableFormatting.class);
        when(formatting.onSave()).thenReturn(onSave);
        var studio = mock(WebStudio.class);
        return new UploadFormatting(formatting) {
            @Override
            public WebStudio getWebStudio() {
                return studio;
            }
        };
    }

    private String styleAt(int row, int column) throws IOException {
        return styleNameAt(dir.resolve(WORKBOOK), SHEET, row, column);
    }

    /** A Datatype at B2 of two fields. */
    private static void person(Sheet sheet) {
        row(sheet, 1, 1, "Datatype Person");
        row(sheet, 2, 1, "String", "name");
        row(sheet, 3, 1, "Integer", "age");
    }
}
