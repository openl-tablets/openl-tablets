package org.openl.rules.excel.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.rules.excel.builder.SheetCells.cell;
import static org.openl.rules.excel.builder.SheetCells.columnTexts;
import static org.openl.rules.excel.builder.SheetCells.rowTexts;
import static org.openl.rules.excel.builder.SheetCells.text;
import static org.openl.rules.excel.builder.export.DataTableExporter.DATA_SHEET;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
        noFieldsModel.setFields(List.of(new FieldModel("this", "Double")));
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

            assertEquals(List.of("Data Object emptyDataTAble", "this", "result"),
                    columnTexts(dtsSheet, TOP_MARGIN, TOP_MARGIN + 2, 1));

            assertEquals("Data Test getTest", text(dtsSheet, TOP_MARGIN + 6, 1));
            assertEquals(List.of("type", "sum", "registrationDate", "isOk", "driver"),
                    rowTexts(dtsSheet, TOP_MARGIN + 7, 1, 5));
            assertEquals(List.of("Type", "Sum", "Registration Date", "Is Ok", "Driver"),
                    rowTexts(dtsSheet, TOP_MARGIN + 8, 1, 5));
            var valueRow = TOP_MARGIN + 9;
            assertEquals("Hello, World", text(dtsSheet, valueRow, 1));
            assertEquals(0.0, cell(dtsSheet, valueRow, 2).getNumericCellValue(), 1e-8);
            assertNotNull(cell(dtsSheet, valueRow, 3).getDateCellValue());
            assertTrue(cell(dtsSheet, valueRow, 4).getBooleanCellValue());
            assertTrue(StringUtils.isBlank(text(dtsSheet, valueRow, 5)));

            assertEquals("Data Test getMyModel", text(dtsSheet, TOP_MARGIN + 12, 1));
            assertEquals(List.of("java_name", "height", "isOk"), rowTexts(dtsSheet, TOP_MARGIN + 13, 1, 3));
            assertEquals(List.of("Java _ Name", "Height", "Is Ok"), rowTexts(dtsSheet, TOP_MARGIN + 14, 1, 3));
            var myModelValueRow = TOP_MARGIN + 15;
            assertEquals("object", text(dtsSheet, myModelValueRow, 1));
            assertEquals(134.44d, cell(dtsSheet, myModelValueRow, 2).getNumericCellValue(), 1e-8);
            assertFalse(cell(dtsSheet, myModelValueRow, 3).getBooleanCellValue());
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
