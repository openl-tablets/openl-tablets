package org.openl.rules.webstudio.web.test;

import static org.junit.Assert.*;
import static org.openl.rules.webstudio.web.test.TestResultExport.Styles.*;

import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestMethodNodeBinder;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IOpenClass;
import org.openl.util.NumberUtils;

public class TestResultExportTest {
    /**
     * Date format used to convert Date to String in this test
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static TestUnitsResults[] testResults;

    @BeforeClass
    public static void runTests() throws
                            org.openl.rules.project.instantiation.RulesInstantiationException,
                            org.openl.rules.project.resolving.ProjectResolvingException,
                            ClassNotFoundException {
        SimpleProjectEngineFactory<?> factory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/test/export/example3")
                .setExecutionMode(false)
                .build();

        CompiledOpenClass openLRules;
        try {
            TestMethodNodeBinder.keepTestsInExecutionMode();
            openLRules = factory.getCompiledOpenClass();
        } finally {
            TestMethodNodeBinder.removeTestsInExecutionMode();
        }
        IOpenClass openClass = openLRules.getOpenClassWithErrors();
        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);

        TestUnitsResults[] results = new TestUnitsResults[tests.length];
        for (int i = 0; i < tests.length; i++) {
            TestSuiteMethod test = tests[i];
            results[i] = new TestSuiteWithPreview(test).invokeSequentially(openClass, 1L);
        }

        // Tests can appear in a random order. For testing convenience sort them alphabetically
        Arrays.sort(results, new Comparator<TestUnitsResults>() {
            @Override
            public int compare(TestUnitsResults o1, TestUnitsResults o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        testResults = results;
    }

    @Test
    public void allResultsInFirstPage() throws Exception {
        File xlsx;
        try (TestResultExport export = new TestResultExport(testResults, -1)) {
            xlsx = export.createExcelFile();
            assertTrue(xlsx.exists());

            try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
                assertEquals(1, workbook.getNumberOfSheets());

                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowNum = TestResultExport.FIRST_ROW;
                rowNum = checkDriverPremiumTest(sheet, rowNum);

                rowNum += TestResultExport.SPACE_BETWEEN_RESULTS + 1;
                rowNum = checkPolicyPremiumTest(sheet, rowNum);

                rowNum += TestResultExport.SPACE_BETWEEN_RESULTS + 1;
                rowNum = checkVehiclePremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());
            }
        }

        assertFalse(xlsx.exists());
    }

    @Test
    public void oneResultPerPage() throws Exception {
        File xlsx;
        try (TestResultExport export = new TestResultExport(testResults, 1)) {
            xlsx = export.createExcelFile();
            assertTrue(xlsx.exists());

            try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
                assertEquals(3, workbook.getNumberOfSheets());

                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowNum = TestResultExport.FIRST_ROW;
                rowNum = checkDriverPremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());

                sheet = workbook.getSheetAt(1);
                rowNum = TestResultExport.FIRST_ROW;
                rowNum = checkPolicyPremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());

                sheet = workbook.getSheetAt(2);
                rowNum = TestResultExport.FIRST_ROW;
                rowNum = checkVehiclePremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());
            }
        }

        assertFalse(xlsx.exists());
    }

    @Test
    public void twoResultsPerPage() throws Exception {
        File xlsx;
        try (TestResultExport export = new TestResultExport(testResults, 2)) {
            xlsx = export.createExcelFile();
            assertTrue(xlsx.exists());

            try (XSSFWorkbook workbook = new XSSFWorkbook(xlsx)) {
                assertEquals(2, workbook.getNumberOfSheets());

                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowNum = TestResultExport.FIRST_ROW;
                rowNum = checkDriverPremiumTest(sheet, rowNum);

                rowNum += TestResultExport.SPACE_BETWEEN_RESULTS + 1;
                rowNum = checkPolicyPremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());

                sheet = workbook.getSheetAt(1);
                rowNum = TestResultExport.FIRST_ROW;
                rowNum = checkVehiclePremiumTest(sheet, rowNum);
                assertEquals(rowNum, sheet.getLastRowNum());
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

        row = sheet.getRow(++rowNum);
        assertRowText(row, "2", "Failed", "Spencer, Sara's Son", "Young Driver", "Eligible", "Standard Risk Driver");
        assertRowColors(row, RED_MAIN, RED_MAIN, null, GREEN_FIELDS, RED_FIELDS, GREEN_FIELDS);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "3", "Passed", "Spencer, No Training", "Young Driver", "Not Eligible", "High Risk Driver");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);

        return rowNum;
    }

    private int checkPolicyPremiumTest(XSSFSheet sheet, int rowNum) {
        assertRowText(sheet.getRow(rowNum), "PolicyPremiumTest");

        rowNum++;
        assertRowText(sheet.getRow(rowNum), "2 test cases");

        rowNum += 2;
        XSSFRow row = sheet.getRow(rowNum);
        assertRowText(row, "ID", "Status", "Description", "Name of Policy", "Expected Score", "Expected Eligibility", "Expected Premium");
        assertRowColors(row, HEADER, HEADER, HEADER, HEADER, HEADER, HEADER);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "case1", "Passed", "Test Policy1", "Policy1", "0", "Eligible", "922.5");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "case2", "Passed", "Test Second policy", "Policy2", "110", "Eligible", "2960");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);

        return rowNum;
    }

    private int checkVehiclePremiumTest(XSSFSheet sheet, int rowNum) {
        assertRowText(sheet.getRow(rowNum), "VehiclePremiumTest");

        rowNum++;
        assertRowText(sheet.getRow(rowNum), "3 test cases");

        rowNum += 2;
        XSSFRow row = sheet.getRow(rowNum);
        assertRowText(row, "ID", "Status", "Car", "Expected Theft Rating", "Expected Injury Rating", "Expected Eligibility", "Created date");
        assertRowColors(row, HEADER, HEADER, HEADER, HEADER, HEADER, HEADER, HEADER);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "1", "Passed", "2005 Honda Odyssey", "Moderate", "Low", "Eligible", "2017-01-02");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "2", "Passed", "2002 Toyota Camry", "Low", "Moderate", "Eligible", "2017-01-02");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);

        row = sheet.getRow(++rowNum);
        assertRowText(row, "3", "Passed", "1965 VW Bug", "High", "Extremely High", "Not Eligible", "2017-01-02");
        assertRowColors(row, GREEN_MAIN, GREEN_MAIN, null, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS, GREEN_FIELDS);

        return rowNum;
    }

    private void assertRowText(XSSFRow row, String... values) {
        int column = TestResultExport.FIRST_COLUMN;

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
        int column = TestResultExport.FIRST_COLUMN;

        String sheetName = row.getSheet().getSheetName();
        for (Integer color : colors) {
            XSSFColor expected = color == null ? null : new XSSFColor(new Color(color));
            String message = "Incorrect color in: {" + sheetName + "[" + row.getRowNum() + ", " + column + "]}";
            assertEquals(message, expected, row.getCell(column).getCellStyle().getFillForegroundColorColor());
            column++;
        }
    }

    @SuppressWarnings("deprecation")
    private String asString(Cell cell) {
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BLANK:
                return null;
            case Cell.CELL_TYPE_BOOLEAN:
                return "" + cell.getBooleanCellValue();
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat(DATE_FORMAT).format(cell.getDateCellValue());
                }
                return "" + NumberUtils.intOrDouble(cell.getNumericCellValue());
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            default:
                throw new UnsupportedOperationException();
        }
    }

}