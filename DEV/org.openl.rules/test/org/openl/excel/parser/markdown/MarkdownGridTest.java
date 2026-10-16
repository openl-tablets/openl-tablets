package org.openl.excel.parser.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.TestUtils;
import org.openl.rules.lang.xls.WorkbookChangedOutsideException;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.CellStyle;

/**
 * A table of a Markdown file is written through the grid the table editor writes into, and saved into the file.
 */
class MarkdownGridTest {

    private static final String GREETING = """
            ---
            table: SmartRules String greeting(Integer hour)
            description: Greeting by the hour
            ---

            Picks a greeting.

            | Hour  | Greeting       |
            |-------|----------------|
            | 0-11  | Good Morning   |
            | 12-17 | Good Afternoon |
            | 18-23 | Good Evening   |
            """;

    private Path file;
    private TableSyntaxNode table;
    private IGridTable grid;

    @BeforeEach
    void compile(@TempDir Path dir) throws Exception {
        file = dir.resolve("Greetings/greeting.md");
        Files.createDirectories(file.getParent());
        Files.writeString(file, GREETING, StandardCharsets.UTF_8);
        table = tableOf(file.getParent());
        grid = GridTableUtils.getOriginalTable(table.getGridTable());
    }

    @Test
    void editIsSavedIntoTheFileAndCompiledFromIt() throws Exception {
        grid.edit();
        var writable = assertInstanceOf(MarkdownGrid.class, grid.getGrid());
        // Sheet rows: the header, the description, the first row of the body, then the hours
        writable.setCellValue(1, 3, "Morning");

        writable.save();

        assertEquals(GREETING.replace("| 0-11  | Good Morning   |", "| 0-11  | Morning        |"),
                Files.readString(file, StandardCharsets.UTF_8));
        assertTrue(table.getXlsSheetSourceCodeModule().getWorkbookSource().isSavedSinceRead());
        // The save ends the write: the sheet reads as it was parsed until it is compiled again
        assertFalse(grid.getGrid() instanceof IWritableGrid);
        var rules = new RulesEngineFactory<>(file.getParent().toString()).newEngineInstance();
        assertEquals("Morning", TestUtils.invoke(rules, "greeting", 9));
    }

    @Test
    void mergeIsSavedIntoTheFrontMatter() throws Exception {
        grid.edit();
        var writable = (IWritableGrid) grid.getGrid();

        // As in Excel, the first value of the range stays and the others are cleared
        writable.addMergedRegion(new GridRegion(4, 0, 5, 0));
        writable.save();

        var text = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(GREETING.replace("description: Greeting by the hour\n",
                        "description: Greeting by the hour\nmerges: [A3:A4]\n")
                .replace("| 18-23 | Good Evening   |", "|       | Good Evening   |"), text);
    }

    @Test
    void cellsReadAsTheyAreWritten() {
        grid.edit();
        var writable = (IWritableGrid) grid.getGrid();
        try {
            writable.addMergedRegion(new GridRegion(4, 0, 5, 0));
            writable.copyCell(1, 3, 2, 3);
            writable.setCellValue(1, 4, "= 1 + 1");
            // The file keeps no formatting: a style is not written, and does not fail the write
            writable.setCellStyle(1, 3, new CellStyle(null));
            writable.setCellFontBold(1, 3, true);
            writable.setCellComment(1, 3, "note", null);

            var covered = writable.getCell(0, 5);
            assertEquals("12-17", covered.getStringValue());
            assertEquals(new GridRegion(4, 0, 5, 0), covered.getAbsoluteRegion());
            // The range is as high as its top left cell, as in a workbook: a covered cell is a cell of its own size
            assertEquals(2, writable.getCell(0, 4).getHeight());
            assertEquals(1, covered.getHeight());
            assertEquals("Good Morning", writable.getCell(2, 3).getStringValue());
            assertEquals("= 1 + 1", writable.getCell(1, 4).getStringValue());
            assertNull(writable.getCell(1, 3).getStyle());
            assertNull(writable.findEmptyRect(2, 2));
            assertTrue(writable.isEmpty(0, 5));

            writable.removeMergedRegion(0, 4);

            assertNull(writable.getCell(0, 5).getRegion());
        } finally {
            grid.stopEditing();
        }
    }

    @Test
    void movedCellsKeepTheirText() {
        grid.edit();
        var writable = (IWritableGrid) grid.getGrid();
        try {
            writable.copyCellValue(1, 3, 2, 3);
            writable.copyCellValue(3, 3, 1, 4);
            writable.addMergedRegion(new GridRegion(3, 0, 4, 0));
            // As a shift of the cells does: the value moves first, then the merge holding it
            writable.copyCellValue(0, 3, 0, 4);
            writable.moveMergedRegion(new GridRegion(3, 0, 4, 0), new GridRegion(4, 0, 5, 0));

            assertEquals("Good Morning", writable.getCell(2, 3).getStringValue());
            assertNull(writable.getCell(1, 4).getStringValue(), "a cell beyond the table empties the target");
            assertEquals("0-11", writable.getCell(0, 4).getStringValue());
            assertTrue(writable.isEmpty(0, 5), "the moved merge clears the cells it covers");
            assertEquals(new GridRegion(4, 0, 5, 0), writable.getCell(0, 5).getAbsoluteRegion());
            assertNull(writable.getCell(0, 3).getRegion(), "the merge left its place");
        } finally {
            grid.stopEditing();
        }
    }

