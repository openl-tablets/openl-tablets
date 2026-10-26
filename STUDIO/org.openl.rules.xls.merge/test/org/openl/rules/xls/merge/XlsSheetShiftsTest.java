package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class XlsSheetShiftsTest {

    @Test
    void isMoved_tellsEditsInPlaceFromInsertedAndDeletedElements() {
        assertFalse(XlsSheetShifts.isMoved(new long[]{1, 2, 3}, new long[]{1, 9, 3}));
        assertFalse(XlsSheetShifts.isMoved(new long[]{1, 2, 0}, new long[]{1, 2, 3}));
        assertFalse(XlsSheetShifts.isMoved(new long[]{7, 1, 1}, new long[]{1, 1, 1}));
        assertTrue(XlsSheetShifts.isMoved(new long[]{1, 2, 0}, new long[]{1, 9, 2}));
        assertTrue(XlsSheetShifts.isMoved(new long[]{1, 9, 2}, new long[]{1, 2, 0}));
        assertTrue(XlsSheetShifts.isMoved(new long[]{1, 0, 2}, new long[]{1, 2, 0}));
    }

    @Test
    void isMoved_takesAChangeASpentBudgetCannotCheckAsMove() {
        // The budget is shared: the first check spends it, so the same check is a move the second time.
        var budget = new XlsSheetShifts.Budget(1);

        assertFalse(XlsSheetShifts.isMoved(new long[]{1, 2, 3}, new long[]{1, 9, 3}, budget));
        assertTrue(XlsSheetShifts.isMoved(new long[]{1, 2, 3}, new long[]{1, 9, 3}, budget));
    }

    @Test
    void isShifted_findsCellsMovedDownBesideCellsInPlace() throws IOException {
        try (var workbooks = new Workbooks()) {
            // The rows of a table at A1:B3 move down while the table beside it, at D1:E3, stays.
            var base = workbooks.sheet("A", "B", "C", "x", "y", "z");
            var shifted = workbooks.sheet("A", "new", "B", "x", "y", "z");
            var edited = workbooks.sheet("A", "new", "C", "x", "y", "z");

            assertTrue(XlsSheetShifts.isShifted(base, shifted, base));
            assertFalse(XlsSheetShifts.isShifted(base, edited, base));
        }
    }

    @Test
    void isShifted_findsCellsMovedSidewaysAboveCellsInPlace() throws IOException {
        try (var workbooks = new Workbooks()) {
            // The columns of a table on the first row move right while the table on the third row stays.
            var base = workbooks.transposed("A", "B", "C", "x", "y", "z");
            var shifted = workbooks.transposed("A", "new", "B", "x", "y", "z");

            assertTrue(XlsSheetShifts.isShifted(base, base, shifted));
        }
    }

    /**
     * Sheets of two tables of three cells each, side by side or one above the other, in one workbook.
     */
    private static final class Workbooks implements AutoCloseable {

        private final XSSFWorkbook workbook = new XSSFWorkbook();

        /** The first three values in A1:A3, the other three in D1:D3. */
        Sheet sheet(String... values) {
            var sheet = workbook.createSheet();
            for (var i = 0; i < 3; i++) {
                var row = sheet.createRow(i);
                row.createCell(0).setCellValue(values[i]);
                row.createCell(3).setCellValue(values[3 + i]);
            }
            return sheet;
        }

        /** The first three values in A1:C1, the other three in A4:C4. */
        Sheet transposed(String... values) {
            var sheet = workbook.createSheet();
            var first = sheet.createRow(0);
            var second = sheet.createRow(3);
            for (var i = 0; i < 3; i++) {
                first.createCell(i).setCellValue(values[i]);
                second.createCell(i).setCellValue(values[3 + i]);
            }
            return sheet;
        }

        @Override
        public void close() throws IOException {
            workbook.close();
        }
    }

    @Test
    void isMoved_takesTooManyChangesAsMove() {
        var base = new long[10_000];
        var other = new long[10_000];
        for (var i = 0; i < base.length; i++) {
            base[i] = i;
            other[i] = -i - 1;
        }
        assertTrue(XlsSheetShifts.isMoved(base, other));
    }
}
