package org.openl.rules.excel.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.AfterClass;
import org.junit.Test;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.ProjectModel;

public class DatatypeTableExporterTest {

    public static final String TEST_PROJECT = "datatype_test_project";
    public static final int TOP_MARGIN = 2;
    public static final int DT_TYPE_CELL = 1;
    public static final int DT_NAME_CELL = 2;
    public static final int DT_DEFAULT_VALUE_CELL = 3;
    public static final String STRING_TYPE = "String";
    public static final String DATATYPE_TEST_PROJECT_NAME = "datatype_test_project.xlsx";

    @Test
    public void testDatatypeExport() throws IOException {
        DatatypeModel dt = new DatatypeModel("Test");

        FieldModel stringField = new FieldModel.Builder().setName("type")
            .setType(STRING_TYPE)
            .setDefaultValue("Hello, World")
            .build();

        FieldModel doubleField = new FieldModel.Builder().setName("sum")
            .setType("Double")
            .setDefaultValue(0.0d)
            .build();

        Date dateValue = new Date();
        FieldModel dateField = new FieldModel.Builder().setName("registrationDate")
            .setType("Date")
            .setDefaultValue(dateValue)
            .build();

        FieldModel booleanField = new FieldModel.Builder().setName("isOk")
            .setType("Boolean")
            .setDefaultValue(true)
            .build();

        FieldModel customTypeField = new FieldModel.Builder().setName("driver").setType("Human").build();

        dt.setFields(Arrays.asList(stringField, doubleField, dateField, booleanField, customTypeField));

        DatatypeModel oneMoreModel = new DatatypeModel("NextModel");
        FieldModel nextModelField = new FieldModel.Builder().setName("color")
            .setType(STRING_TYPE)
            .setDefaultValue("red")
            .build();
        oneMoreModel.setParent("Test");
        oneMoreModel.setFields(Collections.singletonList(nextModelField));

        ProjectModel projectModel = new ProjectModel(TEST_PROJECT,
            Arrays.asList(dt, oneMoreModel),
            Collections.emptyList());
        ExcelFileBuilder.generateProject(projectModel);

        try (XSSFWorkbook wb = new XSSFWorkbook(
            new FileInputStream("../openl-excel-builder/" + DATATYPE_TEST_PROJECT_NAME))) {
            XSSFSheet dtsSheet = wb.getSheet("Datatypes");
            assertNotNull(dtsSheet);
            XSSFRow headerRow = dtsSheet.getRow(TOP_MARGIN);
            assertNotNull(headerRow);
            String headerText = headerRow.getCell(1).getStringCellValue();
            assertEquals("Datatype Test", headerText);

            XSSFRow stringFieldRow = dtsSheet.getRow(TOP_MARGIN + 1);
            assertNotNull(stringFieldRow);
            XSSFCell dtCell = stringFieldRow.getCell(DT_TYPE_CELL);
            assertNotNull(dtCell);
            String typeCell = dtCell.getStringCellValue();
            XSSFCell nameCell = stringFieldRow.getCell(DT_NAME_CELL);
            assertNotNull(nameCell);
            String name = nameCell.getStringCellValue();
            XSSFCell dvCell = stringFieldRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(dvCell);
            String defaultValue = dvCell.getStringCellValue();
            assertEquals(STRING_TYPE, typeCell);
            assertEquals("type", name);
            assertEquals("Hello, World", defaultValue);

            XSSFRow doubleFieldRow = dtsSheet.getRow(TOP_MARGIN + 2);
            assertNotNull(doubleFieldRow);
            XSSFCell doubleTypeCell = doubleFieldRow.getCell(DT_TYPE_CELL);
            assertNotNull(doubleTypeCell);
            String doubleTypeCellValue = doubleTypeCell.getStringCellValue();
            XSSFCell doubleNameCell = doubleFieldRow.getCell(DT_NAME_CELL);
            assertNotNull(doubleNameCell);
            String doubleNameCellValue = doubleNameCell.getStringCellValue();
            XSSFCell doubleDefaultValueCell = doubleFieldRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(doubleDefaultValueCell);
            double doubleDefaultValue = doubleDefaultValueCell.getNumericCellValue();
            assertEquals("Double", doubleTypeCellValue);
            assertEquals("sum", doubleNameCellValue);
            assertEquals(0.0d, doubleDefaultValue, 1e-8);

            XSSFRow dateRow = dtsSheet.getRow(TOP_MARGIN + 3);
            assertNotNull(dateRow);
            XSSFCell dateTypeCell = dateRow.getCell(DT_TYPE_CELL);
            assertNotNull(dateTypeCell);
            String dateCellType = dateTypeCell.getStringCellValue();
            XSSFCell dateNameCell = dateRow.getCell(DT_NAME_CELL);
            assertNotNull(dateNameCell);
            String dateCellName = dateNameCell.getStringCellValue();
            XSSFCell dateDefaultValueCell = dateRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(dateDefaultValueCell);
            Date dateCellValue = dateDefaultValueCell.getDateCellValue();
            assertEquals("Date", dateCellType);
            assertEquals("registrationDate", dateCellName);
            assertEquals(dateValue, dateCellValue);

            XSSFRow booleanRow = dtsSheet.getRow(TOP_MARGIN + 4);
            assertNotNull(booleanRow);
            XSSFCell boolTypeCell = booleanRow.getCell(DT_TYPE_CELL);
            assertNotNull(boolTypeCell);
            String booleanCellType = boolTypeCell.getStringCellValue();
            XSSFCell boolNameCell = booleanRow.getCell(DT_NAME_CELL);
            assertNotNull(boolNameCell);
            String booleanCellName = boolNameCell.getStringCellValue();
            XSSFCell boolDefaultCell = booleanRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(boolDefaultCell);
            boolean booleanCellValue = boolDefaultCell.getBooleanCellValue();
            assertEquals("Boolean", booleanCellType);
            assertEquals("isOk", booleanCellName);
            assertTrue(booleanCellValue);

            XSSFRow customRow = dtsSheet.getRow(TOP_MARGIN + 5);
            assertNotNull(customRow);
            XSSFCell customTypeCell = customRow.getCell(DT_TYPE_CELL);
            assertNotNull(customTypeCell);
            String customCellType = customTypeCell.getStringCellValue();
            XSSFCell customNameCell = customRow.getCell(DT_NAME_CELL);
            assertNotNull(customNameCell);
            String customCellName = customNameCell.getStringCellValue();
            XSSFCell customDefaultValueCell = customRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(customDefaultValueCell);
            String customCellValue = customDefaultValueCell.getStringCellValue();
            assertEquals("Human", customCellType);
            assertEquals("driver", customCellName);
            assertEquals("", customCellValue);

            XSSFRow nextModelHeaderRow = dtsSheet.getRow(TOP_MARGIN + 8);
            assertNotNull(nextModelHeaderRow);
            XSSFCell nextModelHeaderCell = nextModelHeaderRow.getCell(1);
            assertNotNull(nextModelHeaderCell);
            assertEquals("Datatype NextModel extends Test", nextModelHeaderCell.getStringCellValue());
            XSSFRow nextModelRow = dtsSheet.getRow(TOP_MARGIN + 9);
            XSSFCell nextModelDtCell = nextModelRow.getCell(DT_TYPE_CELL);
            assertNotNull(nextModelDtCell);
            XSSFCell nextModelNameCell = nextModelRow.getCell(DT_NAME_CELL);
            assertNotNull(nextModelNameCell);
            XSSFCell nextModelDVCell = nextModelRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(nextModelDVCell);
            assertEquals(STRING_TYPE, nextModelDtCell.getStringCellValue());
            assertEquals("color", nextModelNameCell.getStringCellValue());
            assertEquals("red", nextModelDVCell.getStringCellValue());
        }

    }

