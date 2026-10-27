package org.openl.rules.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellFormulaType;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.service.TableServiceImpl;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * Rows inserted into a table or deleted from it move the cells below as Excel stored them, so a saved workbook differs
 * only in the rows the edit changed.
 */
class MovedCellsTest {

    /** The values of row 3 as Excel stores them: column, type, value. */
    private static final List<String[]> STORED = List.of(new String[]{"0", "n", "24"},
            new String[]{"1", "n", "1.1000000000000001"},
            new String[]{"2", "b", "1"},
            new String[]{"3", "n", "45000"});

    private static final int WIDTH = 8;

    @ParameterizedTest
    @CsvSource({"true, 1", "false, 3"})
    void movesTheCellsBelowAsStored(boolean remove, int movedTo) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet();
            var date = workbook.createCellStyle();
            date.setDataFormat(workbook.createDataFormat().getFormat("dd.mm.yyyy"));
            for (var row = 0; row < 4; row++) {
                var sheetRow = sheet.createRow(row);
                for (var cell : STORED) {
                    var ct = sheetRow.createCell(Integer.parseInt(cell[0])).getCTCell();
                    if ("b".equals(cell[1])) {
                        ct.setT(STCellType.B);
                    }
                    ct.setV(cell[2]);
                }
                sheetRow.getCell(3).setCellStyle(date);
                sheetRow.createCell(4).setCellValue(" padded ");
                var rich = new XSSFRichTextString("bold plain");
                var bold = workbook.createFont();
                bold.setBold(true);
                rich.applyFont(0, 4, bold);
                sheetRow.createCell(5).setCellValue(rich);
                sheetRow.createCell(6).setCellValue("=not a formula");
                var formula = sheetRow.createCell(7).getCTCell();
                formula.setT(STCellType.B);
                formula.addNewF().setStringValue("TRUE()");
                formula.setV("1");
            }
            // The cell under one row holds a value, the cell under the next one holds nothing.
            sheet.getRow(1).createCell(WIDTH).setCellValue("x");
            sheet.getRow(3).createCell(WIDTH).setCellValue("x");
            var stored = stored(sheet.getRow(2).getCell(7));
            var styles = workbook.getNumCellStyles();
            var grid = grid(workbook);
            var table = new GridRegion(0, 0, 3, WIDTH);

            var action = remove
                    ? GridTool.removeRows(1, 0, table, grid, mock(MetaInfoWriter.class))
                    : GridTool.insertRows(1, 0, table, grid, mock(MetaInfoWriter.class));
            action.doAction(new GridTable(table, grid));

