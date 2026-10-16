package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
