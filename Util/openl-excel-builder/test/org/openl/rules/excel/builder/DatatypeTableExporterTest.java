package org.openl.rules.excel.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            var headerRow = dtsSheet.getRow(TOP_MARGIN);
            assertNotNull(headerRow);
            var headerText = headerRow.getCell(1).getStringCellValue();
            assertEquals("Datatype Test", headerText);

            var stringFieldRow = dtsSheet.getRow(TOP_MARGIN + 1);
            assertNotNull(stringFieldRow);
            var dtCell = stringFieldRow.getCell(DT_TYPE_CELL);
            assertNotNull(dtCell);
            var typeCell = dtCell.getStringCellValue();
            var nameCell = stringFieldRow.getCell(DT_NAME_CELL);
            assertNotNull(nameCell);
            var name = nameCell.getStringCellValue();
            var dvCell = stringFieldRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(dvCell);
            var defaultValue = dvCell.getStringCellValue();
            assertEquals(STRING_TYPE, typeCell);
            assertEquals("type", name);
            assertEquals("Hello, World", defaultValue);

            var doubleFieldRow = dtsSheet.getRow(TOP_MARGIN + 2);
            assertNotNull(doubleFieldRow);
            var doubleTypeCell = doubleFieldRow.getCell(DT_TYPE_CELL);
            assertNotNull(doubleTypeCell);
            var doubleTypeCellValue = doubleTypeCell.getStringCellValue();
            var doubleNameCell = doubleFieldRow.getCell(DT_NAME_CELL);
            assertNotNull(doubleNameCell);
            var doubleNameCellValue = doubleNameCell.getStringCellValue();
            var doubleDefaultValueCell = doubleFieldRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(doubleDefaultValueCell);
            var doubleDefaultValue = doubleDefaultValueCell.getNumericCellValue();
            assertEquals("Double", doubleTypeCellValue);
            assertEquals("sum", doubleNameCellValue);
            assertEquals(0.0d, doubleDefaultValue, 1e-8);

            var dateRow = dtsSheet.getRow(TOP_MARGIN + 3);
            assertNotNull(dateRow);
            var dateTypeCell = dateRow.getCell(DT_TYPE_CELL);
            assertNotNull(dateTypeCell);
            var dateCellType = dateTypeCell.getStringCellValue();
            var dateNameCell = dateRow.getCell(DT_NAME_CELL);
            assertNotNull(dateNameCell);
            var dateCellName = dateNameCell.getStringCellValue();
            var dateDefaultValueCell = dateRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(dateDefaultValueCell);
            var dateCellValue = dateDefaultValueCell.getDateCellValue();
            assertEquals("Date", dateCellType);
            assertEquals("registrationDate", dateCellName);
            assertEquals(dateValue, dateCellValue);

            var booleanRow = dtsSheet.getRow(TOP_MARGIN + 4);
            assertNotNull(booleanRow);
            var boolTypeCell = booleanRow.getCell(DT_TYPE_CELL);
            assertNotNull(boolTypeCell);
            var booleanCellType = boolTypeCell.getStringCellValue();
            var boolNameCell = booleanRow.getCell(DT_NAME_CELL);
            assertNotNull(boolNameCell);
            var booleanCellName = boolNameCell.getStringCellValue();
            var boolDefaultCell = booleanRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(boolDefaultCell);
            var booleanCellValue = boolDefaultCell.getBooleanCellValue();
            assertEquals("Boolean", booleanCellType);
            assertEquals("isOk", booleanCellName);
            assertTrue(booleanCellValue);

            var customRow = dtsSheet.getRow(TOP_MARGIN + 5);
            assertNotNull(customRow);
            var customTypeCell = customRow.getCell(DT_TYPE_CELL);
            assertNotNull(customTypeCell);
            var customCellType = customTypeCell.getStringCellValue();
            var customNameCell = customRow.getCell(DT_NAME_CELL);
            assertNotNull(customNameCell);
            var customCellName = customNameCell.getStringCellValue();
            var customDefaultValueCell = customRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(customDefaultValueCell);
            var customCellValue = customDefaultValueCell.getStringCellValue();
            assertEquals("Human", customCellType);
            assertEquals("driver", customCellName);
            assertEquals("", customCellValue);

            var dateTimeRow = dtsSheet.getRow(TOP_MARGIN + 6);
            assertNotNull(dateTimeRow);
            var dateTimeCell = dateTimeRow.getCell(DT_TYPE_CELL);
            assertNotNull(dateTimeCell);
            var dateTimeCellType = dateTimeCell.getStringCellValue();
            var dateTimeNameCell = dateTimeRow.getCell(DT_NAME_CELL);
            assertNotNull(dateTimeNameCell);
            var dateTimeCellName = dateTimeNameCell.getStringCellValue();
            var dateTimeDefaultValueCell = dateTimeRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(dateTimeDefaultValueCell);
            var offsetDateTime = dateTimeDefaultValueCell.getLocalDateTimeCellValue()
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
            assertEquals("Date", dateTimeCellType);
            assertEquals("registrationDateTime", dateTimeCellName);
            assertNotNull(offsetDateTime);

            var floatFieldRow = dtsSheet.getRow(TOP_MARGIN + 7);
            assertNotNull(floatFieldRow);
            var floatTypeCell = floatFieldRow.getCell(DT_TYPE_CELL);
            assertNotNull(floatTypeCell);
            var floatTypeCellValue = floatTypeCell.getStringCellValue();
            var floatNameCell = floatFieldRow.getCell(DT_NAME_CELL);
            assertNotNull(floatNameCell);
            var floatNameCellValue = floatNameCell.getStringCellValue();
            var floatDefaultValueCell = floatFieldRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(floatDefaultValueCell);
            var floatDefaultCell = floatDefaultValueCell.getNumericCellValue();
            assertEquals("Float", floatTypeCellValue);
            assertEquals("weight", floatNameCellValue);
            assertEquals(1.3124124, floatDefaultCell, 1e-8);

            var bigDecimalRow = dtsSheet.getRow(TOP_MARGIN + 8);
            assertNotNull(bigDecimalRow);
            var bigDecimalTypeCell = bigDecimalRow.getCell(DT_TYPE_CELL);
            assertNotNull(bigDecimalTypeCell);
            var bigDecimalTypeCellValue = bigDecimalTypeCell.getStringCellValue();
            var bigDecimalNameCell = bigDecimalRow.getCell(DT_NAME_CELL);
            assertNotNull(bigDecimalNameCell);
            var bigDecimalNameCellValue = bigDecimalNameCell.getStringCellValue();
            var bdDefaultValueCell = bigDecimalRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(bdDefaultValueCell);
            var bdDefValue = bdDefaultValueCell.getStringCellValue();
            assertEquals("BigDecimal", bigDecimalTypeCellValue);
            assertEquals("bigNum", bigDecimalNameCellValue);
            assertEquals("2975671681509007947508815", bdDefValue);

            var bigIntegerRow = dtsSheet.getRow(TOP_MARGIN + 9);
            assertNotNull(bigIntegerRow);
            var bigIntegerTypeCell = bigIntegerRow.getCell(DT_TYPE_CELL);
            assertNotNull(bigIntegerTypeCell);
            var bigIntegerTypeCellValue = bigIntegerTypeCell.getStringCellValue();
            var bigIntegerNameCell = bigIntegerRow.getCell(DT_NAME_CELL);
            assertNotNull(bigIntegerNameCell);
            var bigIntegerNameCellValue = bigIntegerNameCell.getStringCellValue();
            var biDefaultValueCell = bigIntegerRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(biDefaultValueCell);
            var biDefValue = biDefaultValueCell.getNumericCellValue();
            assertEquals("BigInteger", bigIntegerTypeCellValue);
            assertEquals("bigInt", bigIntegerNameCellValue);
            assertEquals(10.0, biDefValue, 1e-8);

            var nextModelHeaderRow = dtsSheet.getRow(TOP_MARGIN + 12);
            assertNotNull(nextModelHeaderRow);
            var nextModelHeaderCell = nextModelHeaderRow.getCell(1);
            assertNotNull(nextModelHeaderCell);
            assertEquals("Datatype NextModel extends Test", nextModelHeaderCell.getStringCellValue());
            var nextModelRow = dtsSheet.getRow(TOP_MARGIN + 13);
            var nextModelDtCell = nextModelRow.getCell(DT_TYPE_CELL);
            assertNotNull(nextModelDtCell);
            var nextModelNameCell = nextModelRow.getCell(DT_NAME_CELL);
            assertNotNull(nextModelNameCell);
            var nextModelDVCell = nextModelRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(nextModelDVCell);
            assertEquals(STRING_TYPE, nextModelDtCell.getStringCellValue());
            assertEquals("color", nextModelNameCell.getStringCellValue());
            assertEquals("red", nextModelDVCell.getStringCellValue());
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

        try (var bos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateDataTypes(asSet(dt), bos);
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

    @SafeVarargs
    private static <T> Set<T> asSet(T... args) {
        return new LinkedHashSet<>(Arrays.asList(args));
    }
}
