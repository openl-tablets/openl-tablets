package org.openl.rules.excel.builder;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Reads the cells an exporter has written, so a test compares a whole row or column in one assertion.
 *
 * <p>A missing row or cell fails the reading test.
 */
final class SheetCells {

    private SheetCells() {
    }

    static Cell cell(Sheet sheet, int row, int column) {
        return sheet.getRow(row).getCell(column);
    }

    static String text(Sheet sheet, int row, int column) {
        return cell(sheet, row, column).getStringCellValue();
    }

    /**
     * Returns the texts of the cells from {@code firstColumn} to {@code lastColumn} inclusive in one row.
     */
    static List<String> rowTexts(Sheet sheet, int row, int firstColumn, int lastColumn) {
        return IntStream.rangeClosed(firstColumn, lastColumn).mapToObj(column -> text(sheet, row, column)).toList();
    }

    /**
     * Returns the texts of the cells from {@code firstRow} to {@code lastRow} inclusive in one column.
     */
    static List<String> columnTexts(Sheet sheet, int firstRow, int lastRow, int column) {
        return IntStream.rangeClosed(firstRow, lastRow).mapToObj(row -> text(sheet, row, column)).toList();
    }
}
