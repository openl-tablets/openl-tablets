package org.openl.rules.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellFormulaType;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.service.TableServiceException;
import org.openl.rules.service.TableServiceImpl;
import org.openl.rules.table.actions.IUndoableGridTableAction;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * Rows and columns put into a table or taken out of it, and a table moved, leave every Excel formula of the workbook
 * computing from the cells it named, as Excel does.
 */
class MovedFormulaReferencesTest {

    /** The table at B2:D5: titles, a base row, and the total of the base factor times three. */
    private static final IGridRegion TABLE = new GridRegion(1, 1, 4, 3);

    @Test
    void columnInsertedMovesTheReferencesToTheCellsRightOfIt() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = table(workbook);
            formula(sheet, "F10", "D4+C4");
            formula(sheet, "H8", "D8");
            formula(sheet, "H9", "SUM(B4:D4)");
            var other = workbook.createSheet("Other");
            formula(other, "A1", "Rules!D4*2");
            formula(other, "A2", "D4");
            var factor = workbook.createName();
            factor.setNameName("Factor");
            factor.setRefersToFormula("Rules!$D$4");
            formula(other, "A3", "Factor*3");

            var grid = grid(workbook);
            run(grid, GridTool.insertColumns(1, 1, TABLE, grid, mock(MetaInfoWriter.class)));

