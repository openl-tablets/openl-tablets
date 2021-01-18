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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

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

        FieldModel stringField = new FieldModel("type", STRING_TYPE, "Hello, World");
        FieldModel doubleField = new FieldModel("sum", "Double", 0.0d);
        FieldModel floatField = new FieldModel("weight", "Float", 1.3124124f);
        Date dateValue = new Date();
        FieldModel dateField = new FieldModel("registrationDate", "Date", dateValue);

        OffsetDateTime dateTimeValue = OffsetDateTime.now(ZoneId.systemDefault());
        FieldModel dateTimeField = new FieldModel("registrationDateTime", "Date", dateTimeValue);

        FieldModel booleanField = new FieldModel("isOk", "Boolean", true);

        FieldModel bigDecimalField = new FieldModel("bigNum",
            "BigDecimal",
            new BigDecimal("2975671681509007947508815"));

        FieldModel bigIntegerField = new FieldModel("bigInt", "BigInteger", BigInteger.TEN);
        FieldModel customTypeField = new FieldModel("driver", "Human");

        dt.setFields(Arrays.asList(stringField,
            doubleField,
            dateField,
            booleanField,
            customTypeField,
            dateTimeField,
            floatField,
            bigDecimalField,
            bigIntegerField));

        DatatypeModel oneMoreModel = new DatatypeModel("NextModel");
        FieldModel nextModelField = new FieldModel("color", STRING_TYPE, "red");
        oneMoreModel.setParent("Test");
        oneMoreModel.setFields(Collections.singletonList(nextModelField));

        ProjectModel projectModel = new ProjectModel(TEST_PROJECT,
            false,
            false,
            asSet(dt, oneMoreModel),
            Collections.emptyList(),
            Collections.emptyList(),
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

            XSSFRow dateTimeRow = dtsSheet.getRow(TOP_MARGIN + 6);
            assertNotNull(dateTimeRow);
            XSSFCell dateTimeCell = dateTimeRow.getCell(DT_TYPE_CELL);
            assertNotNull(dateTimeCell);
            String dateTimeCellType = dateTimeCell.getStringCellValue();
            XSSFCell dateTimeNameCell = dateTimeRow.getCell(DT_NAME_CELL);
            assertNotNull(dateTimeNameCell);
            String dateTimeCellName = dateTimeNameCell.getStringCellValue();
            XSSFCell dateTimeDefaultValueCell = dateTimeRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(dateTimeDefaultValueCell);
            OffsetDateTime offsetDateTime = dateTimeDefaultValueCell.getLocalDateTimeCellValue()
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime();
            assertEquals("Date", dateTimeCellType);
            assertEquals("registrationDateTime", dateTimeCellName);
            assertNotNull(offsetDateTime);

            XSSFRow floatFieldRow = dtsSheet.getRow(TOP_MARGIN + 7);
            assertNotNull(floatFieldRow);
            XSSFCell floatTypeCell = floatFieldRow.getCell(DT_TYPE_CELL);
            assertNotNull(floatTypeCell);
            String floatTypeCellValue = floatTypeCell.getStringCellValue();
            XSSFCell floatNameCell = floatFieldRow.getCell(DT_NAME_CELL);
            assertNotNull(floatNameCell);
            String floatNameCellValue = floatNameCell.getStringCellValue();
            XSSFCell floatDefaultValueCell = floatFieldRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(floatDefaultValueCell);
            double floatDefaultCell = floatDefaultValueCell.getNumericCellValue();
            assertEquals("Float", floatTypeCellValue);
            assertEquals("weight", floatNameCellValue);
            assertEquals(1.3124124, floatDefaultCell, 1e-8);

            XSSFRow bigDecimalRow = dtsSheet.getRow(TOP_MARGIN + 8);
            assertNotNull(bigDecimalRow);
            XSSFCell bigDecimalTypeCell = bigDecimalRow.getCell(DT_TYPE_CELL);
            assertNotNull(bigDecimalTypeCell);
            String bigDecimalTypeCellValue = bigDecimalTypeCell.getStringCellValue();
            XSSFCell bigDecimalNameCell = bigDecimalRow.getCell(DT_NAME_CELL);
            assertNotNull(bigDecimalNameCell);
            String bigDecimalNameCellValue = bigDecimalNameCell.getStringCellValue();
            XSSFCell bdDefaultValueCell = bigDecimalRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(bdDefaultValueCell);
            String bdDefValue = bdDefaultValueCell.getStringCellValue();
            assertEquals("BigDecimal", bigDecimalTypeCellValue);
            assertEquals("bigNum", bigDecimalNameCellValue);
            assertEquals("2975671681509007947508815", bdDefValue);

            XSSFRow bigIntegerRow = dtsSheet.getRow(TOP_MARGIN + 9);
            assertNotNull(bigIntegerRow);
            XSSFCell bigIntegerTypeCell = bigIntegerRow.getCell(DT_TYPE_CELL);
            assertNotNull(bigIntegerTypeCell);
            String bigIntegerTypeCellValue = bigIntegerTypeCell.getStringCellValue();
            XSSFCell bigIntegerNameCell = bigIntegerRow.getCell(DT_NAME_CELL);
            assertNotNull(bigIntegerNameCell);
            String bigIntegerNameCellValue = bigIntegerNameCell.getStringCellValue();
            XSSFCell biDefaultValueCell = bigIntegerRow.getCell(DT_DEFAULT_VALUE_CELL);
            assertNotNull(biDefaultValueCell);
            double biDefValue = biDefaultValueCell.getNumericCellValue();
            assertEquals("BigInteger", bigIntegerTypeCellValue);
            assertEquals("bigInt", bigIntegerNameCellValue);
            assertEquals(10.0, biDefValue, 1e-8);

            XSSFRow nextModelHeaderRow = dtsSheet.getRow(TOP_MARGIN + 12);
            assertNotNull(nextModelHeaderRow);
            XSSFCell nextModelHeaderCell = nextModelHeaderRow.getCell(1);
            assertNotNull(nextModelHeaderCell);
            assertEquals("Datatype NextModel extends Test", nextModelHeaderCell.getStringCellValue());
            XSSFRow nextModelRow = dtsSheet.getRow(TOP_MARGIN + 13);
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
        DatatypeModel dt = new DatatypeModel("Test");

        FieldModel stringField = new FieldModel("type", STRING_TYPE, "Hello, World");
        FieldModel doubleField = new FieldModel("sum", "Double", 0.0d);
        Date dateValue = new Date();
        FieldModel dateField = new FieldModel("registrationDate", "Date", dateValue);
        FieldModel booleanField = new FieldModel("isOk", "Boolean", true);
        FieldModel customTypeField = new FieldModel("driver", "Human");
        dt.setFields(Arrays.asList(stringField, doubleField, dateField, booleanField, customTypeField));

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ExcelFileBuilder.generateDataTypes(asSet(dt), bos);
            try (OutputStream fos = new FileOutputStream(DATATYPE_TEST_PROJECT_NAME)) {
                fos.write(bos.toByteArray());
            }
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

    @SafeVarargs
    private static <T> Set<T> asSet(T... args) {
        return new LinkedHashSet<>(Arrays.asList(args));
    }
}
