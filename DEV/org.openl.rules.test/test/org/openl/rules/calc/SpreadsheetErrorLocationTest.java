package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.openl.message.Severity;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.table.xls.XlsUrlParser;

class SpreadsheetErrorLocationTest {

    @TempDir
    private Path tempDir;

    private static List<Arguments> tableProperties() {
        return List.of(Arguments.of(1, "C8:D8"), Arguments.of(2, "C9:D9"));
    }

    @ParameterizedTest
    @MethodSource("tableProperties")
    void locatesFormulaErrorAfterTableProperties(int propertyCount, String expectedRange) throws Exception {
        assertEquals(expectedRange, getFormulaErrorRange(createProject(propertyCount)));
    }

    @Test
    void locatesFormulaErrorInVerticalTablePart() throws Exception {
        assertEquals("C14:D14", getFormulaErrorRange(createVerticalTablePartProject()));
    }

    @Test
    void locatesFormulaErrorInHorizontalTablePart() throws Exception {
        assertEquals("F8", getFormulaErrorRange(createHorizontalTablePartProject()));
    }

    private static String getFormulaErrorRange(Path project) throws Exception {
        var factory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setExecutionMode(false)
                .setProject(project.toString())
                .build();

        var message = factory.getCompiledOpenClass()
                .getAllMessages()
                .stream()
                .filter(candidate -> candidate.getSeverity() == Severity.ERROR)
                .filter(candidate -> candidate.getSummary().contains("Identifier 'BlaBla' is not found."))
                .findFirst()
                .orElseThrow();

        return new XlsUrlParser(message.getSourceLocation()).getRange();
    }

    private Path createProject(int propertyCount) throws IOException {
        var project = Files.createDirectory(tempDir.resolve("properties-" + propertyCount));
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            addSpreadsheet(sheet, propertyCount);
            try (var output = Files.newOutputStream(project.resolve("SprError.xlsx"))) {
                workbook.write(output);
            }
        }
        return project;
    }

    private Path createVerticalTablePartProject() throws IOException {
        var project = Files.createDirectory(tempDir.resolve("table-part"));
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            addVerticalTablePart(sheet, 4, "TablePart SplitSpreadsheet row 1 of 2");
            setCell(sheet, 5, 1, "Spreadsheet String MySpr (Integer a)");
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 1, 3));
            setCell(sheet, 6, 1, "Step");
            setCell(sheet, 6, 2, "Formula");
            sheet.addMergedRegion(new CellRangeAddress(6, 6, 2, 3));
            addVerticalTablePart(sheet, 12, "TablePart SplitSpreadsheet row 2 of 2");
            setCell(sheet, 13, 1, "Step1");
            setCell(sheet, 13, 2, "=BlaBla+100");
            sheet.addMergedRegion(new CellRangeAddress(13, 13, 2, 3));
            try (var output = Files.newOutputStream(project.resolve("TablePartError.xlsx"))) {
                workbook.write(output);
            }
        }
        return project;
    }

    private Path createHorizontalTablePartProject() throws IOException {
        var project = Files.createDirectory(tempDir.resolve("horizontal-table-part"));
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            setCell(sheet, 4, 1, "TablePart SplitSpreadsheet column 1 of 2");
            setCell(sheet, 5, 1, "Spreadsheet String MySpr (Integer a)");
            setCell(sheet, 6, 1, "Step");
            setCell(sheet, 7, 1, "Step1");

            setCell(sheet, 4, 5, "TablePart SplitSpreadsheet column 2 of 2");
            setCell(sheet, 5, 5, "Spreadsheet String MySpr (Integer a)");
            setCell(sheet, 6, 5, "Formula");
            setCell(sheet, 7, 5, "=BlaBla+100");
            try (var output = Files.newOutputStream(project.resolve("HorizontalTablePartError.xlsx"))) {
                workbook.write(output);
            }
        }
        return project;
    }

    private static void addVerticalTablePart(Sheet sheet, int row, String header) {
        setCell(sheet, row, 1, header);
        sheet.addMergedRegion(new CellRangeAddress(row, row, 1, 3));
    }

    private static void addSpreadsheet(Sheet sheet, int propertyCount) {
        setCell(sheet, 4, 1, "Spreadsheet String MySpr (Integer a)");
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 3));

        setCell(sheet, 5, 1, "properties");
        if (propertyCount == 2) {
            setCell(sheet, 5, 2, "state");
            setCell(sheet, 5, 3, "AL");
            sheet.addMergedRegion(new CellRangeAddress(5, 6, 1, 1));
        }
        var lastPropertyRow = 4 + propertyCount;
        setCell(sheet, lastPropertyRow, 2, "lob");
        setCell(sheet, lastPropertyRow, 3, "TEST");

        var headerRow = 5 + propertyCount;
        setCell(sheet, headerRow, 1, "Step");
        setCell(sheet, headerRow, 2, "Formula");
        sheet.addMergedRegion(new CellRangeAddress(headerRow, headerRow, 2, 3));

        setCell(sheet, headerRow + 1, 1, "Step1");
        setCell(sheet, headerRow + 1, 2, "=BlaBla+100");
        sheet.addMergedRegion(new CellRangeAddress(headerRow + 1, headerRow + 1, 2, 3));

        setCell(sheet, headerRow + 2, 1, "Step2");
        setCell(sheet, headerRow + 2, 2, "Test");
        sheet.addMergedRegion(new CellRangeAddress(headerRow + 2, headerRow + 2, 2, 3));
    }

    private static void setCell(Sheet sheet, int row, int column, String value) {
        var spreadsheetRow = sheet.getRow(row);
        if (spreadsheetRow == null) {
            spreadsheetRow = sheet.createRow(row);
        }
        spreadsheetRow.createCell(column).setCellValue(value);
    }
}
