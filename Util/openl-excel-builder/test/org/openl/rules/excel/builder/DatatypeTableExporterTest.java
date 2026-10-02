package org.openl.rules.excel.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.excel.builder.SheetCells.cell;
import static org.openl.rules.excel.builder.SheetCells.rowTexts;
import static org.openl.rules.excel.builder.SheetCells.text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.ProjectModel;

class DatatypeTableExporterTest {

    private static final String TEST_PROJECT = "datatype_test_project";
    private static final int TOP_MARGIN = 2;
    private static final int DT_TYPE_CELL = 1;
    private static final int DT_NAME_CELL = 2;
    private static final int DT_DEFAULT_VALUE_CELL = 3;
    private static final String STRING_TYPE = "String";
    private static final String DATATYPE_TEST_PROJECT_NAME = "datatype_test_project.xlsx";

    @Test
    void testDatatypeExport() throws IOException {
        var dt = new DatatypeModel("Test");

        var stringField = new FieldModel("type", STRING_TYPE, "Hello, World");
        var doubleField = new FieldModel("sum", "Double", 0.0d);
        var floatField = new FieldModel("weight", "Float", 1.3124124f);
        var dateValue = new Date();
        var dateField = new FieldModel("registrationDate", "Date", dateValue);

        OffsetDateTime dateTimeValue = OffsetDateTime.now(ZoneId.systemDefault());
        var dateTimeField = new FieldModel("registrationDateTime", "Date", dateTimeValue);

        var booleanField = new FieldModel("isOk", "Boolean", true);

        var bigDecimalField = new FieldModel("bigNum",
                "BigDecimal",
                new BigDecimal("2975671681509007947508815"));

        var bigIntegerField = new FieldModel("bigInt", "BigInteger", BigInteger.TEN);
        var customTypeField = new FieldModel("driver", "Human");

        dt.setFields(Arrays.asList(stringField,
                doubleField,
                dateField,
                booleanField,
                customTypeField,
                dateTimeField,
                floatField,
                bigDecimalField,
                bigIntegerField));

        var oneMoreModel = new DatatypeModel("NextModel");
        var nextModelField = new FieldModel("color", STRING_TYPE, "red");
        oneMoreModel.setParent("Test");
        oneMoreModel.setFields(List.of(nextModelField));

        var projectModel = new ProjectModel(TEST_PROJECT,
                false,
                asSet(dt, oneMoreModel),
                List.of(),
                List.of(),
                List.of());
        ExcelFileBuilder.generateProject(projectModel);

        try (var wb = new XSSFWorkbook(
                new FileInputStream("../openl-excel-builder/" + DATATYPE_TEST_PROJECT_NAME))) {
            var dtsSheet = wb.getSheet("Datatypes");
            assertNotNull(dtsSheet);
            assertEquals("Datatype Test", text(dtsSheet, TOP_MARGIN, 1));

            assertEquals(List.of(STRING_TYPE, "type", "Hello, World"), fieldTexts(dtsSheet, TOP_MARGIN + 1));

            assertEquals(List.of("Double", "sum"), typeAndName(dtsSheet, TOP_MARGIN + 2));
            assertEquals(0.0d, defaultValueCell(dtsSheet, TOP_MARGIN + 2).getNumericCellValue(), 1e-8);

            assertEquals(List.of("Date", "registrationDate"), typeAndName(dtsSheet, TOP_MARGIN + 3));
            assertEquals(dateValue, defaultValueCell(dtsSheet, TOP_MARGIN + 3).getDateCellValue());

            assertEquals(List.of("Boolean", "isOk"), typeAndName(dtsSheet, TOP_MARGIN + 4));
            assertTrue(defaultValueCell(dtsSheet, TOP_MARGIN + 4).getBooleanCellValue());

            assertEquals(List.of("Human", "driver", ""), fieldTexts(dtsSheet, TOP_MARGIN + 5));

            assertEquals(List.of("Date", "registrationDateTime"), typeAndName(dtsSheet, TOP_MARGIN + 6));
            var offsetDateTime = defaultValueCell(dtsSheet, TOP_MARGIN + 6).getLocalDateTimeCellValue()
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
            assertNotNull(offsetDateTime);

            assertEquals(List.of("Float", "weight"), typeAndName(dtsSheet, TOP_MARGIN + 7));
            assertEquals(1.3124124, defaultValueCell(dtsSheet, TOP_MARGIN + 7).getNumericCellValue(), 1e-8);

            assertEquals(List.of("BigDecimal", "bigNum", "2975671681509007947508815"),
                    fieldTexts(dtsSheet, TOP_MARGIN + 8));

            assertEquals(List.of("BigInteger", "bigInt"), typeAndName(dtsSheet, TOP_MARGIN + 9));
            assertEquals(10.0, defaultValueCell(dtsSheet, TOP_MARGIN + 9).getNumericCellValue(), 1e-8);

            assertEquals("Datatype NextModel extends Test", text(dtsSheet, TOP_MARGIN + 12, 1));
            assertEquals(List.of(STRING_TYPE, "color", "red"), fieldTexts(dtsSheet, TOP_MARGIN + 13));
        }

    }

    @Test
    void writeDataTypes() throws IOException {
        var dt = new DatatypeModel("Test");

        var stringField = new FieldModel("type", STRING_TYPE, "Hello, World");
        var doubleField = new FieldModel("sum", "Double", 0.0d);
        var dateValue = new Date();
        var dateField = new FieldModel("registrationDate", "Date", dateValue);
        var booleanField = new FieldModel("isOk", "Boolean", true);
        var customTypeField = new FieldModel("driver", "Human");
        dt.setFields(Arrays.asList(stringField, doubleField, dateField, booleanField, customTypeField));

        var projectModel = new ProjectModel(TEST_PROJECT, false, asSet(dt), List.of(), List.of(), List.of());
        try (var bos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateDataTypes(projectModel, bos);
            try (var fos = new FileOutputStream(DATATYPE_TEST_PROJECT_NAME)) {
                fos.write(bos.toByteArray());
            }
        }

        try (var wb = new XSSFWorkbook(
                new FileInputStream("../openl-excel-builder/" + DATATYPE_TEST_PROJECT_NAME))) {
            var dtsSheet = wb.getSheet("Datatypes");
            assertNotNull(dtsSheet);
        }
    }

    @AfterAll
    static void clean() throws IOException {
        var dir = new File("../openl-excel-builder");
        var files = dir.listFiles();
        assertNotNull(files);
        for (File file : files) {
            if (file.getName().equals(DATATYPE_TEST_PROJECT_NAME)) {
                Files.delete(file.toPath());
                break;
            }
        }
    }

    /**
     * Returns the type, the name and the default value of a field whose default value is written as a text.
     */
    private static List<String> fieldTexts(Sheet sheet, int row) {
        return rowTexts(sheet, row, DT_TYPE_CELL, DT_DEFAULT_VALUE_CELL);
    }

    private static List<String> typeAndName(Sheet sheet, int row) {
        return rowTexts(sheet, row, DT_TYPE_CELL, DT_NAME_CELL);
    }

    private static Cell defaultValueCell(Sheet sheet, int row) {
        return cell(sheet, row, DT_DEFAULT_VALUE_CELL);
    }

    @SafeVarargs
    private static <T> Set<T> asSet(T... args) {
        return new LinkedHashSet<>(Arrays.asList(args));
    }
}
