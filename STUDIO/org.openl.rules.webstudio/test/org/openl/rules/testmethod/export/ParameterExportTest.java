package org.openl.rules.testmethod.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParameterExportTest extends AbstractParameterExportTest {

    private ParameterExport export;

    @BeforeEach
    void setUp() {
        workbook = new SXSSFWorkbook();
        sheet = workbook.createSheet("Type 1");
        export = new ParameterExport(new Styles(workbook));
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
        assertRowEquals(row, "ID", "p1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "0.5");
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
        assertEquals(BaseExport.FIRST_ROW + 4, sheetToCheck.getLastRowNum());
        var rowNum = BaseExport.FIRST_ROW;
        var row = sheetToCheck.getRow(rowNum);
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
    void listOfObjects() throws IOException {
        List<Object> paramList = Arrays.asList(12, 23.0);
        var mapValues = new HashMap<String, Integer>();
        mapValues.put("key1", 123);
        mapValues.put("key2", 333);
        var result = mockResults(params(paramList, mapValues));
        export.write(sheet, result, true);
        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;
        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "p1", "p2[\"key1\"]:Integer", "p2[\"key2\"]:Integer");
        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "12,23.0", "123", "333");
    }

    /**
     * A run may be given nothing for a map, and a map may hold nothing under a key. The parameter is written out
     * either way: this is what the workbook of a result looks like when nothing was asked of it, so a run of a
     * rule taking a map must not be the one run that cannot be written down.
     */
    @Test
    void mapThatIsNotThere() throws IOException {
        var result = mockResults(params(new Class[]{ Map.class }, new Object[]{ null }));
        export.write(sheet, result, true);
        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;
        assertRowEquals(sheetToCheck.getRow(rowNum), "ID", "p1");
        assertRowEquals(sheetToCheck.getRow(++rowNum), "#1", "");
    }

    @Test
    void mapHoldingNothingUnderAKey() throws IOException {
        var mapValues = new HashMap<String, Integer>();
        mapValues.put("key1", 123);
        mapValues.put("key2", null);
        var result = mockResults(params(mapValues));
        export.write(sheet, result, true);
        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;
        // The key is named all the same; with nothing under it there is no type to name after it.
        assertRowEquals(sheetToCheck.getRow(rowNum), "ID", "p1[\"key1\"]:Integer", "p1[\"key2\"]");
        assertRowEquals(sheetToCheck.getRow(++rowNum), "#1", "123", "");
    }

    /**
     * The columns of a map are laid out once for the whole table, from every key any of the cases carries.
     * Taken from the first case alone they would be too few for a case holding more, and its values would be
     * written over the columns of whatever stands after it.
     */
    @Test
    void casesWhoseMapsDifferShareTheColumns() throws IOException {
        var first = new LinkedHashMap<String, Integer>();
        first.put("a", 1);
        var second = new LinkedHashMap<String, Integer>();
        second.put("a", 2);
        second.put("b", 3);
        var result = mockResults(params(first), params(second));
        export.write(sheet, result, true);
        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;
        assertRowEquals(sheetToCheck.getRow(rowNum), "ID", "p1[\"a\"]:Integer", "p1[\"b\"]:Integer");
        assertRowEquals(sheetToCheck.getRow(++rowNum), "#1", "1", "");
        assertRowEquals(sheetToCheck.getRow(++rowNum), "#2", "2", "3");
    }

    @Test
    void aCaseGivenNoMapLeavesTheColumnsOfTheOthersWhereTheyAre() throws IOException {
        var second = new LinkedHashMap<String, Integer>();
        second.put("a", 1);
        second.put("b", 2);
        var result = mockResults(
                params(new Class[]{ Map.class }, new Object[]{ null }),
                params(new Class[]{ Map.class }, new Object[]{ second }));
        export.write(sheet, result, true);
        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;
        assertRowEquals(sheetToCheck.getRow(rowNum), "ID", "p1[\"a\"]:Integer", "p1[\"b\"]:Integer");
        assertRowEquals(sheetToCheck.getRow(++rowNum), "#1", "", "");
        assertRowEquals(sheetToCheck.getRow(++rowNum), "#2", "1", "2");
    }

    @Test
    void complexObjectWithListAndMap() throws IOException {
        List<Object> paramList = Arrays.asList(12, 23.0);
        var mapValues = new HashMap<String, Integer>();
        mapValues.put("key1", 123);
        mapValues.put("key2", 333);
        var obj = new ComplexObj(paramList, mapValues);
        var result = mockResults(params(obj));
        export.write(sheet, result, true);
        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;
        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "p1.paramList", "p1.mapValues[\"key1\"]:Integer", "p1.mapValues[\"key2\"]:Integer");
        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "12,23.0", "123", "333");
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
    void complexObjects() throws IOException {
        var a1 = new A("name1", 1);
        var a2 = new A("name2", 2);

        var b11 = new B("id11", new A("n1", 111, 2, 3), new A("n2", 112));
        var b12 = new B("id12", new A("n3", 121), new A("n4", 122), new A("n5", 123));

        var b1 = new B("id1", a1, a2);
        b1.setChildBValues(b11, b12);

        export.write(sheet, mockResults(params(b1)), true);

        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;

        var row = sheetToCheck.getRow(rowNum);
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
    void twoParameters() throws IOException {
        export.write(sheet,
                mockResults(params(new Class[]{A.class, String.class}, null, "str1"),
                        params(new A("name2", 5, 6), "str2")),
                true);

        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;

        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "p1.name", "p1.values", "p2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "", "", "str1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#2", "name2", "5,6", "str2");
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
    void paramsWithPK() throws IOException {
        export.write(sheet,
                mockResults(params(new String[]{"n1", null}, new A("name1"), "str1"),
                        params(new String[]{"n2", null}, new A("name2", 5, 6), "str2")),
                true);

        var sheetToCheck = saveAndReadSheet();
        var rowNum = BaseExport.FIRST_ROW + 2;

        var row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row, "ID", "p1._PK_", "p1.name", "p1.values", "p2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#1", "n1", "name1", "", "str1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "#2", "n2", "name2", "5,6", "str2");
    }

}
