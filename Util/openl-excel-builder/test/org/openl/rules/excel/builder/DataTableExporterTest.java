package org.openl.rules.excel.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.excel.builder.export.DataTableExporter.DATA_SHEET;

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
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.util.StringUtils;

public class DataTableExporterTest {

    public static final String STRING_TYPE = "String";
    public static final String DATA_TEST_PROJECT_NAME = "data_test_project.xlsx";
    public static final int TOP_MARGIN = 2;

    @Test
    public void writeDataTables() throws IOException {
        DatatypeModel noFieldsModel = new DatatypeModel("NoFieldsModel");
        noFieldsModel.setFields(Collections.singletonList(new FieldModel("this", "Double")));
        DataModel emptyModel = new DataModel("emptyDataTAble", "Object", null, noFieldsModel);

        DatatypeModel dt = new DatatypeModel("Test");

        FieldModel stringField = new FieldModel("type", STRING_TYPE, "Hello, World");
        FieldModel doubleField = new FieldModel("sum", "Double", 0.0d);
        Date dateValue = new Date();
        FieldModel dateField = new FieldModel("registrationDate", "Date", dateValue);
        FieldModel booleanField = new FieldModel("isOk", "Boolean", true);
        FieldModel customTypeField = new FieldModel("driver", "Human");
        dt.setFields(Arrays.asList(stringField, doubleField, dateField, booleanField, customTypeField));
        PathInfo info = new PathInfo("/getTest",
            "/getTest",
            PathInfo.Operation.GET,
            new TypeInfo("Test", "Test", TypeInfo.Type.DATATYPE),
            "application/json",
            "application/json");
        DataModel dm = new DataModel("getTest", "Test", info, dt);

        DatatypeModel secondModel = new DatatypeModel("MyModel");

        FieldModel integerField = new FieldModel("java_name", "String", "object");
        FieldModel sumField = new FieldModel("height", "Double", 134.44d);
        FieldModel isOkField = new FieldModel("isOk", "Boolean", false);
        secondModel.setFields(Arrays.asList(integerField, sumField, isOkField));
        PathInfo infoForNotOk = new PathInfo("/getMyModel",
            "/my/model",
            PathInfo.Operation.POST,
            new TypeInfo("Unknown", "Unknown", TypeInfo.Type.DATATYPE),
            "text/plain",
            "text/html");
        DataModel myModel = new DataModel("getMyModel", "Test", infoForNotOk, secondModel);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateDataTables(Arrays.asList(emptyModel, dm, myModel), bos);
            try (OutputStream fos = new FileOutputStream(DATA_TEST_PROJECT_NAME)) {
                fos.write(bos.toByteArray());
            }
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(
            new FileInputStream("../openl-excel-builder/" + DATA_TEST_PROJECT_NAME))) {
            XSSFSheet dtsSheet = wb.getSheet(DATA_SHEET);
            assertNotNull(dtsSheet);

            XSSFRow emptyModelRow = dtsSheet.getRow(TOP_MARGIN);
            assertNotNull(emptyModelRow);
            XSSFCell headerCell = emptyModelRow.getCell(1);
            assertNotNull(headerCell);
            String emptyModelHeaderText = headerCell.getStringCellValue();
            assertEquals("Data Object emptyDataTAble", emptyModelHeaderText);

            XSSFRow emptyModelSubheaderRow = dtsSheet.getRow(TOP_MARGIN + 1);
            assertNotNull(emptyModelSubheaderRow);
            XSSFCell subheaderCell = emptyModelSubheaderRow.getCell(1);
            assertNotNull(subheaderCell);
            assertEquals("this", subheaderCell.getStringCellValue());

            XSSFRow emptyModelSubheaderColumnRow = dtsSheet.getRow(TOP_MARGIN + 2);
            assertNotNull(emptyModelSubheaderColumnRow);
            XSSFCell subheaderColumnCell = emptyModelSubheaderColumnRow.getCell(1);
            assertNotNull(subheaderColumnCell);
            assertEquals("result", subheaderColumnCell.getStringCellValue());

            XSSFRow headerRow = dtsSheet.getRow(TOP_MARGIN + 6);
            assertNotNull(headerRow);
            String headerText = headerRow.getCell(1).getStringCellValue();
            assertEquals("Data Test getTest", headerText);

            XSSFRow subheaderRow = dtsSheet.getRow(TOP_MARGIN + 7);
            assertNotNull(subheaderRow);

            XSSFCell typeSbCell = subheaderRow.getCell(1);
            assertNotNull(typeSbCell);
            String typeSubheader = typeSbCell.getStringCellValue();
            assertEquals("type", typeSubheader);

            XSSFCell sumSbCell = subheaderRow.getCell(2);
            assertNotNull(sumSbCell);
            String sumSubheader = sumSbCell.getStringCellValue();
            assertEquals("sum", sumSubheader);

            XSSFCell registrationSbCell = subheaderRow.getCell(3);
            assertNotNull(registrationSbCell);
            String registrationDateSubheader = registrationSbCell.getStringCellValue();
            assertEquals("registrationDate", registrationDateSubheader);

            XSSFCell isOkSbCell = subheaderRow.getCell(4);
            assertNotNull(isOkSbCell);
            String isOkSubheader = isOkSbCell.getStringCellValue();
            assertEquals("isOk", isOkSubheader);

            XSSFCell driverSbCell = subheaderRow.getCell(5);
            assertNotNull(driverSbCell);
            String driverSubheader = driverSbCell.getStringCellValue();
            assertEquals("driver", driverSubheader);

            XSSFRow columnHeaderRow = dtsSheet.getRow(TOP_MARGIN + 8);
            assertNotNull(columnHeaderRow);

            XSSFCell typeColumnHeaderCell = columnHeaderRow.getCell(1);
            assertNotNull(typeColumnHeaderCell);
            String typeColumnHeader = typeColumnHeaderCell.getStringCellValue();
            assertEquals("Type", typeColumnHeader);

            XSSFCell sumColumnHeaderCell = columnHeaderRow.getCell(2);
            assertNotNull(sumColumnHeaderCell);
            String sumColumnHeader = sumColumnHeaderCell.getStringCellValue();
            assertEquals("Sum", sumColumnHeader);

            XSSFCell registrationColumnHeaderCell = columnHeaderRow.getCell(3);
            assertNotNull(registrationColumnHeaderCell);
            String registrationColumnHeader = registrationColumnHeaderCell.getStringCellValue();
            assertEquals("Registration Date", registrationColumnHeader);

            XSSFCell isOkColumnHeaderCell = columnHeaderRow.getCell(4);
            assertNotNull(isOkColumnHeaderCell);
            String isOkColumnHeader = isOkColumnHeaderCell.getStringCellValue();
            assertEquals("Is Ok", isOkColumnHeader);

            XSSFCell driverColumnHeaderCell = columnHeaderRow.getCell(5);
            assertNotNull(driverColumnHeaderCell);
            String driverColumnHeader = driverColumnHeaderCell.getStringCellValue();
            assertEquals("Driver", driverColumnHeader);

            XSSFRow valueRow = dtsSheet.getRow(TOP_MARGIN + 9);
            assertNotNull(valueRow);

            XSSFCell typeValueCell = valueRow.getCell(1);
            assertNotNull(typeValueCell);
            String typeValue = typeValueCell.getStringCellValue();
            assertEquals("Hello, World", typeValue);

            XSSFCell sumValueCell = valueRow.getCell(2);
            assertNotNull(sumValueCell);
            double numericCellValue = sumValueCell.getNumericCellValue();
            assertEquals(0.0, numericCellValue, 1e-8);

            XSSFCell registrationDateCell = valueRow.getCell(3);
            assertNotNull(registrationDateCell);
            Date registrationTime = registrationDateCell.getDateCellValue();
            assertNotNull(registrationTime);

            XSSFCell isOkCell = valueRow.getCell(4);
            assertNotNull(isOkCell);
            boolean isOk = isOkCell.getBooleanCellValue();
            assertTrue(isOk);

            XSSFCell driverCell = valueRow.getCell(5);
            assertNotNull(driverCell);
            String driverValue = driverCell.getStringCellValue();
            assertTrue(StringUtils.isBlank(driverValue));

            XSSFRow getMyModelRow = dtsSheet.getRow(TOP_MARGIN + 12);
            assertNotNull(getMyModelRow);
            String myModelHeaderText = getMyModelRow.getCell(1).getStringCellValue();
            assertEquals("Data Test getMyModel", myModelHeaderText);

            XSSFRow myModelSubheaderRow = dtsSheet.getRow(TOP_MARGIN + 13);
            assertNotNull(myModelSubheaderRow);

            XSSFCell typeMyModelSb = myModelSubheaderRow.getCell(1);
            assertNotNull(typeMyModelSb);
            String typeMyModelSubheader = typeMyModelSb.getStringCellValue();
            assertEquals("java_name", typeMyModelSubheader);

            XSSFCell sumMyModelSb = myModelSubheaderRow.getCell(2);
            assertNotNull(sumMyModelSb);
            String sumMyModelSubheader = sumMyModelSb.getStringCellValue();
            assertEquals("height", sumMyModelSubheader);

            XSSFCell isOkMyModelSb = myModelSubheaderRow.getCell(3);
            assertNotNull(isOkMyModelSb);
            String isOkMyModelSbText = isOkMyModelSb.getStringCellValue();
            assertEquals("isOk", isOkMyModelSbText);

            XSSFRow columnMyModelHeaderRow = dtsSheet.getRow(TOP_MARGIN + 14);
            assertNotNull(columnMyModelHeaderRow);

            XSSFCell javaNameMyModelColumnHeaderCell = columnMyModelHeaderRow.getCell(1);
            assertNotNull(javaNameMyModelColumnHeaderCell);
            String javaNameColumnHeader = javaNameMyModelColumnHeaderCell.getStringCellValue();
            assertEquals("Java _ Name", javaNameColumnHeader);

            XSSFCell sumMyModelColumnHeaderCell = columnMyModelHeaderRow.getCell(2);
            assertNotNull(sumMyModelColumnHeaderCell);
            String sumMyModelColumnHeader = sumMyModelColumnHeaderCell.getStringCellValue();
            assertEquals("Height", sumMyModelColumnHeader);

            XSSFCell isOkMyModelColumnHeaderCell = columnMyModelHeaderRow.getCell(3);
            assertNotNull(isOkMyModelColumnHeaderCell);
            String isOkMyModelColumnHeader = isOkMyModelColumnHeaderCell.getStringCellValue();
            assertEquals("Is Ok", isOkMyModelColumnHeader);

            XSSFRow myModelValueRow = dtsSheet.getRow(TOP_MARGIN + 15);
            assertNotNull(myModelValueRow);

            XSSFCell javaNameCell = myModelValueRow.getCell(1);
            assertNotNull(javaNameCell);
            String javaName = javaNameCell.getStringCellValue();
            assertEquals("object", javaName);

            XSSFCell heightCell = myModelValueRow.getCell(2);
            assertNotNull(heightCell);
            double heightCellValue = heightCell.getNumericCellValue();
            assertEquals(134.44d, heightCellValue, 1e-8);

            XSSFCell isOkCellMyModel = myModelValueRow.getCell(3);
            boolean myModelIsOk = isOkCellMyModel.getBooleanCellValue();
            assertFalse(myModelIsOk);

        }
    }

    @AfterClass
    public static void clean() throws IOException {
        File dir = new File("../openl-excel-builder");
        File[] files = dir.listFiles();
        assertNotNull(files);
        for (File file : files) {
            if (file.getName().equals(DATA_TEST_PROJECT_NAME)) {
                Files.delete(file.toPath());
                break;
            }
        }
    }
}
