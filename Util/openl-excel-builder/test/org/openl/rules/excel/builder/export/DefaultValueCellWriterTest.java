package org.openl.rules.excel.builder.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DefaultValueCellWriterTest {

    /** A whole number a double holds is written as a number. */
    @ParameterizedTest
    @CsvSource({"Integer, 5", "Long, 9007199254740992", "BigInteger, 12345678"})
    void writesWholeNumbersAsNumbers(String type, String value) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = cellOf(workbook);

            DefaultValueCellWriter.writeValueToCell(type, value, cell);

            assertEquals(CellType.NUMERIC, cell.getCellType());
            assertEquals(new BigDecimal(value).doubleValue(), cell.getNumericCellValue());
        }
    }

    /** A whole number a double rounds is written as its text, with every digit. */
    @ParameterizedTest
    @CsvSource({"Long, 9007199254740993", "BigInteger, 123456789012345678901"})
    void writesWholeNumbersBeyondDoubleAsText(String type, String value) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = cellOf(workbook);

            DefaultValueCellWriter.writeValueToCell(type, value, cell);

            assertEquals(CellType.STRING, cell.getCellType());
            assertEquals(value, cell.getStringCellValue());
        }
    }

    /** The number of a vocabulary field is written as a number when a double holds it, as its text otherwise. */
    @Test
    void writesVocabularyNumbers() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var held = cellOf(workbook);
            DefaultValueCellWriter.writeValueToCell("Rate", new BigDecimal("0.5"), held);
            assertEquals(CellType.NUMERIC, held.getCellType());

            var beyond = cellOf(workbook);
            DefaultValueCellWriter.writeValueToCell("Rate", new BigDecimal("0.66666666666666663"), beyond);
            assertEquals("0.66666666666666663", beyond.getStringCellValue());
        }
    }

    /** A value its type cannot read leaves the cell empty. */
    @Test
    void leavesCellEmptyForUnreadableNumber() throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var cell = cellOf(workbook);

            DefaultValueCellWriter.writeValueToCell("Long", "abc", cell);

            assertEquals(CellType.BLANK, cell.getCellType());
        }
    }

    private static XSSFCell cellOf(XSSFWorkbook workbook) {
        var sheet = workbook.getNumberOfSheets() == 0 ? workbook.createSheet() : workbook.getSheetAt(0);
        return sheet.createRow(sheet.getPhysicalNumberOfRows()).createCell(0);
    }
}
