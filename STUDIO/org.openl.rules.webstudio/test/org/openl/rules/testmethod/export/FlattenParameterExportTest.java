package org.openl.rules.testmethod.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Arrays;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlattenParameterExportTest extends AbstractParameterExportTest {

    private FlattenParameterExport export;

    @BeforeEach
    void setUp() {
        workbook = new SXSSFWorkbook();
        sheet = workbook.createSheet("Type 1");
        export = new FlattenParameterExport(new Styles(workbook));
    }

    @AfterEach
    void tearDown() {
        workbook.dispose();
    }

    @Test
    void simpleType() throws IOException {
        export.write(sheet, mockResults(params(0.5)), true);

        var sheetToCheck = saveAndReadSheet();
        assertEquals(BaseExport.FIRST_ROW, sheetToCheck.getFirstRowNum());
        assertEquals(5, sheetToCheck.getLastRowNum());

        var rowNum = BaseExport.FIRST_ROW;
        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "Parameters of TestRule");

        rowNum += 2;
        row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "#1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1", "0.5");
    }

    @Test
    void emptyParameters() throws IOException {
        export.write(sheet, mockResults(), true);

        assertEquals(-1, saveAndReadSheet().getLastRowNum());
    }

    @Test
    void halfFilled() throws IOException {
        export.write(sheet, mockResults(params(new A("name1")), params(new A("name2"))), true);

        var sheetToCheck = saveAndReadSheet();
        assertEquals(BaseExport.FIRST_ROW, sheetToCheck.getFirstRowNum());
        assertEquals(BaseExport.FIRST_ROW + 3, sheetToCheck.getLastRowNum());
        var rowNum = BaseExport.FIRST_ROW;
        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "Parameters of TestRule");

        rowNum += 2;
        row = sheetToCheck.getRow(rowNum);
        assertEquals(BaseExport.FIRST_COLUMN, row.getFirstCellNum());
        assertEquals(BaseExport.FIRST_COLUMN + 3, row.getLastCellNum());

        assertRowEquals(row, "ID", "#1", "#2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.name", "name1", "name2");
    }

    @Test
    void arrayOfObjects() throws IOException {
        var result = mockResults(params((Object) new A[]{new A("name1"), new A("name2")}),
                params((Object) null),
                params((Object) new A[]{new A("name3")}),
                params());
        export.write(sheet, result, true);

        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;
        var row = sheetToCheck.getRow(rowNum);

        assertRowEquals(row, "ID", "#1", "#2", "#3", "#4");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1[0].name", "name1", "", "name3", "");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1[1].name", "name2", "", "", "");

        assertNull(sheetToCheck.getRow(++rowNum));
    }

    @Test
    void complexObjects() throws IOException {
        var A1 = new A("name1", 1);
        var A2 = new A("name2", 2);

        var B11 = new B("id11", new A("n1", 111, 2, 3), new A("n2", 112));
        var B12 = new B("id12", new A("n3", 121), new A("n4", 122), new A("n5", 123));

        var B1 = new B("id1", A1, A2);
        B1.setChildBValues(B11, B12);

        export.write(sheet, mockResults(params(B1)), true);

        var sheetToCheck = saveAndReadSheet();
        assertEquals(BaseExport.FIRST_ROW + 21, sheetToCheck.getLastRowNum());
        var rowNum = BaseExport.FIRST_ROW + 2;

        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "#1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.aValues[0].name", "name1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.aValues[0].values[0]", "1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.aValues[1].name", "name2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.aValues[1].values[0]", "2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[0].aValues[0].name", "n1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[0].aValues[0].values[0]", "111");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[0].aValues[0].values[1]", "2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[0].aValues[0].values[2]", "3");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[0].aValues[1].name", "n2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[0].aValues[1].values[0]", "112");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[0].id", "id11");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[1].aValues[0].name", "n3");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[1].aValues[0].values[0]", "121");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[1].aValues[1].name", "n4");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[1].aValues[1].values[0]", "122");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[1].aValues[2].name", "n5");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[1].aValues[2].values[0]", "123");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.childBValues[1].id", "id12");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.id", "id1");

        assertNull(sheetToCheck.getRow(++rowNum));
    }

    @Test
    void nestedComplexFieldsWithTheSameDataAndType() throws IOException {
        var B1 = new B(null, new A("name1", 1),  new A(null, 2),  new A("name3", 3));

        export.write(sheet, mockResults(params(B1)), true);

        var sheetToCheck = saveAndReadSheet();
        assertEquals(BaseExport.FIRST_ROW + 7, sheetToCheck.getLastRowNum());
        var rowNum = BaseExport.FIRST_ROW + 2;

        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "#1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.aValues[0].name", "name1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.aValues[0].values[0]", "1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.aValues[1].values[0]", "2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.aValues[2].name", "name3");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.aValues[2].values[0]", "3");

        assertNull(sheetToCheck.getRow(++rowNum));
    }

    @Test
    void nestedArrayWithComplex_Case1() throws IOException {
        var C1 = new C(new A("name1"), new A("name1"));

        export.write(sheet, mockResults(params(C1)), true);

        var sheetToCheck = saveAndReadSheet();
        assertEquals(BaseExport.FIRST_ROW + 4, sheetToCheck.getLastRowNum());
        var rowNum = BaseExport.FIRST_ROW + 2;

        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "#1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.filed1.name", "name1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.filed2.name", "name1");

        assertNull(sheetToCheck.getRow(++rowNum));
    }

    @Test
    void twoParameters() throws IOException {
        export.write(sheet,
                mockResults(params(new Class[]{A.class, String.class}, null, "str1"),
                        params(new A("name2", 5, 6), "str2")),
                true);

        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;

        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "#1", "#2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.name", "", "name2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.values[0]", "", "5");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.values[1]", "", "6");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p2", "str1", "str2");

        assertNull(sheetToCheck.getRow(++rowNum));
    }

    @Test
    void twoParametersWithArray() throws IOException {
        export.write(sheet,
                mockResults(params(new Class[]{A[].class, String.class}, null, "str1"),
                        params(new A[]{}, "str2"),
                        params(new A[]{new A("name3", 5, 6)}, "str3"),
                        params(new A[]{new A("name4.1"), new A("name4.2", 7)}, "str4")),
                true);

        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;

        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "#1", "#2", "#3", "#4");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1[0].name", "", "", "name3", "name4.1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1[0].values[0]", "", "", "5", "");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1[0].values[1]", "", "", "6", "");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1[1].name", "", "", "", "name4.2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1[1].values[0]", "", "", "", "7");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p2", "str1", "str2", "str3", "str4");

        assertNull(sheetToCheck.getRow(++rowNum));
    }

    @Test
    void twoTestsInSheet() throws IOException {
        export.write(sheet,
                Arrays.asList(mockResult("FirstTest", params(new A("name1"), "str1"), params(new A("name2"), "str2")),
                        mockResult("SecondTest", params(1, "str1", 3.5), params(2, "str2", 4.5))),
                true);

        var sheetToCheck = saveAndReadSheet();
        // First test
        var rowNum = BaseExport.FIRST_ROW;
        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "Parameters of FirstTest");

        rowNum += 2;
        row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "#1", "#2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1.name", "name1", "name2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p2", "str1", "str2");

        // Second test
        rowNum += BaseExport.SPACE_BETWEEN_RESULTS + 1;
        row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "Parameters of SecondTest");

        rowNum += 2;
        row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "#1", "#2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p1", "1", "2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p2", "str1", "str2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "p3", "3.5", "4.5");

        assertNull(sheetToCheck.getRow(++rowNum));
    }

}
