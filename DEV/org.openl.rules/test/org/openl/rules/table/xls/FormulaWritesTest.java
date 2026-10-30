package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * A text starting with {@code =} is an OpenL formula and is written as a text. An Excel formula is written only when it
 * is asked for as one.
 */
class FormulaWritesTest {

    @ParameterizedTest
    @ValueSource(strings = {"= product($F1:$F4)", "= $F1 * 2", "=SUM(1,2)", "= $Value$F1"})
    void writesAnOpenLFormulaAsTheTextItIs(String formula) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = workbook.createSheet().createRow(0).createCell(0);

            grid(workbook).setCellValue(0, 0, formula);

            assertEquals(CellType.STRING, cell.getCellType());
            assertEquals(formula, cell.getStringCellValue());
        }
    }

    @Test
    void writesAnExcelFormulaWithTheValueItComputes() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = workbook.createSheet().createRow(0).createCell(0);

            grid(workbook).setCellFormula(0, 0, "=1+2");

            assertEquals(CellType.FORMULA, cell.getCellType());
            assertEquals("1+2", cell.getCellFormula());
            assertEquals(3, cell.getNumericCellValue());
        }
    }

    /** A formula OpenL would read as empty, as it cannot be computed, is refused as one Excel cannot read is. */
    @ParameterizedTest
    @ValueSource(strings = {"=SUM(1,", "=INFO(\"osversion\")"})
    void refusesAnExcelFormulaThatCannotBeReadOrComputed(String formula) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = workbook.createSheet().createRow(0).createCell(0);
            var grid = grid(workbook);

            assertThrows(IllegalArgumentException.class, () -> grid.setCellFormula(0, 0, formula));
            assertEquals(CellType.BLANK, cell.getCellType());
        }
    }

    /** A formula is computed again from the cells as they are now; one that cannot be computed keeps its value. */
    @Test
    void computesTheFormulasOfARegionAgain() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var row = workbook.createSheet().createRow(0);
            row.createCell(0).setCellValue(2);
            PoiExcelHelper.setFormula(row.createCell(1), "=A1*10");
            var unknown = row.createCell(2);
            unknown.setCellFormula("INFO(\"osversion\")");
            unknown.getCTCell().setV("7");
            row.getCell(0).setCellValue(5);

            PoiExcelHelper.evaluateFormulas(row.getSheet(), new GridRegion(0, 0, 3, 3));

            assertEquals(50, row.getCell(1).getNumericCellValue());
            assertEquals(7, unknown.getNumericCellValue());
        }
    }

    /** A table of another workbook keeps its Excel formulas as formulas and its OpenL formulas as texts. */
    @Test
    void copiesTheFormulasOfATableOfAnotherWorkbookAsTheyAre() throws Exception {
        try (var from = new XSSFWorkbook(); var to = new XSSFWorkbook()) {
            var row = from.createSheet().createRow(0);
            row.createCell(0).setCellFormula("2*3");
            row.createCell(1).setCellValue("= $F1 * 2");
            var rate = from.createName();
            rate.setNameName("Rate");
            rate.setRefersToFormula("7");
            row.createCell(2).setCellFormula("Rate");
            row.getCell(2).getCTCell().setV("7");
            to.createSheet();
            var builder = new TableBuilder(grid(to), null);

            builder.beginTable(3, 1);
            var written = builder.getTableRegion();
            builder.writeGridTable(new GridTable(new GridRegion(0, 0, 0, 2), grid(from)));
            builder.endTable();

            var copy = to.getSheetAt(0).getRow(written.getTop());
            var left = written.getLeft();
            assertEquals(CellType.FORMULA, copy.getCell(left).getCellType());
            assertEquals("2*3", copy.getCell(left).getCellFormula());
            assertEquals(CellType.STRING, copy.getCell(left + 1).getCellType());
            assertEquals("= $F1 * 2", copy.getCell(left + 1).getStringCellValue());
            // A formula naming what the workbook written to has not keeps the value it computed.
            assertEquals(CellType.NUMERIC, copy.getCell(left + 2).getCellType());
            assertEquals(7, copy.getCell(left + 2).getNumericCellValue());
        }
    }

    private static XlsSheetGridModel grid(Workbook workbook) {
        var source = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule("./test/rules/XlsSheetGridModelTest.xls"),
                new SimpleWorkbookLoader(workbook));
        return new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, source));
    }
}
