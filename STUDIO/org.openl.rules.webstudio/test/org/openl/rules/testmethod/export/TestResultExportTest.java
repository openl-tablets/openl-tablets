package org.openl.rules.testmethod.export;

import static org.junit.Assert.*;
import static org.openl.rules.testmethod.export.Styles.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IOpenClass;
import org.openl.util.NumberUtils;

public class TestResultExportTest {
    /**
     * Date format used to convert Date to String in this test
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TRIVIAL_PROJECT = "test-resources/test/export/trivial";

    private static TestUnitsResults[] testResults;
    private static TestUnitsResults[] trivialResults;
    private static TestUnitsResults[] resultsWithPK;

    @BeforeClass
    public static void runTests() throws Exception {
        testResults = runTests("test-resources/test/export/example3");
        trivialResults = runTests(TRIVIAL_PROJECT);
        resultsWithPK = runTests("test-resources/test/export/example3-pk");
    }

    @AfterClass
    public static void cleanUp() {
        testResults = null;
        trivialResults = null;
    }

    private static TestUnitsResults[] runTests(String path) throws Exception {
        SimpleProjectEngineFactory<?> factory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
            .setProject(path)
            .setExecutionMode(false)
            .build();

        CompiledOpenClass openLRules = factory.getCompiledOpenClass();
        IOpenClass openClass = openLRules.getOpenClassWithErrors();
        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);

        TestUnitsResults[] results = new TestUnitsResults[tests.length];
        for (int i = 0; i < tests.length; i++) {
            TestSuiteMethod test = tests[i];
            results[i] = new TestSuite(test).invokeSequentially(openClass, 1L);
        }

        // Tests can appear in a random order. For testing convenience sort them alphabetically
        Arrays.sort(results, new Comparator<TestUnitsResults>() {
            @Override
            public int compare(TestUnitsResults o1, TestUnitsResults o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return results;
    }

    private static TestUnitsResults runTest(String path, String testName, int... indices) throws Exception {
        SimpleProjectEngineFactory<?> factory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
            .setProject(path)
            .setExecutionMode(false)
            .build();
        CompiledOpenClass openLRules = factory.getCompiledOpenClass();
        IOpenClass openClass = openLRules.getOpenClassWithErrors();
        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
        for (TestSuiteMethod test : tests) {
            if (test.getName().equals(testName)) {
                return new TestSuite(test, indices).invokeSequentially(openClass, 1L);
            }
        }

        throw new IllegalArgumentException("Test '" + testName + "' is not found");
    }

    @Test
    public void allResultsInFirstPage() throws Exception {
        File xlsx;
        try (TempFileExporter export = new TempFileExporter()) {
            xlsx = export.createExcelFile(testResults, -1);
            assertTrue(xlsx.exists());

            try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
                assertEquals(2, workbook.getNumberOfSheets());
                assertNotNull(workbook.getSheet("Parameters 1"));

                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowNum = BaseExport.FIRST_ROW;
                rowNum = checkDriverPremiumTest(sheet, rowNum);

                rowNum += BaseExport.SPACE_BETWEEN_RESULTS + 1;
                rowNum = checkPolicyPremiumTest(sheet, rowNum);

                rowNum += BaseExport.SPACE_BETWEEN_RESULTS + 1;
                rowNum = checkVehiclePremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());
            }
        }

        assertFalse(xlsx.exists());
    }

    @Test
    public void oneResultPerPage() throws Exception {
        File xlsx;
        try (TempFileExporter export = new TempFileExporter()) {
            xlsx = export.createExcelFile(testResults, 1);
            assertTrue(xlsx.exists());

            try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
                assertEquals(6, workbook.getNumberOfSheets());
                assertNotNull(workbook.getSheet("Parameters 1"));
                assertNotNull(workbook.getSheet("Parameters 2"));
                assertNotNull(workbook.getSheet("Parameters 3"));

                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowNum = BaseExport.FIRST_ROW;
                rowNum = checkDriverPremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());

                sheet = workbook.getSheetAt(1);
                rowNum = BaseExport.FIRST_ROW;
                rowNum = checkPolicyPremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());

                sheet = workbook.getSheetAt(2);
                rowNum = BaseExport.FIRST_ROW;
                rowNum = checkVehiclePremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());
            }
        }

        assertFalse(xlsx.exists());
    }

    @Test
    public void testTrivialParameters() throws Exception {
        File xlsx;
        try (TempFileExporter export = new TempFileExporter()) {
            xlsx = export.createExcelFile(trivialResults, -1);
            assertTrue(xlsx.exists());

            try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
                assertEquals(2, workbook.getNumberOfSheets());
                assertNotNull(workbook.getSheet("Result 1"));
                assertNotNull(workbook.getSheet("Parameters 1"));

                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowNum = BaseExport.FIRST_ROW;
                assertRowText(sheet.getRow(rowNum), "HelloTest");

                rowNum++;
                assertRowText(sheet.getRow(rowNum), "2 test cases (1 failed)");

                rowNum += 2;
                XSSFRow row = sheet.getRow(rowNum);
                assertRowText(row, "ID", "Status", "Hour", "Result");
                assertRowColors(row, HEADER, HEADER, HEADER, HEADER);
                assertComments(row, 3, (String) null);

                row = sheet.getRow(++rowNum);
                assertRowText(row, "1", "Failed", "5", "Good Morning");
                assertRowColors(row, RED_MAIN, RED_MAIN, null, RED_FIELDS);
                assertComments(row, 3, "Expected: aaa");

                row = sheet.getRow(++rowNum);
                assertRowText(row, "2", "Passed", "22", "Good Night");
                assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS);
                assertComments(row, 3, (String) null);

                assertEquals(rowNum, sheet.getLastRowNum());
            }
        }

        assertFalse(xlsx.exists());
    }

    @Test
    public void testOneTestCase() throws Exception {
        File xlsx;
        TestUnitsResults singleTestCase = runTest(TRIVIAL_PROJECT, "HelloTest", 0);

        try (TempFileExporter export = new TempFileExporter()) {
            xlsx = export.createExcelFile(new TestUnitsResults[] { singleTestCase }, -1);
            assertTrue(xlsx.exists());

            try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
                assertEquals(2, workbook.getNumberOfSheets());

                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowNum = BaseExport.FIRST_ROW;
                assertRowText(sheet.getRow(rowNum), "HelloTest");

                rowNum++;
                assertRowText(sheet.getRow(rowNum), "1 test case (1 failed)");
            }
        }

        assertFalse(xlsx.exists());

        singleTestCase = runTest(TRIVIAL_PROJECT, "HelloTest", 1);
        try (TempFileExporter export = new TempFileExporter()) {
            xlsx = export.createExcelFile(new TestUnitsResults[] { singleTestCase }, -1);
            assertTrue(xlsx.exists());

            try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
                assertEquals(2, workbook.getNumberOfSheets());
                assertNotNull(workbook.getSheet("Parameters 1"));

                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowNum = BaseExport.FIRST_ROW;
                assertRowText(sheet.getRow(rowNum), "HelloTest");

                rowNum++;
                assertRowText(sheet.getRow(rowNum), "1 test case");
            }
        }

        assertFalse(xlsx.exists());
    }

    @Test
    public void testParametersWithPrimaryKey() throws Exception {
        File xlsx;
        try (TempFileExporter export = new TempFileExporter()) {
            xlsx = export.createExcelFile(resultsWithPK, -1);
            assertTrue(xlsx.exists());

            try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
                assertEquals(2, workbook.getNumberOfSheets());
                assertNotNull(workbook.getSheet("Parameters 1"));

                // Test the case when parameter is referenced by primary key
                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowNum = BaseExport.FIRST_ROW;
                assertRowText(sheet.getRow(rowNum), "DriverPremiumTest1");

                rowNum++;
                assertRowText(sheet.getRow(rowNum), "3 test cases (1 failed)");

                rowNum += 2;
                XSSFRow row = sheet.getRow(rowNum);
                assertRowText(row,
                    "ID",
                    "Status",
                    "Driver",
                    "Expected Age Type",
                    "Expected Eligibility",
                    "Expected Risk");
                assertRowColors(row, HEADER, HEADER, HEADER, HEADER, HEADER, HEADER);

                row = sheet.getRow(++rowNum);
                assertRowText(row, "1", "Passed", "a1", "Standard Driver", "Eligible", "Standard Risk Driver");
                assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);
                assertComments(row, 3, null, null, null);

                row = sheet.getRow(++rowNum);
                assertRowText(row, "2", "Failed", "b2", "Young Driver", "Eligible", "Standard Risk Driver");
                assertRowColors(row, RED_MAIN, RED_MAIN, null, GREEN_FIELDS, RED_FIELDS, GREEN_FIELDS);
                assertComments(row, 3, null, "Expected: Provisional", null);

                row = sheet.getRow(++rowNum);
                assertRowText(row, "3", "Passed", "c3", "Young Driver", "Not Eligible", "High Risk Driver");
                assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);
                assertComments(row, 3, null, null, null);

                // Test the case when parameter is referenced by field name despite that data table is with primary key
                rowNum += BaseExport.SPACE_BETWEEN_RESULTS + 1;
                assertRowText(sheet.getRow(rowNum), "DriverPremiumTest2");

                rowNum++;
                assertRowText(sheet.getRow(rowNum), "3 test cases (1 failed)");

                rowNum += 2;
                row = sheet.getRow(rowNum);
                assertRowText(row,
                    "ID",
                    "Status",
                    "Driver",
                    "Expected Age Type",
                    "Expected Eligibility",
                    "Expected Risk");
                assertRowColors(row, HEADER, HEADER, HEADER, HEADER, HEADER, HEADER);

                row = sheet.getRow(++rowNum);
                assertRowText(row, "1", "Passed", "Sara", "Standard Driver", "Eligible", "Standard Risk Driver");
                assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);
                assertComments(row, 3, null, null, null);

                row = sheet.getRow(++rowNum);
                assertRowText(row,
                    "2",
                    "Failed",
                    "Spencer, Sara's Son",
                    "Young Driver",
                    "Eligible",
                    "Standard Risk Driver");
                assertRowColors(row, RED_MAIN, RED_MAIN, null, GREEN_FIELDS, RED_FIELDS, GREEN_FIELDS);
                assertComments(row, 3, null, "Expected: Provisional", null);

                row = sheet.getRow(++rowNum);
                assertRowText(row,
                    "3",
                    "Passed",
                    "Spencer, No Training",
                    "Young Driver",
                    "Not Eligible",
                    "High Risk Driver");
                assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);
                assertComments(row, 3, null, null, null);

                assertEquals(rowNum, sheet.getLastRowNum());
            }
        }

        assertFalse(xlsx.exists());
    }

    @Test
    public void twoResultsPerPage() throws Exception {
        File xlsx;
        try (TempFileExporter export = new TempFileExporter()) {
            xlsx = export.createExcelFile(testResults, 2);
            assertTrue(xlsx.exists());

            try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
                assertEquals(4, workbook.getNumberOfSheets());
                assertNotNull(workbook.getSheet("Parameters 1"));
                assertNotNull(workbook.getSheet("Parameters 2"));

                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowNum = BaseExport.FIRST_ROW;
                rowNum = checkDriverPremiumTest(sheet, rowNum);

                rowNum += BaseExport.SPACE_BETWEEN_RESULTS + 1;
                rowNum = checkPolicyPremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());

                sheet = workbook.getSheetAt(1);
                rowNum = BaseExport.FIRST_ROW;
                rowNum = checkVehiclePremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());
            }
        }

        assertFalse(xlsx.exists());
    }

    @Test
    public void partialObjectInitializationUsedInPrimaryKey() throws Exception {
        File xlsx;
        TestUnitsResults[] results = runTests("test-resources/test/export/EPBDS-7147-partial-object-initialization");

        try (TempFileExporter export = new TempFileExporter()) {
            xlsx = export.createExcelFile(results, -1);
            assertTrue(xlsx.exists());

            try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
                assertEquals(2, workbook.getNumberOfSheets());
                assertNotNull(workbook.getSheet("Parameters 1"));

                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowNum = BaseExport.FIRST_ROW;
                assertRowText(sheet.getRow(rowNum), "TestDataDReturnTest7");

                rowNum++;
                assertRowText(sheet.getRow(rowNum), "3 test cases");

                rowNum += 2;
                XSSFRow row = sheet.getRow(rowNum);
                assertRowText(row, "ID", "Status", "obj", "Result field 2");
                assertRowColors(row, HEADER, HEADER, HEADER, HEADER);

                row = sheet.getRow(++rowNum);
                assertRowText(row, "1", "Passed", "MyObjectD{ field1=2.0 field2=null }", "2");
                assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS);
                assertComments(row, 3, (String) null);

                row = sheet.getRow(++rowNum);
                assertRowText(row, "2", "Passed", "MyObjectD{ field1=4.0 field2=null }", "4");
                assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS);
                assertComments(row, 3, (String) null);

                row = sheet.getRow(++rowNum);
                assertRowText(row, "3", "Passed", "MyObjectD{ field1=5.0 field2=null }", "5");
                assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS);
                assertComments(row, 3, (String) null);
            }
        }

        assertFalse(xlsx.exists());
    }

    private int checkDriverPremiumTest(XSSFSheet sheet, int rowNum) {
        assertRowText(sheet.getRow(rowNum), "DriverPremiumTest");

        rowNum++;
        assertRowText(sheet.getRow(rowNum), "3 test cases (1 failed)");

        rowNum += 2;
        XSSFRow row = sheet.getRow(rowNum);
        assertRowText(row, "ID", "Status", "Driver", "Expected Age Type", "Expected Eligibility", "Expected Risk");
        assertRowColors(row, HEADER, HEADER, HEADER, HEADER, HEADER, HEADER);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "1", "Passed", "Sara", "Standard Driver", "Eligible", "Standard Risk Driver");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);
        assertComments(row, 3, null, null, null);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "2", "Failed", "Spencer, Sara's Son", "Young Driver", "Eligible", "Standard Risk Driver");
        assertRowColors(row, RED_MAIN, RED_MAIN, null, GREEN_FIELDS, RED_FIELDS, GREEN_FIELDS);
        assertComments(row, 3, null, "Expected: Provisional", null);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "3", "Passed", "Spencer, No Training", "Young Driver", "Not Eligible", "High Risk Driver");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);
        assertComments(row, 3, null, null, null);

        return rowNum;
    }

    private int checkPolicyPremiumTest(XSSFSheet sheet, int rowNum) {
        assertRowText(sheet.getRow(rowNum), "PolicyPremiumTest");

        rowNum++;
        assertRowText(sheet.getRow(rowNum), "2 test cases");

        rowNum += 2;
        XSSFRow row = sheet.getRow(rowNum);
        assertRowText(row,
            "ID",
            "Status",
            "Description",
            "Name of Policy",
            "Expected Score",
            "Expected Eligibility",
            "Expected Premium");
        assertRowColors(row, HEADER, HEADER, HEADER, HEADER, HEADER, HEADER);
        assertComments(row, 4, null, null, null);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "case1", "Passed", "Test Policy1", "Policy1", "0", "Eligible", "922.5");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);
        assertComments(row, 4, null, null, null);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "case2", "Passed", "Test Second policy", "Policy2", "110", "Eligible", "2960");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);
        assertComments(row, 4, null, null, null);

        return rowNum;
    }

    private int checkVehiclePremiumTest(XSSFSheet sheet, int rowNum) {
        assertRowText(sheet.getRow(rowNum), "VehiclePremiumTest");

        rowNum++;
        assertRowText(sheet.getRow(rowNum), "3 test cases");

        rowNum += 2;
        XSSFRow row = sheet.getRow(rowNum);
        assertRowText(row,
            "ID",
            "Status",
            "Car",
            "Expected Theft Rating",
            "Expected Injury Rating",
            "Expected Eligibility",
            "Created date");
        assertRowColors(row, HEADER, HEADER, HEADER, HEADER, HEADER, HEADER, HEADER);
        assertComments(row, 3, null, null, null, null);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "1", "Passed", "2005 Honda Odyssey", "Moderate", "Low", "Eligible", "2017-01-02");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);
        assertComments(row, 3, null, null, null, null);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "2", "Passed", "2002 Toyota Camry", "Low", "Moderate", "Eligible", "2017-01-02");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);
        assertComments(row, 3, null, null, null, null);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "3", "Passed", "1965 VW Bug", "High", "Extremely High", "Not Eligible", "2017-01-02");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);
        assertComments(row, 3, null, null, null, null);

        return rowNum;
    }

    private void assertRowText(XSSFRow row, String... values) {
        int column = BaseExport.FIRST_COLUMN;

        String sheetName = row.getSheet().getSheetName();
        for (String value : values) {
            String message = "Incorrect text in: {" + sheetName + "[" + row.getRowNum() + ", " + column + "]}";
            assertEquals(message, value, asString(row.getCell(column)));
            column++;
        }

        String message = "There are extra cells in row " + row.getRowNum() + " of sheet " + sheetName;
        assertEquals(message, column, row.getLastCellNum());
    }

    private void assertRowColors(XSSFRow row, Integer... colors) {
        int column = BaseExport.FIRST_COLUMN;

        XSSFSheet sheet = row.getSheet();
        String sheetName = sheet.getSheetName();
        XSSFWorkbook workbook = sheet.getWorkbook();
        IndexedColorMap indexedColors = workbook.getStylesSource().getIndexedColors();
        for (Integer color : colors) {
            XSSFColor expected = color == null ? null : new XSSFColor(convertRGB(color), indexedColors);
            String message = "Incorrect color in: {" + sheetName + "[" + row.getRowNum() + ", " + column + "]}";
            assertEquals(message, expected, row.getCell(column).getCellStyle().getFillForegroundColorColor());
            column++;
        }
    }

    private void assertComments(XSSFRow row, int firstResultColumn, String... expectedTexts) {
        int columnNum = firstResultColumn;
        for (String expectedText : expectedTexts) {
            XSSFComment comment = row.getCell(BaseExport.FIRST_COLUMN + columnNum).getCellComment();

            if (expectedText == null) {
                assertNull(comment);
            } else {
                assertNotNull(comment);
                assertEquals(expectedText, comment.getString().getString());
            }

            columnNum++;
        }
    }

    @SuppressWarnings("deprecation")
    private String asString(Cell cell) {
        switch (cell.getCellType()) {
            case BLANK:
                return null;
            case BOOLEAN:
                return "" + cell.getBooleanCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat(DATE_FORMAT).format(cell.getDateCellValue());
                }
                return "" + NumberUtils.intOrDouble(cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue();
            default:
                throw new UnsupportedOperationException();
        }
    }

    static class TempFileExporter implements AutoCloseable {

        File tempFile;

        File createExcelFile(TestUnitsResults[] results, int testsPerPage) throws IOException {
            tempFile = File.createTempFile("test-results", ".xlsx");
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                new TestResultExport().export(outputStream, testsPerPage, results);
            }
            return tempFile;
        }

        @Override
        public void close() throws Exception {
            if (!tempFile.delete()) {
                throw new IOException("File " + tempFile + " is not deleted. Possibly it's locked.");
            }
        }
    }
}
