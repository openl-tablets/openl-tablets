package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.TableCellEditorView;
import org.openl.studio.projects.model.tables.TableEditorsView;

/**
 * Verifies how a table says its cells are written.
 *
 * <p>The fixture is a decision table whose condition holds whole numbers and whose return holds text, so the
 * numbers are entered within the bounds of their type and the text as any other text.
 */
class TableEditorsReaderTest {

    private ProjectModel projectModel;

    @BeforeEach
    void writeModule(@TempDir Path projectDir) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Rules");
            row(sheet, 1, "Rules String Greeting(Integer hour)");
            row(sheet, 2, "C1", "RET1");
            row(sheet, 3, "hour < max", "value");
            row(sheet, 4, "Integer max", "String value");
            row(sheet, 5, "Max", "Greeting");
            row(sheet, 6, "12", "Good Morning");
            row(sheet, 7, "24", "Good Evening");
            // The header spans both columns, so the matrix holds a cell the merge reaches over.
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 2));

            try (OutputStream out = Files.newOutputStream(projectDir.resolve("Rules.xlsx"))) {
                workbook.write(out);
            }
        }
        var module = ProjectResolver.getInstance().resolve(projectDir).getModules().getFirst();
        // Compile this module alone: the whole-project compile runs in the background and would replace what the
        // model holds part-way through the test.
        module.getWebstudioConfiguration().setCompileThisModuleOnly(true);
        projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(module);
    }

    @Test
    void saysHowACellHoldingANumberIsWritten() {
        var read = new TableEditorsReader().read(table("Greeting"), null, null);

        var numeric = editorOf(read, "numeric");
        assertNotNull(numeric, "a cell holding an Integer is entered as a number");
        assertEquals(Integer.MIN_VALUE, numeric.min());
        assertEquals(Integer.MAX_VALUE, numeric.max());
        assertEquals(Boolean.TRUE, numeric.intOnly());
    }

    @Test
    void keepsAnEditorOnceForEveryCellThatAsksForIt() {
        var read = new TableEditorsReader().read(table("Greeting"), null, null);

        var numeric = read.editors().indexOf(editorOf(read, "numeric"));
        var pointing = read.cells().stream().filter(cell -> cell.editor() == numeric).toList();
        // Both values of the condition column are entered the same way, and the way is described once.
        assertEquals(2, pointing.size());
        assertEquals(1, read.editors().stream().filter(editor -> "numeric".equals(editor.editor())).count());
    }

    @Test
    void saysNothingAboutACellWrittenAsPlainText() {
        var read = new TableEditorsReader().read(table("Greeting"), null, null);

        var numeric = read.editors().indexOf(editorOf(read, "numeric"));
        // The return column holds text, which a screen writes as text without being told, so only the two cells
        // of the condition column are reported at all.
        assertEquals(2, read.cells().size(), "only the cells asking for an editor of their own are reported");
        assertTrue(read.cells().stream().allMatch(cell -> cell.editor() == numeric));
    }

    @Test
    void reportsOnlyTheRowsItIsAskedAbout() {
        var window = new TableEditorsReader().read(table("Greeting"), 6, 1);

        assertFalse(window.cells().isEmpty(), "the window holds the first of the two data rows");
        assertTrue(window.cells().stream().allMatch(cell -> cell.row() == 0),
                "a cell is pointed at by its place in the window, as the raw read reports it");
    }

    @Test
    void readsATableWhoseCellsAMergeReachesOver() {
        var read = new TableEditorsReader().read(table("Greeting"), null, null);

        // The covered cell of the merged header stands for the cell it belongs to and describes nothing itself;
        // reading it as a cell of its own would fail before any editor is found.
        assertFalse(read.cells().isEmpty(), "the table's cells are read past the merged header");
    }

    /** The first editor of the given kind, or {@code null} when the table asks for none. */
    private static TableCellEditorView editorOf(TableEditorsView read, String kind) {
        return read.editors().stream()
                .filter(editor -> kind.equals(editor.editor()))
                .findFirst()
                .orElse(null);
    }

    /** The table of the module with the given name. */
    private IOpenLTable table(String name) {
        for (TableSyntaxNode tsn : projectModel.getAllTableSyntaxNodes()) {
            var table = new TableSyntaxNodeAdapter(tsn);
            if (name.equals(table.getName())) {
                return table;
            }
        }
        throw new IllegalStateException("No table named " + name);
    }

    private static void row(Sheet sheet, int rowIndex, String... values) {
        var row = sheet.createRow(rowIndex);
        for (int column = 0; column < values.length; column++) {
            row.createCell(column + 1).setCellValue(values[column]);
        }
    }
}
