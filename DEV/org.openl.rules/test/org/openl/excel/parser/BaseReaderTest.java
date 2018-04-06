package org.openl.excel.parser;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class BaseReaderTest {
    protected ExcelReader reader;

    protected abstract ExcelReader createReader() throws IOException;

    @Before
    public void setUp() {
        try {
            reader  = createReader();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        reader.close();
        reader = null;
    }

    @Test
    public void getSheets() {
        List<? extends SheetDescriptor> sheets = reader.getSheets();

        assertEquals(3, sheets.size());

        assertEquals("Main", sheets.get(0).getName());
        assertEquals("Second", sheets.get(1).getName());
        assertEquals("Sheet3", sheets.get(2).getName());

        assertEquals(0, sheets.get(0).getIndex());
        assertEquals(1, sheets.get(1).getIndex());
        assertEquals(2, sheets.get(2).getIndex());
    }

    @Test
    public void getMainSheet() {
        Object[][] cells = reader.getCells(reader.getSheets().get(0));
        assertEquals(12, cells.length);
        assertEquals(1, cells[0].length);

        //// Different type assertions
        int row = 0;
        assertEquals("Value", cells[row][0]);

        row++;
        assertThat(cells[row][0], instanceOf(Integer.class));
        assertEquals(1, cells[row][0]);

        row++;
        assertThat(cells[row][0], instanceOf(Double.class));
        assertEquals("1.5", cells[row][0].toString());

        row++;
        assertThat(cells[row][0], instanceOf(Boolean.class));
        assertEquals(true, cells[row][0]);

        row++;
        assertThat(cells[row][0], instanceOf(Date.class));
        Calendar expected = Calendar.getInstance();
        expected.clear();
        expected.set(2018, Calendar.DECEMBER, 20);
        assertEquals(expected.getTime(), cells[row][0]);

        // Custom date format
        row++;
        assertThat(cells[row][0], instanceOf(Date.class));
        expected.set(2018, Calendar.DECEMBER, 21);
        assertEquals(expected.getTime(), cells[row][0]);

        //// Assertions for formulas
        // Double
        row++;
        assertThat(cells[row][0], instanceOf(Double.class));
        assertEquals("0.75", cells[row][0].toString());

        // Date
        row++;
        assertThat(cells[row][0], instanceOf(Date.class));
        expected.set(2018, Calendar.DECEMBER, 21);
        assertEquals(expected.getTime(), cells[row][0]);

        // String
        row++;
        assertEquals("ConcatValue", cells[row][0]);

        // Boolean
        row++;
        assertEquals(Boolean.TRUE, cells[row][0]);

        //// Indention assertions
        row++;
        assertEquals(new AlignedValue("Indent 1", (short) 1), cells[row][0]);

        row++;
        assertEquals(new AlignedValue("Indent 2", (short) 2), cells[row][0]);
    }

    @Test
    public void getSecondSheet() {
        Object[][] cells = reader.getCells(reader.getSheets().get(1));
        assertEquals(8, cells.length);
        assertEquals(2, cells[0].length);

        assertEquals("SimpleRules Integer  rule1 ( Integer  period )", cells[0][0]);
        assertEquals(MergedCell.MERGE_WITH_LEFT, cells[0][1]);
        assertEquals(MergedCell.MERGE_WITH_UP, cells[1][0]);
        assertEquals(MergedCell.MERGE_WITH_LEFT, cells[1][1]);
        assertEquals("Period", cells[2][0]);
        assertEquals("Factor", cells[2][1]);
        assertEquals(2, cells[3][0]);
        assertEquals(0, cells[3][1]);
        assertEquals(5, cells[4][0]);
        assertEquals(1, cells[4][1]);

        assertNull(cells[5][0]);
        assertNull(cells[5][1]);

        assertNull(cells[6][0]);
        assertEquals("Vertical Merge", cells[6][1]);
        assertNull(cells[7][0]);
        assertEquals(MergedCell.MERGE_WITH_UP, cells[7][1]);
    }

    @Test
    public void getSheet3() {
        Object[][] cells = reader.getCells(reader.getSheets().get(2));
        assertEquals(1, cells.length);
        assertEquals(1, cells[0].length);
        assertNull(cells[0][0]);
    }
}