            var moved = sheet.getRow(movedTo);
            for (var cell : STORED) {
                var ct = moved.getCell(Integer.parseInt(cell[0])).getCTCell();
                assertEquals(cell[1], ct.isSetT() ? ct.getT().toString() : "n", "type of column " + cell[0]);
                assertEquals(cell[2], ct.getV(), "value of column " + cell[0]);
            }
            assertEquals("dd.mm.yyyy", moved.getCell(3).getCellStyle().getDataFormatString());
            assertEquals(" padded ", moved.getCell(4).getStringCellValue());
            var rich = moved.getCell(5).getRichStringCellValue();
            assertEquals(2, rich.numFormattingRuns(), "the text keeps the fonts of its parts");
            assertTrue(rich.getFontAtIndex(0).getBold());
            assertEquals(STCellType.S, moved.getCell(6).getCTCell().getT(), "a text that looks like a formula");
            assertEquals(stored, stored(moved.getCell(7)), "a formula keeps its last result");
            assertEquals(styles, workbook.getNumCellStyles(), "no style is added");
            if (remove) {
                assertTrue(moved.getCell(WIDTH).getStringCellValue().isEmpty(), "the cell is emptied");
            }
        }
    }

    @Test
    void movesAMergeBelowAsStored() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet();
            var bottom = workbook.createCellStyle();
            for (var row = 0; row < 4; row++) {
                sheet.createRow(row).createCell(0).setCellValue("r" + row);
            }
            sheet.getRow(2).getCell(0).getCTCell().setT(STCellType.N);
            sheet.getRow(2).getCell(0).getCTCell().setV("24");
            sheet.getRow(3).getCell(0).setBlank();
            sheet.getRow(3).getCell(0).setCellStyle(bottom);
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 0, 0));
            var styles = workbook.getNumCellStyles();
            var grid = grid(workbook);
            var table = new GridRegion(0, 0, 3, 0);

            removeFirstRows(grid, table, 1);

            assertEquals(List.of(new CellRangeAddress(1, 2, 0, 0)), sheet.getMergedRegions());
            assertEquals("24", sheet.getRow(1).getCell(0).getCTCell().getV());
            assertEquals(bottom.getIndex(), sheet.getRow(2).getCell(0).getCellStyle().getIndex());
            assertEquals(styles, workbook.getNumCellStyles(), "no style is added");
        }
    }

    /** A table moved where it has room to grow keeps what its cells store, and its merged title only in one cell. */
    @Test
    void movesATableAsStored() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet();
            var title = sheet.createRow(1);
            title.createCell(1).setCellValue("Data Item first");
            title.createCell(2);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 2));
            var fields = sheet.createRow(2);
            fields.createCell(1).setCellValue(" id");
            fields.createCell(2).setCellValue("Name ");
            var values = sheet.createRow(3);
            values.createCell(1).getCTCell().setV("1");
            values.createCell(2).setCellValue("one");
            var styles = workbook.getNumCellStyles();
            var grid = grid(workbook);

            var moved = new TableServiceImpl().moveTable(new GridTable(new GridRegion(1, 1, 3, 2), grid));

            var top = moved.getTop();
            var left = moved.getLeft();
            assertEquals("Data Item first", sheet.getRow(top).getCell(left).getStringCellValue());
            assertFalse(sheet.getRow(top).getCell(left + 1).getCTCell().isSetV(), "the merge holds its title once");
            assertTrue(sheet.getMergedRegions().contains(new CellRangeAddress(top, top, left, left + 1)));
            assertEquals(" id", sheet.getRow(top + 1).getCell(left).getStringCellValue());
            assertEquals("Name ", sheet.getRow(top + 1).getCell(left + 1).getStringCellValue());
            assertEquals("1", sheet.getRow(top + 2).getCell(left).getCTCell().getV());
            assertEquals(styles, workbook.getNumCellStyles(), "no style is added");
            assertNull(sheet.getRow(1).getCell(1), "the table leaves its place");
        }
    }

    /** A part of a table written elsewhere keeps the value of a merge reaching into it from outside the part. */
    @Test
    void writesTheValueOfAMergeReachingIntoThePart() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet();
            sheet.createRow(0).createCell(0).setCellValue("group");
            sheet.createRow(1).createCell(0);
            sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 0));
            var grid = grid(workbook);
            var builder = new TableBuilder(grid, null);

            builder.beginTable(1, 1);
            var written = builder.getTableRegion();
            builder.writeGridTable(new GridTable(new GridRegion(1, 0, 1, 0), grid));
            builder.endTable();

            assertEquals("group", sheet.getRow(written.getTop()).getCell(written.getLeft()).getStringCellValue());
        }
    }

    /** The rows a table no longer covers keep no cell of a merge that moved up off them. */
    @Test
    void clearsTheRowsLeftBelowAMovedMerge() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet();
            var group = workbook.createCellStyle();
            for (var row = 0; row < 5; row++) {
                var cell = sheet.createRow(row).createCell(0);
                cell.setCellStyle(group);
                if (row < 3) {
                    cell.setCellValue("r" + row);
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(2, 4, 0, 0));
            var grid = grid(workbook);
            var table = new GridRegion(0, 0, 4, 0);

            removeFirstRows(grid, table, 1);

            assertEquals(List.of(new CellRangeAddress(1, 3, 0, 0)), sheet.getMergedRegions());
            assertNull(sheet.getRow(4).getCell(0), "the cell below the table is gone, its style with it");
        }
    }

    /**
     * The merge moves over the rows that moved before it and takes no value they left behind: its top-left cell
     * keeps what it held, nothing or only spaces.
     */
    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void keepsWhatAMovedMergeHolds(String topLeft) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet();
            for (var row = 0; row < 6; row++) {
                sheet.createRow(row).createCell(0).setCellValue(row < 4 ? "r" + row : topLeft);
            }
            if (topLeft.isEmpty()) {
                sheet.getRow(4).getCell(0).setBlank();
            }
            sheet.getRow(5).getCell(0).setBlank();
            sheet.addMergedRegion(new CellRangeAddress(4, 5, 0, 0));
            var grid = grid(workbook);

            removeFirstRows(grid, new GridRegion(0, 0, 5, 0), 2);

            assertEquals(List.of(new CellRangeAddress(2, 3, 0, 0)), sheet.getMergedRegions());
            assertEquals("r2", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("r3", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(topLeft, sheet.getRow(2).getCell(0).getStringCellValue());
            assertFalse(sheet.getRow(3).getCell(0).getCTCell().isSetV(), "the merge holds no other value");
        }
    }

    /** An error value of an {@code .xls} workbook moves as the error it is. */
    @Test
    void movesAnErrorOfAnXlsWorkbook() throws IOException {
        try (var workbook = new HSSFWorkbook()) {
            var sheet = workbook.createSheet();
            sheet.createRow(0).createCell(0).setCellValue("r0");
            sheet.createRow(1).createCell(0).setCellErrorValue(FormulaError.NA.getCode());
            var grid = grid(workbook);
            var table = new GridRegion(0, 0, 1, 0);

            removeFirstRows(grid, table, 1);

            assertEquals(CellType.ERROR, sheet.getRow(0).getCell(0).getCellType());
            assertEquals(FormulaError.NA.getCode(), sheet.getRow(0).getCell(0).getErrorCellValue());
        }
    }

    /** A date merged into a top-left cell of no date format is given one, so it is still read as a date. */
    @Test
    void keepsADateMergedIntoAPlainCell() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var date = workbook.createCellStyle();
            date.setDataFormat(workbook.createDataFormat().getFormat("dd.mm.yyyy"));
            var row = workbook.createSheet().createRow(0);
            row.createCell(0);
            var dated = row.createCell(1);
            dated.setCellStyle(date);
            dated.getCTCell().setV("45000");
            var grid = grid(workbook);

            grid.addMergedRegion(new GridRegion(0, 0, 0, 1));

            assertTrue(DateUtil.isCellDateFormatted(row.getCell(0)));
            assertEquals(DateUtil.getJavaDate(45000), grid.getCell(0, 0).getObjectValue());
        }
    }

    /** A formula shared with other cells is read relative to them, so it moves as the text it reads as. */
    @Test
    void movesASharedFormulaAsItsText() throws IOException {
        var file = new ByteArrayOutputStream();
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet();
            for (var row = 0; row < 3; row++) {
                sheet.createRow(row).createCell(1).setCellValue(row);
            }
            var master = sheet.getRow(1).createCell(0).getCTCell();
            var f = master.addNewF();
            f.setT(STCellFormulaType.SHARED);
            f.setRef("A2:A3");
            f.setSi(0);
            f.setStringValue("B2+1");
            sheet.getRow(2).createCell(0).getCTCell().addNewF().setT(STCellFormulaType.SHARED);
            sheet.getRow(2).getCell(0).getCTCell().getF().setSi(0);
            workbook.write(file);
        }
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(file.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            var grid = grid(workbook);
            var table = new GridRegion(0, 0, 2, 1);

            removeFirstRows(grid, table, 1);

            var moved = sheet.getRow(1).getCell(0);
            assertEquals("B3+1", moved.getCellFormula());
            assertEquals(STCellFormulaType.NORMAL, moved.getCTCell().getF().getT());
        }
    }

    /** Takes the given number of rows away from the top of the table. */
    private static void removeFirstRows(XlsSheetGridModel grid, GridRegion table, int count) {
        GridTool.removeRows(count, 0, table, grid, mock(MetaInfoWriter.class)).doAction(new GridTable(table, grid));
    }

    private static XlsSheetGridModel grid(Workbook workbook) {
        var source = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule("./test/rules/XlsSheetGridModelTest.xls"),
                new SimpleWorkbookLoader(workbook));
        return new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, source));
    }

    /** What a cell holds as it is stored: type, formula and value. */
    private static String stored(XSSFCell cell) {
        var ct = cell.getCTCell();
        return ct.getT() + " " + (ct.isSetF() ? ct.getF().getStringValue() : "") + " " + ct.getV();
    }
}
