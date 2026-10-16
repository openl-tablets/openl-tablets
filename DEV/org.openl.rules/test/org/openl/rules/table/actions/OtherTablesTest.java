package org.openl.rules.table.actions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * The tables an edit must leave room around: the edited one stands in B2:C4, one more table under it from row 7, and
 * one on its right from column F.
 */
class OtherTablesTest {

    @TempDir
    Path tempDir;

    private XlsSheetGridModel grid;
    private IGridTable edited;

    @BeforeEach
    void setUp() throws IOException {
        var file = tempDir.resolve("tables.xlsx");
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet();
            write(sheet, 1, 1, "Datatype Edited");
            write(sheet, 2, 1, "String", "code");
            write(sheet, 3, 1, "String", "text");
            write(sheet, 6, 1, "Datatype Below");
            write(sheet, 7, 1, "String", "name");
            sheet.getRow(1).createCell(5).setCellValue("Datatype Right");
            sheet.getRow(2).createCell(5).setCellValue("String");
            workbook.write(out);
        }
        var workbook = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(file.toString()));
        grid = spy(new XlsSheetGridModel(new XlsSheetSourceCodeModule(0, workbook)));
        edited = new GridTable(1, 1, 3, 2, grid);
    }

    @Test
    void leavesRoomAroundTheOtherTablesOfTheSheet() {
        var otherTables = new OtherTables(edited);

        // A row laid down under the table keeps a blank row before the table under it; a second one would not.
        assertTrue(UndoableInsertRowsAction.canInsertRows(edited, 1, otherTables));
        assertFalse(UndoableInsertRowsAction.canInsertRows(edited, 2, otherTables));
        // The same on the right, where the next table starts two columns away.
        assertTrue(UndoableInsertColumnsAction.canInsertColumns(edited, 1, otherTables));
        assertFalse(UndoableInsertColumnsAction.canInsertColumns(edited, 2, otherTables));
    }

    @Test
    void readsTheSheetOnceForEveryInsertOfAnEdit() {
        var otherTables = new OtherTables(edited);

        for (var lines = 0; lines < 5; lines++) {
            UndoableInsertRowsAction.canInsertRows(edited, 1, otherTables);
            UndoableInsertColumnsAction.canInsertColumns(edited, 1, otherTables);
        }

        verify(grid, times(1)).getTables();
    }

    @Test
    void neverTakesTheEditedTableForAnotherOne() {
        var otherTables = new OtherTables(edited);
        UndoableInsertRowsAction.canInsertRows(edited, 1, otherTables);

        // The edit took the last row away: the rows it lays down now stand where the table stood when it was found.
        var shrunk = new GridTable(1, 1, 2, 2, grid);

        assertTrue(UndoableInsertRowsAction.canInsertRows(shrunk, 2, otherTables));
        assertFalse(UndoableInsertRowsAction.canInsertRows(shrunk, 3, otherTables));
    }

    private static void write(Sheet sheet, int row, int column, String... values) {
        var sheetRow = sheet.createRow(row);
        for (var i = 0; i < values.length; i++) {
            sheetRow.createCell(column + i).setCellValue(values[i]);
        }
    }
}
