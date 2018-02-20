package org.openl.rules.webstudio.web.test.export;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.java.JavaOpenClass;

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
        export.write(sheet, JavaOpenClass.DOUBLE, singletonList(new ParameterWithValueDeclaration("d", 0.5)));
        assertEquals(0, saveAndReadSheet().getLastRowNum());
    }

    @Test
    public void emptyParameters() throws IOException {
        export.write(sheet,
                JavaOpenClass.getOpenClass(A.class),
                Collections.<ParameterWithValueDeclaration>emptyList());
        assertEquals(0, saveAndReadSheet().getLastRowNum());
    }

    @Test
    public void halfFilled() throws IOException {
        List<ParameterWithValueDeclaration> data = new ArrayList<>();
        data.add(param(new A("name1")));
        data.add(param(new A("name2")));
        export.write(sheet, JavaOpenClass.getOpenClass(A.class), data);

        XSSFSheet sheetToCheck = saveAndReadSheet();
        assertEquals(BaseExport.FIRST_ROW, sheetToCheck.getFirstRowNum());
        assertEquals(BaseExport.FIRST_ROW + 2, sheetToCheck.getLastRowNum());
        int rowNum = BaseExport.FIRST_ROW;
        XSSFRow row = sheetToCheck.getRow(rowNum);
        assertEquals(BaseExport.FIRST_COLUMN, row.getFirstCellNum());
        assertEquals(BaseExport.FIRST_COLUMN + 2, row.getLastCellNum());

        assertRowEquals(row, "ID", "name");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "1", "name1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "2", "name2");
    }

    @Test
    public void arrayOfObjects() throws IOException {
        List<ParameterWithValueDeclaration> data = new ArrayList<>();
        data.add(param(new A[] { new A("name1"), new A("name2") }));
        data.add(param(null));
        data.add(param(new A[] { new A("name3") }));
        export.write(sheet, JavaOpenClass.getOpenClass(A[].class), data);

        XSSFSheet sheetToCheck = saveAndReadSheet();
        int rowNum = BaseExport.FIRST_ROW;
        XSSFRow row = sheetToCheck.getRow(rowNum);

        assertRowEquals(row, "ID", "name");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "1", "name1");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "", "name2");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "2", "");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "3", "name3");

        List<CellRangeAddress> mergedRegions = sheetToCheck.getMergedRegions();
        assertEquals(1, mergedRegions.size());
        assertTrue(mergedRegions.contains(new CellRangeAddress(3, 4, 1, 1))); // "ID"
    }

    @Test
    public void complexObjects() throws IOException {
        A A1 = new A("name1", 1);
        A A2 = new A("name2", 2);

        B B11 = new B("id11", new A("n1", 111, 2, 3), new A("n2", 112));
        B B12 = new B("id12", new A("n3", 121), new A("n4", 122), new A("n5", 123));

        B B1 = new B("id1", A1, A2);
        B1.setChildBValues(B11, B12);

        List<ParameterWithValueDeclaration> data = new ArrayList<>();
        data.add(param(B1));

        export.write(sheet, JavaOpenClass.getOpenClass(B.class), data);
        XSSFSheet sheetToCheck = saveAndReadSheet();
        int rowNum = BaseExport.FIRST_ROW;

        XSSFRow row = sheetToCheck.getRow(rowNum);
        assertRowEquals(row,
                "ID",
                "id",
                "aValues[].name",
                "aValues[].values",
                "childBValues[].id",
                "childBValues[].aValues[].name",
                "childBValues[].aValues[].values");

        row = sheetToCheck.getRow(++rowNum);
        assertRowEquals(row, "1", "id1", "name1", "1", "id11", "n1", "111,2,3");

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
        assertTrue(mergedRegions.contains(new CellRangeAddress(3, 7, 1, 1))); // "ID"
        assertTrue(mergedRegions.contains(new CellRangeAddress(3, 7, 2, 2))); // "id"
        assertTrue(mergedRegions.contains(new CellRangeAddress(4, 7, 3, 3))); // aValues[].name
        assertTrue(mergedRegions.contains(new CellRangeAddress(4, 7, 4, 4))); // aValues[].values
        assertTrue(mergedRegions.contains(new CellRangeAddress(3, 4, 5, 5))); // childBValues[].id
        assertTrue(mergedRegions.contains(new CellRangeAddress(5, 7, 5, 5))); // childBValues[].id
    }

    private void assertRowEquals(Row row, String... values) {
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

    private ParameterWithValueDeclaration param(Object value) {
        return new ParameterWithValueDeclaration("p", value);
    }

    private XSSFSheet saveAndReadSheet() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        workbook.write(stream);
        workbook.close();
        return new XSSFWorkbook(new ByteArrayInputStream(stream.toByteArray())).getSheetAt(0);
    }
}