package org.openl.rules.excel.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.excel.builder.export.DataTableExporter.DATA_SHEET;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.util.StringUtils;

class DataTableExporterTest {

    private static final String STRING_TYPE = "String";
    private static final String DATA_TEST_PROJECT_NAME = "data_test_project.xlsx";
    private static final int TOP_MARGIN = 2;

    @Test
    void writeDataTables() throws IOException {
        var noFieldsModel = new DatatypeModel("NoFieldsModel");
        noFieldsModel.setFields(Collections.singletonList(new FieldModel("this", "Double")));
        var emptyModel = new DataModel("emptyDataTAble", "Object", null, noFieldsModel);

        var dt = new DatatypeModel("Test");

        var stringField = new FieldModel("type", STRING_TYPE, "Hello, World");
        var doubleField = new FieldModel("sum", "Double", 0.0d);
        var dateValue = new Date();
        var dateField = new FieldModel("registrationDate", "Date", dateValue);
        var booleanField = new FieldModel("isOk", "Boolean", true);
        var customTypeField = new FieldModel("driver", "Human");
        dt.setFields(Arrays.asList(stringField, doubleField, dateField, booleanField, customTypeField));
        var info = new PathInfo("/getTest",
                "/getTest",
                PathInfo.Operation.GET,
                new TypeInfo("Test", "Test", TypeInfo.Type.DATATYPE),
                "application/json",
                "application/json");
        var dm = new DataModel("getTest", "Test", info, dt);

        var secondModel = new DatatypeModel("MyModel");

        var integerField = new FieldModel("java_name", "String", "object");
        var sumField = new FieldModel("height", "Double", 134.44d);
        var isOkField = new FieldModel("isOk", "Boolean", false);
        secondModel.setFields(Arrays.asList(integerField, sumField, isOkField));
        var infoForNotOk = new PathInfo("/getMyModel",
                "/my/model",
                PathInfo.Operation.POST,
                new TypeInfo("Unknown", "Unknown", TypeInfo.Type.DATATYPE),
                "text/plain",
                "text/html");
        var myModel = new DataModel("getMyModel", "Test", infoForNotOk, secondModel);

        try (var bos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateDataTables(Arrays.asList(emptyModel, dm, myModel), bos);
            try (var fos = new FileOutputStream(DATA_TEST_PROJECT_NAME)) {
                fos.write(bos.toByteArray());
            }
        }

        try (var wb = new XSSFWorkbook(
                new FileInputStream("../openl-excel-builder/" + DATA_TEST_PROJECT_NAME))) {
            var dtsSheet = wb.getSheet(DATA_SHEET);
            assertNotNull(dtsSheet);

            var emptyModelRow = dtsSheet.getRow(TOP_MARGIN);
            assertNotNull(emptyModelRow);
            var headerCell = emptyModelRow.getCell(1);
            assertNotNull(headerCell);
            var emptyModelHeaderText = headerCell.getStringCellValue();
            assertEquals("Data Object emptyDataTAble", emptyModelHeaderText);

            var emptyModelSubheaderRow = dtsSheet.getRow(TOP_MARGIN + 1);
            assertNotNull(emptyModelSubheaderRow);
            var subheaderCell = emptyModelSubheaderRow.getCell(1);
            assertNotNull(subheaderCell);
            assertEquals("this", subheaderCell.getStringCellValue());

            var emptyModelSubheaderColumnRow = dtsSheet.getRow(TOP_MARGIN + 2);
            assertNotNull(emptyModelSubheaderColumnRow);
            var subheaderColumnCell = emptyModelSubheaderColumnRow.getCell(1);
            assertNotNull(subheaderColumnCell);
            assertEquals("result", subheaderColumnCell.getStringCellValue());

            var headerRow = dtsSheet.getRow(TOP_MARGIN + 6);
            assertNotNull(headerRow);
            var headerText = headerRow.getCell(1).getStringCellValue();
            assertEquals("Data Test getTest", headerText);

            var subheaderRow = dtsSheet.getRow(TOP_MARGIN + 7);
            assertNotNull(subheaderRow);

            var typeSbCell = subheaderRow.getCell(1);
            assertNotNull(typeSbCell);
            var typeSubheader = typeSbCell.getStringCellValue();
            assertEquals("type", typeSubheader);

            var sumSbCell = subheaderRow.getCell(2);
            assertNotNull(sumSbCell);
            var sumSubheader = sumSbCell.getStringCellValue();
            assertEquals("sum", sumSubheader);

            var registrationSbCell = subheaderRow.getCell(3);
            assertNotNull(registrationSbCell);
            var registrationDateSubheader = registrationSbCell.getStringCellValue();
            assertEquals("registrationDate", registrationDateSubheader);

            var isOkSbCell = subheaderRow.getCell(4);
            assertNotNull(isOkSbCell);
            var isOkSubheader = isOkSbCell.getStringCellValue();
            assertEquals("isOk", isOkSubheader);

            var driverSbCell = subheaderRow.getCell(5);
            assertNotNull(driverSbCell);
            var driverSubheader = driverSbCell.getStringCellValue();
            assertEquals("driver", driverSubheader);

            var columnHeaderRow = dtsSheet.getRow(TOP_MARGIN + 8);
            assertNotNull(columnHeaderRow);

            var typeColumnHeaderCell = columnHeaderRow.getCell(1);
            assertNotNull(typeColumnHeaderCell);
            var typeColumnHeader = typeColumnHeaderCell.getStringCellValue();
            assertEquals("Type", typeColumnHeader);

            var sumColumnHeaderCell = columnHeaderRow.getCell(2);
            assertNotNull(sumColumnHeaderCell);
            var sumColumnHeader = sumColumnHeaderCell.getStringCellValue();
            assertEquals("Sum", sumColumnHeader);

            var registrationColumnHeaderCell = columnHeaderRow.getCell(3);
            assertNotNull(registrationColumnHeaderCell);
            var registrationColumnHeader = registrationColumnHeaderCell.getStringCellValue();
            assertEquals("Registration Date", registrationColumnHeader);

            var isOkColumnHeaderCell = columnHeaderRow.getCell(4);
            assertNotNull(isOkColumnHeaderCell);
            var isOkColumnHeader = isOkColumnHeaderCell.getStringCellValue();
            assertEquals("Is Ok", isOkColumnHeader);

            var driverColumnHeaderCell = columnHeaderRow.getCell(5);
            assertNotNull(driverColumnHeaderCell);
            var driverColumnHeader = driverColumnHeaderCell.getStringCellValue();
            assertEquals("Driver", driverColumnHeader);

            var valueRow = dtsSheet.getRow(TOP_MARGIN + 9);
            assertNotNull(valueRow);

            var typeValueCell = valueRow.getCell(1);
            assertNotNull(typeValueCell);
            var typeValue = typeValueCell.getStringCellValue();
            assertEquals("Hello, World", typeValue);

            var sumValueCell = valueRow.getCell(2);
            assertNotNull(sumValueCell);
            var numericCellValue = sumValueCell.getNumericCellValue();
            assertEquals(0.0, numericCellValue, 1e-8);

            var registrationDateCell = valueRow.getCell(3);
            assertNotNull(registrationDateCell);
            var registrationTime = registrationDateCell.getDateCellValue();
            assertNotNull(registrationTime);

            var isOkCell = valueRow.getCell(4);
            assertNotNull(isOkCell);
            var isOk = isOkCell.getBooleanCellValue();
            assertTrue(isOk);

            var driverCell = valueRow.getCell(5);
            assertNotNull(driverCell);
            var driverValue = driverCell.getStringCellValue();
            assertTrue(StringUtils.isBlank(driverValue));

            var getMyModelRow = dtsSheet.getRow(TOP_MARGIN + 12);
            assertNotNull(getMyModelRow);
            var myModelHeaderText = getMyModelRow.getCell(1).getStringCellValue();
            assertEquals("Data Test getMyModel", myModelHeaderText);

            var myModelSubheaderRow = dtsSheet.getRow(TOP_MARGIN + 13);
            assertNotNull(myModelSubheaderRow);

            var typeMyModelSb = myModelSubheaderRow.getCell(1);
            assertNotNull(typeMyModelSb);
            var typeMyModelSubheader = typeMyModelSb.getStringCellValue();
            assertEquals("java_name", typeMyModelSubheader);

            var sumMyModelSb = myModelSubheaderRow.getCell(2);
            assertNotNull(sumMyModelSb);
            var sumMyModelSubheader = sumMyModelSb.getStringCellValue();
            assertEquals("height", sumMyModelSubheader);

            var isOkMyModelSb = myModelSubheaderRow.getCell(3);
            assertNotNull(isOkMyModelSb);
            var isOkMyModelSbText = isOkMyModelSb.getStringCellValue();
            assertEquals("isOk", isOkMyModelSbText);

            var columnMyModelHeaderRow = dtsSheet.getRow(TOP_MARGIN + 14);
            assertNotNull(columnMyModelHeaderRow);

            var javaNameMyModelColumnHeaderCell = columnMyModelHeaderRow.getCell(1);
            assertNotNull(javaNameMyModelColumnHeaderCell);
            var javaNameColumnHeader = javaNameMyModelColumnHeaderCell.getStringCellValue();
            assertEquals("Java _ Name", javaNameColumnHeader);

            var sumMyModelColumnHeaderCell = columnMyModelHeaderRow.getCell(2);
            assertNotNull(sumMyModelColumnHeaderCell);
            var sumMyModelColumnHeader = sumMyModelColumnHeaderCell.getStringCellValue();
            assertEquals("Height", sumMyModelColumnHeader);

            var isOkMyModelColumnHeaderCell = columnMyModelHeaderRow.getCell(3);
            assertNotNull(isOkMyModelColumnHeaderCell);
            var isOkMyModelColumnHeader = isOkMyModelColumnHeaderCell.getStringCellValue();
            assertEquals("Is Ok", isOkMyModelColumnHeader);

            var myModelValueRow = dtsSheet.getRow(TOP_MARGIN + 15);
            assertNotNull(myModelValueRow);

            var javaNameCell = myModelValueRow.getCell(1);
            assertNotNull(javaNameCell);
            var javaName = javaNameCell.getStringCellValue();
            assertEquals("object", javaName);

            var heightCell = myModelValueRow.getCell(2);
            assertNotNull(heightCell);
            var heightCellValue = heightCell.getNumericCellValue();
            assertEquals(134.44d, heightCellValue, 1e-8);

            var isOkCellMyModel = myModelValueRow.getCell(3);
            var myModelIsOk = isOkCellMyModel.getBooleanCellValue();
            assertFalse(myModelIsOk);

        }
    }

    @AfterAll
    static void clean() throws IOException {
        var dir = new File("../openl-excel-builder");
        var files = dir.listFiles();
        assertNotNull(files);
        for (File file : files) {
            if (file.getName().equals(DATA_TEST_PROJECT_NAME)) {
                Files.delete(file.toPath());
                break;
            }
        }
    }
}