    @Test
    void fileAnotherProgramChangedIsNotSavedOver() throws Exception {
        var changed = GREETING.replace("Good Morning", "Morning, written in another program");
        Files.writeString(file, changed, StandardCharsets.UTF_8);
        grid.edit();
        var writable = (IWritableGrid) grid.getGrid();
        try {
            writable.setCellValue(1, 4, "Afternoon");

            assertThrows(WorkbookChangedOutsideException.class, writable::save);
            assertEquals(changed, Files.readString(file, StandardCharsets.UTF_8));
        } finally {
            grid.stopEditing();
        }
    }

    @Test
    void excelFormulaIsRefused() {
        grid.edit();
        var writable = (IWritableGrid) grid.getGrid();
        try {
            // A text starting with = is an OpenL formula, so an Excel formula has no text to be written as
            var error = assertThrows(MarkdownWriteException.class, () -> writable.setCellFormula(1, 4, "B2*C2"));

            assertEquals("""
                    The Excel formula '=B2*C2' cannot be written in a Markdown table, which holds values and \
                    OpenL formulas""", error.getMessage());
            assertEquals("Good Afternoon", writable.getCell(1, 4).getStringValue());
        } finally {
            grid.stopEditing();
        }
    }

    @Test
    void layoutWithoutMarkdownFormIsNotSaved() throws Exception {
        grid.edit();
        var writable = (IWritableGrid) grid.getGrid();
        try {
            writable.setCellValue(5, 0, "beside the header");

            assertThrows(MarkdownWriteException.class, writable::save);
            assertEquals(GREETING, Files.readString(file, StandardCharsets.UTF_8));
        } finally {
            grid.stopEditing();
        }
    }

    @Test
    void newFileIsWrittenFromTheTableWrittenOnItsSheet() throws Exception {
        var saved = new AtomicReference<String>();
        var sheet = MarkdownGrid.create("Proj/rules/Rating/Factors/bonus.md", saved::set);

        // A new table is laid down where the sheet finds room: the top left corner of the sheet of its file
        assertEquals(new GridRegion(0, 0, 2, 1), sheet.findEmptyRect(2, 3));
        sheet.setCellValue(0, 0, "SmartRules Double bonus(Integer years)");
        sheet.addMergedRegion(new GridRegion(0, 0, 0, 1));
        sheet.setCellValue(0, 1, "Years");
        sheet.setCellValue(1, 1, "Bonus");
        sheet.setCellValue(0, 2, 5);
        sheet.setCellValue(1, 2, 0.1);
        sheet.save();

        assertEquals("Proj/rules/Rating/Factors/bonus.md?sheet=Factors", sheet.getUri());
        assertEquals("""
                ---
                table: SmartRules Double bonus(Integer years)
                ---

                | Years | Bonus |
                |---|---|
                | 5 | 0.1 |
                """, saved.get());
    }

    @Test
    void newFileIsAddressedAsTheFilesOfItsModuleAre() {
        var sibling = table.getXlsSheetSourceCodeModule().getWorkbookSource();
        var folder = file.getParent();

        var uri = MarkdownGrid.uriOf(sibling, folder, folder.resolve("Sub Folder/bonus.md"));

        assertEquals(sibling.getUri().replace("greeting.md", "Sub%20Folder/bonus.md"), uri);
    }

    @Test
    void valuesAreWrittenAsTextTheTableReadsBack() {
        var date = Date.from(LocalDate.of(2024, Month.JANUARY, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());

        assertNull(MarkdownGrid.textOf(null));
        assertEquals("3", MarkdownGrid.textOf(3.0));
        assertEquals("0.05", MarkdownGrid.textOf(0.05));
        assertEquals("100000000000000000000", MarkdownGrid.textOf(1e20));
        assertEquals("1.5", MarkdownGrid.textOf(1.5f));
        assertEquals("12.3", MarkdownGrid.textOf(new BigDecimal("12.300")));
        assertEquals("42", MarkdownGrid.textOf(42));
        assertEquals("true", MarkdownGrid.textOf(true));
        assertEquals("2024-01-31", MarkdownGrid.textOf(date));
        // A time is written with no zone, which every date type reads in the zone of the reader
        assertEquals("2024-01-31T18:30:00", MarkdownGrid.textOf(Date.from(LocalDate.of(2024, Month.JANUARY, 31).atTime(18, 30)
                .atZone(ZoneId.systemDefault()).toInstant())));
        assertEquals("HOURS", MarkdownGrid.textOf(java.util.concurrent.TimeUnit.HOURS));
        assertEquals("a\\,b,c", MarkdownGrid.textOf(new String[]{"a,b", "c"}));
        assertEquals("NaN", MarkdownGrid.textOf(Double.NaN));
    }

    private static TableSyntaxNode tableOf(Path module) {
        var compiled = new RulesEngineFactory<>(module.toString()).getCompiledOpenClass();
        return Arrays.stream(((XlsMetaInfo) compiled.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
                        .getXlsTableSyntaxNodes())
                .filter(node -> node.getHeader().getSourceString().startsWith("SmartRules"))
                .findFirst()
                .orElseThrow();
    }
}
