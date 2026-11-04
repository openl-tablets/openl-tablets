package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;

import org.openl.excel.parser.ExcelReaderFactory;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * A cell whose Excel formula computes an error, or which holds an error typed in, reads as the text Excel shows for
 * it.
 */
class ExcelErrorValuesTest {

    @Test
    void anXlsxCellReadsAsTheErrorExcelShows() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var row = errors(workbook);
            // Excel 365 writes errors Apache POI does not know, such as the one of a formula spilling over a value
            var spill = (XSSFCell) row.createCell(4);
            spill.getCTCell().setT(STCellType.E);
            spill.getCTCell().setV("#SPILL!");

            assertEquals("#DIV/0!", PoiExcelHelper.getCellValue(row.getCell(0)));
            assertEquals("#N/A", PoiExcelHelper.getCellValue(row.getCell(1)));
            assertEquals("#REF!", PoiExcelHelper.getCellValue(row.getCell(2)));
            assertEquals("#VALUE!", PoiExcelHelper.getCellValue(row.getCell(3)));
            assertEquals("#SPILL!", PoiExcelHelper.getCellValue(spill));
        }
    }

    @Test
    void anXlsCellReadsAsTheErrorExcelShows() throws IOException {
        try (var workbook = new HSSFWorkbook()) {
            var row = errors(workbook);

            assertEquals("#DIV/0!", PoiExcelHelper.getCellValue(row.getCell(0)));
            assertEquals("#N/A", PoiExcelHelper.getCellValue(row.getCell(1)));
            assertEquals("#REF!", PoiExcelHelper.getCellValue(row.getCell(2)));
            assertEquals("#VALUE!", PoiExcelHelper.getCellValue(row.getCell(3)));
        }
    }

    /**
     * A module is compiled from the cells as the sequential reader reads them, an {@code .xlsx} workbook and an
     * {@code .xls} one alike: an error is no empty cell.
     */
    @ParameterizedTest
    @ValueSource(strings = {".xlsx", ".xls"})
    void theSequentialReaderReadsTheError(String extension) throws Exception {
        var file = Files.createTempFile("errors", extension);
        try {
            try (var workbook = ".xls".equals(extension) ? new HSSFWorkbook() : new XSSFWorkbook();
                 var out = Files.newOutputStream(file)) {
                errors(workbook);
                workbook.write(out);
            }
            try (var reader = ExcelReaderFactory.sequentialFactory().create(file.toString())) {
                var cells = reader.getCells(reader.getSheets().getFirst());

                assertEquals("#DIV/0!", cells[0][0]);
                assertEquals("#N/A", cells[0][1]);
                assertEquals("#REF!", cells[0][2]);
                assertEquals("#VALUE!", cells[0][3]);
            }
        } finally {
            Files.delete(file);
        }
    }

    /** OpenL Studio reads the cells of a table through the grid: a table read shows the error too. */
    @Test
    void aCellOfTheGridReadsAsTheError() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            errors(workbook);
            var source = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule("./test/rules/XlsCellTest.xls"),
                    new SimpleWorkbookLoader(workbook));
            var grid = new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, source));

            assertEquals("#DIV/0!", grid.getCell(0, 0).getObjectValue());
            assertEquals("#DIV/0!", grid.getCell(0, 0).getStringValue());
            assertEquals("#REF!", grid.getCell(2, 0).getObjectValue());
        }
    }

    /**
     * The first row of a new sheet: {@code =1/0}, {@code =NA()}, {@code #REF!} typed in, and {@code ="a"+1}, each
     * formula computed.
     */
    private static Row errors(Workbook workbook) {
        var row = workbook.createSheet("Errors").createRow(0);
        formula(row.createCell(0), "1/0");
        formula(row.createCell(1), "NA()");
        row.createCell(2).setCellErrorValue(FormulaError.REF.getCode());
        formula(row.createCell(3), "\"a\"+1");
        return row;
    }

    private static void formula(Cell cell, String formula) {
        cell.setCellFormula(formula);
        cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(cell);
    }
}
