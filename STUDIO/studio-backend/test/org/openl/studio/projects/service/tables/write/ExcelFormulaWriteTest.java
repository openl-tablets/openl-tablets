package org.openl.studio.projects.service.tables.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.AppendTarget;
import org.openl.studio.projects.model.tables.RawCellInput;
import org.openl.studio.projects.model.tables.RawTableCell;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.model.tables.UpdateTarget;
import org.openl.studio.projects.service.tables.TableTestProjects;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * A text starting with {@code =} is written as the OpenL formula it is, after the apostrophe of Excel. An Excel formula
 * is written only from the {@code formula} of a cell, and the value of such a cell is ignored.
 */
class ExcelFormulaWriteTest {

    @TempDir
    Path tempDir;

    private Path project;

    @BeforeEach
    void setUp() throws IOException {
        project = TableTestProjects.writeProject(tempDir.resolve("main"), "main", "main", new String[][]{
                {"Spreadsheet SpreadsheetResult Calc()", null},
                {"Step", "Value"},
                {"F1", "one"},
                {"F2", "=1+2"}
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"= product($F1:$F2)", "= $F1 * 2", "=SUM(1,2)"})
    void writesAValueStartingWithAnEqualsSignAsText(String formula) throws IOException {
        apply(new UpdateTarget.Cell(2, 1, formula));

        assertEquals(formula, cellOf(2, 1).value());
        assertNull(cellOf(2, 1).formula());
        assertEquals(CellType.STRING, cellType(3, 2));
        assertTrue(quoted(3, 2), "Excel keeps the text a text when the cell is edited in Excel");
    }

    /** Excel would turn an Excel formula after an apostrophe into a text once the cell is edited in Excel. */
    @Test
    void takesTheApostropheFromAnExcelFormulaWrittenOverAnOpenLFormula() throws IOException {
        apply(new UpdateTarget.Cell(2, 1, "= 1 + 2"));
        apply(new UpdateTarget.Cell(2, 1, null, null, "=2*3"));

        assertEquals("=2*3", cellOf(2, 1).formula());
        assertEquals(CellType.FORMULA, cellType(3, 2));
        assertFalse(quoted(3, 2));
    }

    @Test
    void writesTheFormulaOfACell() {
        apply(new UpdateTarget.Cell(2, 1, null, null, "=2*3"));

        assertEquals("=2*3", cellOf(2, 1).formula());
        assertEquals(6, cellOf(2, 1).value());
    }

    @Test
    void writesAFormulaWithSpacesAroundIt() {
        apply(new UpdateTarget.Cell(2, 1, null, null, "  =2*3 "));

        assertEquals("=2*3", cellOf(2, 1).formula());
    }

    /** A formula keeps the value it computes from a cell written after it, in the same edit or in another one. */
    @Test
    void computesAFormulaFromTheCellsAsTheyAreSaved() {
        // F2 stands in C5 of the sheet.
        new RawTableWriter(load()).apply(List.of(
                new RawTableSourceAction.Update(new UpdateTarget.Cell(2, 1, null, null, "=C5*2")),
                new RawTableSourceAction.Update(new UpdateTarget.Cell(3, 1, 10))));

        assertEquals(20, cellOf(2, 1).value());

        apply(new UpdateTarget.Cell(3, 1, 7));

        assertEquals(14, cellOf(2, 1).value());
    }

    @Test
    void refusesAnEditNamingBothAValueAndAFormula() {
        var bothInCell = new UpdateTarget.Cell(2, 1, 6, null, "=2*3");
        var bothInRow = new UpdateTarget.Row(2,
                List.of(new RawCellInput("F1", null, null, null), new RawCellInput(6, null, null, null, "=2*3")));

        var cell = assertThrows(BadRequestException.class, () -> apply(bothInCell));
        var row = assertThrows(BadRequestException.class, () -> apply(bothInRow));

        assertEquals("openl.error.400.table.action.cell.value-and-formula.message", cell.getErrorCode());
        assertEquals("openl.error.400.table.action.cell.value-and-formula.message", row.getErrorCode());
        assertEquals("one", cellOf(2, 1).value(), "nothing is written");
    }

    @Test
    void writesAValueOverAnExcelFormula() {
        apply(new UpdateTarget.Cell(3, 1, "=1+2"));

        assertEquals("=1+2", cellOf(3, 1).value());
        assertNull(cellOf(3, 1).formula(), "the same text is a value now");
    }

    @Test
    void writesAValueOverAnExcelFormulaEvenWhenItEqualsWhatTheFormulaComputed() {
        apply(new UpdateTarget.Cell(3, 1, 3));

        assertEquals(3, cellOf(3, 1).value());
        assertNull(cellOf(3, 1).formula(), "the value replaces the formula");
    }

    @Test
    void writesTheExcelFormulasOfARow() {
        apply(new UpdateTarget.Row(2, List.of(new RawCellInput("F1", null, null, null),
                new RawCellInput(null, null, null, null, "=4*5"))));

        assertEquals("=4*5", cellOf(2, 1).formula());
        assertEquals(20, cellOf(2, 1).value());
    }

    @Test
    void appendsARowHoldingOnlyAnExcelFormula() {
        apply(new AppendTarget.Rows(List.of(List.of(new RawCellInput(null, null, null, null),
                new RawCellInput(null, null, null, null, "=7*2")))));

        assertEquals("=7*2", cellOf(4, 1).formula());
    }

    @Test
    void keepsTheExcelFormulasOfATableWrittenBackAsItWasRead() {
        var read = new RawTableReader().read(load());
        var rows = read.source.stream().map(List::copyOf).toList();
        var changed = new ArrayList<>(rows);
        changed.set(2, List.of(rows.get(2).getFirst(), RawTableCell.builder().value("= 3").build()));

        new RawTableWriter(load()).write(RawTableView.builder()
                .kind(read.kind)
                .name(read.name)
                .source(changed)
                .build());

        assertEquals("= 3", cellOf(2, 1).value());
        assertEquals("=1+2", cellOf(3, 1).formula(), "the formula read with the table is written back");
        assertEquals(3, cellOf(3, 1).value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"=SUM(1,", "=INFO(\"osversion\")", "1+2", "=", "= "})
    void refusesAnExcelFormulaExcelCannotRead(String formula) {
        var target = new UpdateTarget.Cell(2, 1, null, null, formula);

        var refused = assertThrows(BadRequestException.class, () -> apply(target));

        assertEquals("openl.error.400.table.cell.formula.invalid.message", refused.getErrorCode());
        assertEquals("one", cellOf(2, 1).value(), "nothing is written");
    }

    private void apply(UpdateTarget target) {
        new RawTableWriter(load()).apply(new RawTableSourceAction.Update(target));
    }

    private void apply(AppendTarget target) {
        new RawTableWriter(load()).apply(new RawTableSourceAction.Append(target));
    }

    private RawTableCell cellOf(int row, int column) {
        return new RawTableReader().read(load()).source.get(row).get(column);
    }

    /** The type of a cell as the workbook stores it, by its row and column on the sheet. */
    private CellType cellType(int row, int column) throws IOException {
        return stored(row, column, Cell::getCellType);
    }

    /** Whether a cell follows the apostrophe of Excel, by its row and column on the sheet. */
    private boolean quoted(int row, int column) throws IOException {
        return stored(row, column, cell -> cell.getCellStyle().getQuotePrefixed());
    }

    private <T> T stored(int row, int column, Function<Cell, T> what) throws IOException {
        try (var in = Files.newInputStream(project.resolve("main.xlsx")); var workbook = new XSSFWorkbook(in)) {
            return what.apply(workbook.getSheetAt(0).getRow(row).getCell(column));
        }
    }

    private IOpenLTable load() {
        return TableTestProjects.projectModel(project).getAllTableSyntaxNodes().stream()
                .map(TableSyntaxNodeAdapter::new)
                .filter(table -> table.getGridTable(IXlsTableNames.VIEW_DEVELOPER) != null)
                .findFirst()
                .orElseThrow();
    }
}