    @Test
    public void writeDataTypes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DatatypeModel dt = new DatatypeModel("Test");

        FieldModel stringField = new FieldModel.Builder().setName("type")
            .setType(STRING_TYPE)
            .setDefaultValue("Hello, World")
            .build();

        FieldModel doubleField = new FieldModel.Builder().setName("sum")
            .setType("Double")
            .setDefaultValue(0.0d)
            .build();

        Date dateValue = new Date();
        FieldModel dateField = new FieldModel.Builder().setName("registrationDate")
            .setType("Date")
            .setDefaultValue(dateValue)
            .build();

        FieldModel booleanField = new FieldModel.Builder().setName("isOk")
            .setType("Boolean")
            .setDefaultValue(true)
            .build();

        FieldModel customTypeField = new FieldModel.Builder().setName("driver").setType("Human").build();

        dt.setFields(Arrays.asList(stringField, doubleField, dateField, booleanField, customTypeField));
        ExcelFileBuilder.generateDataTypes(Collections.singletonList(dt), bos);
        try (OutputStream fos = new FileOutputStream(DATATYPE_TEST_PROJECT_NAME)) {
            fos.write(bos.toByteArray());
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(
            new FileInputStream("../openl-excel-builder/" + DATATYPE_TEST_PROJECT_NAME))) {
            XSSFSheet dtsSheet = wb.getSheet("Datatypes");
            assertNotNull(dtsSheet);
        }
    }

    @AfterClass
    public static void clean() throws IOException {
        File dir = new File("../openl-excel-builder");
        File[] files = dir.listFiles();
        assertNotNull(files);
        for (File file : files) {
            if (file.getName().equals(DATATYPE_TEST_PROJECT_NAME)) {
                Files.delete(file.toPath());
                break;
            }
        }
    }
}