            assertFormula(sheet, "D5", "E4*3", 6);
            assertFormula(sheet, "F10", "E4+D4", 12);
            assertFormula(sheet, "H8", "D8", 0);
            assertFormula(sheet, "H9", "SUM(B4:E4)", 12);
            assertFormula(other, "A1", "Rules!E4*2", 4);
            assertFormula(other, "A2", "D4", 0);
            assertEquals("Rules!$E$4", factor.getRefersToFormula(), "a defined name follows the cell it names");
            assertFormula(other, "A3", "Factor*3", 6);
        }
    }

    @Test
    void rowInsertedMovesTheReferencesToTheRowsUnderIt() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = table(workbook);
            formula(sheet, "F10", "SUM(C3:C5)");
            formula(sheet, "F11", "C2");

            var grid = grid(workbook);
            run(grid, GridTool.insertRowsOneAfterAnother(1, 1, TABLE, grid, mock(MetaInfoWriter.class)));

            assertFormula(sheet, "C6", "D5*3", 6);
            assertFormula(sheet, "F10", "SUM(C3:C6)", 16);
            assertFormula(sheet, "F11", "C2", 0);
        }
    }

    @Test
    void rowInsertedOverAMergeMovesTheReferencesOnce() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = table(workbook);
            sheet.addMergedRegion(CellRangeAddress.valueOf("B4:B5"));
            formula(sheet, "F10", "D4");

            var grid = grid(workbook);
            run(grid, GridTool.insertRowsOneAfterAnother(2, 1, TABLE, grid, mock(MetaInfoWriter.class)));

            assertFormula(sheet, "F10", "D6", 2);
        }
    }

    @Test
    void columnRemovedLosesTheReferencesToIt() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = table(workbook);
            formula(sheet, "F10", "SUM(B4:D4)");
            formula(sheet, "F11", "D4*2");
            formula(sheet, "F12", "C4");
            formula(sheet, "F13", "SUM(A4:Z4)");
            formula(sheet, "F14", "F11+1");

            var grid = grid(workbook);
            run(grid, GridTool.removeColumns(1, 1, TABLE, grid, mock(MetaInfoWriter.class)));

            assertFormula(sheet, "F10", "SUM(B4:C4)", 2);
            assertFormula(sheet, "F11", "C4*2", 4);
            assertEquals("#REF!", cell(sheet, "F12").getCellFormula());
            // A formula that keeps its text computes again from the cells as they are now
            assertFormula(sheet, "F13", "SUM(A4:Z4)", 2);
            assertFormula(sheet, "F14", "F11+1", 5);
        }
    }

    @Test
    void rowRemovedMovesTheReferencesToTheRowsUnderIt() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = table(workbook);
            formula(sheet, "F10", "C5");
            formula(sheet, "F11", "SUM(C3:C4)");

            var grid = grid(workbook);
            run(grid, GridTool.removeRows(1, 1, TABLE, grid, mock(MetaInfoWriter.class)));

            assertFormula(sheet, "F10", "C4", 6);
            assertFormula(sheet, "F11", "SUM(C3:C3)", 10);
        }
    }

    @Test
    void propertyRowMovesTheReferencesToTheRowsUnderIt() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = table(workbook);
            formula(sheet, "F10", "D4");

            var grid = grid(workbook);
            run(grid, GridTool.insertProp(TABLE, grid, "description", "Total", mock(MetaInfoWriter.class)));

            assertFormula(sheet, "C6", "D5*3", 6);
            assertFormula(sheet, "F10", "D5", 2);
        }
    }

    @Test
    void tableMovedTakesTheReferencesInItAlong() throws IOException, TableServiceException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = table(workbook);
            formula(sheet, "J1", "D4");
            formula(sheet, "J2", "SUM(A4:D4)");

            var grid = grid(workbook);
            new TableServiceImpl(mock(MetaInfoWriter.class)).moveTableTo(new GridTable(TABLE, grid),
                    new GridRegion(11, 5, 14, 7));

            assertFormula(sheet, "G15", "H14*3", 6);
            assertFormula(sheet, "J1", "H14", 2);
            assertEquals("SUM(A4:D4)", cell(sheet, "J2").getCellFormula(), "a range reaching out of the table");
        }
    }

    @Test
    void sharedFormulaLeavesItsGroupWhenItsReferencesMove() throws IOException {
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(sharedFormulas()))) {
            var sheet = workbook.getSheet("Rules");
            var third = workbook.getSheet("Third");

            var grid = grid(workbook);
            run(grid, GridTool.insertColumns(1, 1, TABLE, grid, mock(MetaInfoWriter.class)));

            assertFormula(sheet, "H4", "E4", 2);
            assertFormula(sheet, "H5", "E5", 0);
            assertEquals("D4*2", cell(sheet, "J4").getCellFormula(), "an array formula is left as it is");
            assertEquals("SUM(Rules:Other!D4)", cell(third, "A1").getCellFormula(), "a range of sheets");
            assertEquals(STCellFormulaType.SHARED, ((XSSFCell) cell(sheet, "H7")).getCTCell().getF().getT(),
                    "a formula that cannot be read is left as it is");
        }
    }

    /**
     * A workbook whose H4:H5 share the formula {@code =D4}, read as Excel saved it, with an array formula, a formula
     * over a range of sheets and a cell of a shared formula that is missing.
     */
    private static byte[] sharedFormulas() throws IOException {
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = table(workbook);
            var master = ((XSSFCell) cell(sheet, "H4")).getCTCell().addNewF();
            master.setT(STCellFormulaType.SHARED);
            master.setRef("H4:H5");
            master.setSi(0);
            master.setStringValue("D4");
            shared(sheet, "H5", 0);
            shared(sheet, "H7", 7);
            sheet.setArrayFormula("D4*2", CellRangeAddress.valueOf("J4"));
            var third = workbook.createSheet("Third");
            workbook.createSheet("Other");
            cell(third, "A1").setCellFormula("SUM(Rules:Other!D4)");
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void shared(Sheet sheet, String address, int group) {
        var f = ((XSSFCell) cell(sheet, address)).getCTCell().addNewF();
        f.setT(STCellFormulaType.SHARED);
        f.setSi(group);
    }

    @Test
    void xlsReferencesToTheSheetMove() throws IOException {
        try (var workbook = new HSSFWorkbook()) {
            table(workbook);
            var other = workbook.createSheet("Other");
            formula(other, "A1", "Rules!D4*2");
            formula(other, "A2", "Rules!C4");

            var grid = grid(workbook);
            run(grid, GridTool.removeColumns(1, 1, TABLE, grid, mock(MetaInfoWriter.class)));

            assertFormula(other, "A1", "Rules!C4*2", 4);
            assertEquals("#REF!", cell(other, "A2").getCellFormula());
        }
    }

    /** The sheet of the table at B2:D5, its total {@code =D4*3} computed. */
    private static Sheet table(Workbook workbook) {
        var sheet = workbook.createSheet("Rules");
        cell(sheet, "B2").setCellValue("Spreadsheet SpreadsheetResult Calc");
        cell(sheet, "B3").setCellValue("Step");
        cell(sheet, "C3").setCellValue("Value");
        cell(sheet, "D3").setCellValue("Factor");
        cell(sheet, "B4").setCellValue("Base");
        cell(sheet, "C4").setCellValue(10);
        cell(sheet, "D4").setCellValue(2);
        cell(sheet, "B5").setCellValue("Total");
        formula(sheet, "C5", "D4*3");
        return sheet;
    }

    private static void formula(Sheet sheet, String address, String formula) {
        var cell = cell(sheet, address);
        cell.setCellFormula(formula);
        sheet.getWorkbook().getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(cell);
    }

    private static void assertFormula(Sheet sheet, String address, String formula, double value) {
        var cell = cell(sheet, address);
        assertEquals(formula, cell.getCellFormula(), address);
        assertEquals(value, cell.getNumericCellValue(), address);
    }

    private static Cell cell(Sheet sheet, String address) {
        var reference = new CellReference(address);
        var row = sheet.getRow(reference.getRow());
        if (row == null) {
            row = sheet.createRow(reference.getRow());
        }
        var cell = row.getCell(reference.getCol());
        return cell == null ? row.createCell(reference.getCol()) : cell;
    }

    private static void run(XlsSheetGridModel grid, @Nullable IUndoableGridTableAction action) {
        Objects.requireNonNull(action).doAction(new GridTable(TABLE, grid));
    }

    private static XlsSheetGridModel grid(Workbook workbook) {
        var source = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule("./test/rules/XlsSheetGridModelTest.xls"),
                new SimpleWorkbookLoader(workbook));
        return new XlsSheetGridModel(new XlsSheetSourceCodeModule(workbook.getSheetIndex("Rules"), source));
    }
}
