package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.xls.merge.MergeFixture.RULES;
import static org.openl.rules.xls.merge.MergeFixture.cell;
import static org.openl.rules.xls.merge.MergeFixture.number;
import static org.openl.rules.xls.merge.MergeFixture.set;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ArrayFormulaMergeTest {

    private static final MergeFixture FIXTURE = new MergeFixture(MergeFixture::tables);
    /** The tables, with an array formula over H1:H2 that doubles the first rates. */
    private static final MergeFixture WITH_ARRAY = new MergeFixture(workbook -> {
        MergeFixture.tables(workbook);
        workbook.getSheet(RULES).setArrayFormula("B4:B5*2", CellRangeAddress.valueOf("H1:H2"));
    });

    @Test
    void sheetOfTheirs_keepsItsArrayFormulas() throws IOException {
        assertArraysOfAddedSheet(XSSFWorkbook::new);
    }

    @Test
    void sheetOfTheirsInXls_keepsItsArrayFormulas() throws IOException {
        assertArraysOfAddedSheet(HSSFWorkbook::new);
    }

    @Test
    void arrayFormulasOfTheirs_areMergedCellByCell() throws IOException {
        var merged = FIXTURE.mergeXlsx(sheet -> set(sheet, 3, 1, 10), sheet -> {
            sheet.setArrayFormula("SUM(B5:B6*2)", CellRangeAddress.valueOf("E20"));
            sheet.setArrayFormula("B5:B6*2", CellRangeAddress.valueOf("E22:E23"));
        });

        assertArray(merged, "E20", "E20", "SUM(B5:B6*2)");
        assertArray(merged, "E22", "E22:E23", "B5:B6*2");
        assertTrue(cell(merged, 22, 4).isPartOfArrayFormulaGroup());
        assertEquals(10, number(merged, 19, 4));
        assertEquals(6, number(merged, 22, 4));
        assertEquals(10, number(merged, 3, 1));
    }

    @Test
    void sheetOfTheirs_replacesTheArrayFormulasOfOurs() throws IOException {
        assertArraysReplaced(XSSFWorkbook::new);
    }

    @Test
    void sheetOfTheirsInXls_replacesTheArrayFormulasOfOurs() throws IOException {
        assertArraysReplaced(HSSFWorkbook::new);
    }

    @Test
    void arrayFormulaChangedInTheirs_replacesTheOneOfOursCellByCell() throws IOException {
        var merged = WITH_ARRAY.mergeXlsx(sheet -> set(sheet, 5, 1, 30), sheet -> {
            sheet.removeArrayFormula(cell(sheet, 0, 7));
            sheet.setArrayFormula("B4:B5*3", CellRangeAddress.valueOf("H1:H2"));
        });

        assertArray(merged, "H1", "H1:H2", "B4:B5*3");
        assertTrue(cell(merged, 1, 7).isPartOfArrayFormulaGroup());
        assertEquals(3, number(merged, 0, 7));
        assertEquals(6, number(merged, 1, 7));
        assertEquals(30, number(merged, 5, 1));
    }

    @Test
    void arrayFormulaRemovedInTheirs_leavesItsValuesCellByCell() throws IOException {
        var merged = WITH_ARRAY.mergeXlsx(sheet -> set(sheet, 5, 1, 30), sheet -> {
            sheet.removeArrayFormula(cell(sheet, 0, 7));
            set(sheet, 0, 7, 7);
            set(sheet, 1, 7, 8);
        });

        assertFalse(cell(merged, 0, 7).isPartOfArrayFormulaGroup());
        assertEquals(7, number(merged, 0, 7));
        assertEquals(8, number(merged, 1, 7));
    }

    @Test
    void cellOfOurArrayFormulaRestyledInTheirs_keepsTheArrayFormula() throws IOException {
        var merged = WITH_ARRAY.mergeXlsx(sheet -> set(sheet, 5, 1, 30), sheet -> MergeFixture.bold(sheet, 1, 7));

        assertArray(merged, "H1", "H1:H2", "B4:B5*2");
        assertTrue(cell(merged, 1, 7).isPartOfArrayFormulaGroup());
        assertTrue(MergeFixture.isBold(merged, 1, 7));
        assertEquals(4, number(merged, 1, 7));
    }

    @Test
    void cellWrittenOverAnArrayFormulaCopiedBefore_isNoArrayFormula() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var source = workbook.createSheet("Source");
            set(source, 0, 0, 1);
            set(source, 1, 0, 2);
            source.setArrayFormula("A1:A2*2", CellRangeAddress.valueOf("B1:B2"));
            set(source, 0, 2, 7);
            var src = new Cursor(workbook, source);
            var dest = new Cursor(workbook, workbook.createSheet("Merged"));
            dest.cell = dest.sheet.createRow(0).createCell(1);
            var blank = new Cursor(workbook, dest.sheet);
            blank.cell = dest.sheet.getRow(0).createCell(2);

            // the first cell of the array, written over with a value
            src.cell = cell(source, 0, 1);
            XlsSheetCopier.copyValue(src, dest);
            src.cell = cell(source, 0, 2);
            XlsSheetCopier.copyValue(src, dest);
            // the first cell of another array, cleared afterwards
            src.cell = cell(source, 0, 1);
            XlsSheetCopier.copyValue(src, blank);
            blank.cell.setBlank();
            dest.arrayFormulas.addAll(blank.arrayFormulas);
            XlsSheetCopier.restoreArrayFormulas(dest);

            assertFalse(dest.cell.isPartOfArrayFormulaGroup());
            assertEquals(7, dest.cell.getNumericCellValue());
            assertFalse(blank.cell.isPartOfArrayFormulaGroup());
        }
    }

    /**
     * Takes from their branch a sheet that holds an array formula over several cells in every revision.
     */
    private static void assertArraysReplaced(Supplier<Workbook> format) throws IOException {
        var merged = WITH_ARRAY.merge(format, sheet -> {
        }, sheet -> set(sheet, 3, 1, 10));

        assertArray(merged, "H1", "H1:H2", "B4:B5*2");
        assertTrue(cell(merged, 1, 7).isPartOfArrayFormulaGroup());
        assertEquals(20, number(merged, 0, 7));
        assertEquals(4, number(merged, 1, 7));
    }

    /**
     * Merges a sheet their branch added with array formulas over several cells and over one cell.
     */
    private static void assertArraysOfAddedSheet(Supplier<Workbook> format) throws IOException {
        Consumer<Sheet> addArrays = sheet -> {
            var arrays = sheet.getWorkbook().createSheet("Arrays");
            set(arrays, 0, 0, 1);
            set(arrays, 1, 0, 2);
            arrays.setArrayFormula("A1:A2*2", CellRangeAddress.valueOf("B1:B2"));
            arrays.setArrayFormula("SUM(A1:A2*2)", CellRangeAddress.valueOf("C1"));
        };
        var merged = FIXTURE.merge(format, sheet -> set(sheet, 3, 1, 10), addArrays).getWorkbook();

        var arrays = merged.getSheet("Arrays");
        assertArray(arrays, "B1", "B1:B2", "A1:A2*2");
        assertTrue(cell(arrays, 1, 1).isPartOfArrayFormulaGroup());
        assertArray(arrays, "C1", "C1", "SUM(A1:A2*2)");
        assertEquals(4, number(arrays, 1, 1));
        assertEquals(6, number(arrays, 0, 2));
        assertEquals(10, number(merged.getSheet(RULES), 3, 1));
    }

    private static void assertArray(Sheet sheet, String first, String range, String formula) {
        var address = CellRangeAddress.valueOf(first);
        var cell = cell(sheet, address.getFirstRow(), address.getFirstColumn());
        assertTrue(cell.isPartOfArrayFormulaGroup());
        assertEquals(range, cell.getArrayFormulaRange().formatAsString());
        assertEquals(formula, cell.getCellFormula());
    }
}
