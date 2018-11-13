package org.openl.rules.testmethod.export;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.data.PrimaryKeyField;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ParameterExportTest {
    private ParameterExport export;
    private SXSSFWorkbook workbook;
    private SXSSFSheet sheet;

    @Before
    public void setUp() {
        workbook = new SXSSFWorkbook();
        sheet = workbook.createSheet("Type 1");
        export = new ParameterExport(new Styles(workbook));
    }

    @After
    public void tearDown() {
        workbook.dispose();
    }

    @Test
    public void simpleType() throws IOException {
        export.write(sheet, mockResults(params(0.5)));

        XSSFSheet sheetToCheck = saveAndReadSheet();
        assertEquals(BaseExport.FIRST_ROW, sheetToCheck.getFirstRowNum());
        assertEquals(5, sheetToCheck.getLastRowNum());

        int rowNum = BaseExport.FIRST_ROW;
        XSSFRow row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "Parameters of TestRule");

        rowNum += 2;
        row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "p1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "0.5");
    }

    @Test
    public void emptyParameters() throws IOException {
        export.write(sheet, mockResults());

        assertEquals(0, saveAndReadSheet().getLastRowNum());
    }

    @Test
    public void halfFilled() throws IOException {
        export.write(sheet, mockResults(params(new A("name1")), params(new A("name2"))));

        XSSFSheet sheetToCheck = saveAndReadSheet();
        assertEquals(BaseExport.FIRST_ROW, sheetToCheck.getFirstRowNum());
        assertEquals(BaseExport.FIRST_ROW + 4, sheetToCheck.getLastRowNum());
        int rowNum = BaseExport.FIRST_ROW;
        XSSFRow row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "Parameters of TestRule");

        rowNum += 2;
        row = sheetToCheck.getRow(rowNum);
        assertEquals(BaseExport.FIRST_COLUMN, row.getFirstCellNum());
        assertEquals(BaseExport.FIRST_COLUMN + 2, row.getLastCellNum());

        assertRowEquals(row, "ID", "p1.name");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "name1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#2", "name2");
    }

    @Test
    public void arrayOfObjects() throws IOException {
        List<TestUnitsResults> result = mockResults(
                params((Object) new A[] { new A("name1"), new A("name2") }),
                params((Object) null),
                params((Object) new A[] { new A("name3") }),
                params()
        );
        export.write(sheet, result);

        XSSFSheet sheetToCheck = saveAndReadSheet();
        int rowNum = BaseExport.FIRST_ROW + 2;
        XSSFRow row = sheetToCheck.getRow(rowNum);

        assertRowEquals(row, "ID", "p1.name");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "name1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "", "name2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#2", "");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#3", "name3");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#4", "");

        List<CellRangeAddress> mergedRegions = sheetToCheck.getMergedRegions();
        assertEquals(1, mergedRegions.size());
        assertTrue(mergedRegions.contains(new CellRangeAddress(5, 6, 1, 1))); // "ID"
    }

    @Test
    public void complexObjects() throws IOException {
        A A1 = new A("name1", 1);
        A A2 = new A("name2", 2);

        B B11 = new B("id11", new A("n1", 111, 2, 3), new A("n2", 112));
        B B12 = new B("id12", new A("n3", 121), new A("n4", 122), new A("n5", 123));

        B B1 = new B("id1", A1, A2);
        B1.setChildBValues(B11, B12);

        export.write(sheet, mockResults(params(B1)));

        XSSFSheet sheetToCheck = saveAndReadSheet();
        int rowNum = BaseExport.FIRST_ROW + 2;

        XSSFRow row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row,
                "ID",
                "p1.id",
                "p1.aValues.name",
                "p1.aValues.values",
                "p1.childBValues.id",
                "p1.childBValues.aValues.name",
                "p1.childBValues.aValues.values");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "id1", "name1", "1", "id11", "n1", "111,2,3");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "", "", "name2", "2", "", "n2", "112");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "", "", "", "", "id12", "n3", "121");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "", "", "", "", "", "n4", "122");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "", "", "", "", "", "n5", "123");

        List<CellRangeAddress> mergedRegions = sheetToCheck.getMergedRegions();
        assertEquals(6, mergedRegions.size());
        assertTrue(mergedRegions.contains(new CellRangeAddress(5, 9, 1, 1))); // "ID"
        assertTrue(mergedRegions.contains(new CellRangeAddress(5, 9, 2, 2))); // "id"
        assertTrue(mergedRegions.contains(new CellRangeAddress(6, 9, 3, 3))); // aValues.name
        assertTrue(mergedRegions.contains(new CellRangeAddress(6, 9, 4, 4))); // aValues.values
        assertTrue(mergedRegions.contains(new CellRangeAddress(5, 6, 5, 5))); // childBValues.id
        assertTrue(mergedRegions.contains(new CellRangeAddress(7, 9, 5, 5))); // childBValues.id
    }

    @Test
    public void twoParameters() throws IOException {
        export.write(sheet, mockResults(
                params(new Class[] { A.class, String.class }, null, "str1"),
                params(new A("name2", 5, 6), "str2")
        ));

        XSSFSheet sheetToCheck = saveAndReadSheet();
        int rowNum = BaseExport.FIRST_ROW + 2;

        XSSFRow row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "p1.name", "p1.values", "p2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "", "", "str1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#2", "name2", "5,6", "str2");
    }

    @Test
    public void twoParametersWithArray() throws IOException {
        export.write(sheet, mockResults(
                params(new Class[] { A[].class, String.class }, null, "str1"),
                params(new A[] {}, "str2"),
                params(new A[] { new A("name3", 5, 6) }, "str3"),
                params(new A[] { new A("name4.1"), new A("name4.2", 7) }, "str4")
        ));

        XSSFSheet sheetToCheck = saveAndReadSheet();
        int rowNum = BaseExport.FIRST_ROW + 2;

        XSSFRow row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "p1.name", "p1.values", "p2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "", "", "str1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#2", "", "", "str2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#3", "name3", "5,6", "str3");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#4", "name4.1", "", "str4");
        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "", "name4.2", "7", "");

        List<CellRangeAddress> mergedRegions = sheetToCheck.getMergedRegions();
        assertEquals(2, mergedRegions.size());
        assertTrue(mergedRegions.contains(new CellRangeAddress(8, 9, 1, 1))); // "ID"
        assertTrue(mergedRegions.contains(new CellRangeAddress(8, 9, 4, 4))); // "p2"
    }

    @Test
    public void twoTestsInSheet() throws IOException {
        export.write(sheet, Arrays.asList(
                mockResult("FirstTest",
                        params(new A("name1"), "str1"),
                        params(new A("name2"), "str2")
                ),
                mockResult("SecondTest",
                        params(1, "str1", 3.5),
                        params(2, "str2", 4.5)
                )
        ));

        XSSFSheet sheetToCheck = saveAndReadSheet();
        // First test
        int rowNum = BaseExport.FIRST_ROW;
        XSSFRow row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "Parameters of FirstTest");

        rowNum += 2;
        row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "p1.name", "p2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "name1", "str1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#2", "name2", "str2");

        // Second test
        rowNum += BaseExport.SPACE_BETWEEN_RESULTS + 1;
        row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "Parameters of SecondTest");

        rowNum += 2;
        row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "p1", "p2", "p3");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "1", "str1", "3.5");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#2", "2", "str2", "4.5");
    }

    @Test
    public void paramsWithPK() throws IOException {
        export.write(sheet, mockResults(
                params(new String[] { "n1", null }, new A("name1"), "str1"),
                params(new String[] { "n2", null }, new A("name2", 5, 6), "str2")
        ));

        XSSFSheet sheetToCheck = saveAndReadSheet();
        int rowNum = BaseExport.FIRST_ROW + 2;

        XSSFRow row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "p1._PK_", "p1.name", "p1.values", "p2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "n1", "name1", "", "str1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#2", "n2", "name2", "5,6", "str2");
    }

    private void assertRowEquals(Row row, String... values) {
        assertNotNull("Row is absent. Expected values: " + Arrays.toString(values), row);

        int colNum = BaseExport.FIRST_COLUMN;
        for (String value : values) {
            Cell cell = row.getCell(colNum);
            assertNotNull("Column " + colNum + " is absent", cell);
            assertEquals("Incorrect column " + colNum, value, cell.getStringCellValue());
            colNum++;
        }

        short lastCellNum = row.getLastCellNum();
        int total = lastCellNum - BaseExport.FIRST_COLUMN;
        if (values.length < total) {
            StringBuilder sb = new StringBuilder("Missed values: ");
            int count = 0;
            while (colNum < lastCellNum) {
                if (count > 0) {
                    sb.append(',');
                }
                sb.append(row.getCell(colNum++).getStringCellValue());
                count++;
            }

            fail(sb.toString());
        }
    }

    private ParameterWithValueDeclaration[] params(Object... values) {
        return params(null, null, values);
    }

    private ParameterWithValueDeclaration[] params(Class[] types, Object... values) {
        return params(null, types, values);
    }

    private ParameterWithValueDeclaration[] params(String[] pkValues, Object... values) {
        return params(pkValues, null, values);
    }

    private ParameterWithValueDeclaration[] params(String[] pkValues, Class[] types, Object... values) {
        ParameterWithValueDeclaration[] params = new ParameterWithValueDeclaration[values.length];
        for (int i = 0; i < values.length; i++) {

            IOpenClass type;
            if (types == null) {
                type = ParameterWithValueDeclaration.getParamType(values[i]);
            } else {
                type = JavaOpenClass.getOpenClass(types[i]);
            }

            PrimaryKeyField field = mockKeyField(pkValues, i);
            params[i] = new ParameterWithValueDeclaration("p" + (i + 1), values[i], type, field);
        }
        return params;
    }

    private PrimaryKeyField mockKeyField(String[] pkValues, int i) {
        if (pkValues != null && pkValues[i] != null) {
            PrimaryKeyField field = mock(PrimaryKeyField.class);
            when(field.get(any(), any(IRuntimeEnv.class))).thenReturn(pkValues[i]);
            return field;
        }

        return null;
    }

    private XSSFSheet saveAndReadSheet() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        workbook.write(stream);
        workbook.close();
        return new XSSFWorkbook(new ByteArrayInputStream(stream.toByteArray())).getSheetAt(0);
    }

    private List<TestUnitsResults> mockResults(ParameterWithValueDeclaration[]... paramsForEachCase) {
        return Collections.singletonList(mockResult("TestRule", paramsForEachCase));
    }

    private TestUnitsResults mockResult(String testMethodName, ParameterWithValueDeclaration[]... paramsForEachCase) {
        IOpenMethod testedMethod = mock(IOpenMethod.class);
        when(testedMethod.getName()).thenReturn(testMethodName);

        List<TestDescription> results = new ArrayList<>();
        for (int i = 0; i < paramsForEachCase.length; i++) {
            TestDescription testDescription = mock(TestDescription.class);
            when(testDescription.getId()).thenReturn("#" + (i + 1));
            when(testDescription.getExecutionParams()).thenReturn(paramsForEachCase[i]);
            when(testDescription.getTestedMethod()).thenReturn(testedMethod);
            results.add(testDescription);
        }

        TestSuite testSuite = new TestSuite(results.toArray(new TestDescription[0]));
        return new TestUnitsResults(testSuite);
    }

}