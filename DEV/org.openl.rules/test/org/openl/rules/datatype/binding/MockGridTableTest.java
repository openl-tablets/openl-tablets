package org.openl.rules.datatype.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;

/**
 * Created by dl on 6/16/14.
 */
public class MockGridTableTest {

    private static final String CELL1 = "cell1";
    private static final String CELL2 = "cell2";
    private static final String CELL3 = "cell3";
    private static final String CELL4 = "cell4";
    private static final String CELL5 = "cell5";
    private static final String CELL6 = "cell6";

    @Test
    public void testCellMergedHorizontally() {
        String[][] mas = new String[1][3];
        mas[0][0] = CELL1;
        mas[0][1] = null;
        mas[0][2] = null;
        MockGridTable t = new MockGridTable(mas);

        assertEquals(3, t.getWidth());
        assertEquals(1, t.getHeight());

        ICell cell = t.getCell(0, 0);
        assertEquals(CELL1, cell.getStringValue());

        assertEquals(3, cell.getWidth());
        assertEquals(1, cell.getHeight());
    }

    @Test
    public void testCellMergedVertically() {
        String[][] mas = new String[3][1];
        mas[0][0] = CELL1;
        mas[1][0] = null;
        mas[2][0] = null;
        MockGridTable t = new MockGridTable(mas);

        assertEquals(1, t.getWidth());
        assertEquals(3, t.getHeight());

        ICell cell = t.getCell(0, 0);
        assertEquals(CELL1, cell.getStringValue());

        assertEquals(1, cell.getWidth());
        assertEquals(3, cell.getHeight());
    }

    @Test
    public void testCellMergedVertically1() {
        String[][] mas = new String[3][1];
        mas[0][0] = null;
        mas[1][0] = CELL1;
        mas[2][0] = null;
        try {
            new MockGridTable(mas);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("There should be any not null value before the null.", ex.getMessage());
        }
    }

    @Test
    public void testGridTable() {
        String[][] mas = new String[2][3];
        mas[0][0] = CELL1;
        mas[0][1] = CELL2;
        mas[0][2] = CELL3;
        mas[1][0] = CELL4;
        mas[1][1] = CELL5;
        mas[1][2] = CELL6;

        MockGridTable t = new MockGridTable(mas);

        assertEquals(CELL1, t.getCell(0, 0).getStringValue());
        assertEquals(CELL2, t.getCell(1, 0).getStringValue());
        assertEquals(CELL3, t.getCell(2, 0).getStringValue());
        assertEquals(CELL4, t.getCell(0, 1).getStringValue());
        assertEquals(CELL5, t.getCell(1, 1).getStringValue());
        assertEquals(CELL6, t.getCell(2, 1).getStringValue());

        assertEquals(2, t.getHeight());
        assertEquals(3, t.getWidth());

        IGridTable row_0 = t.getRow(0);
        assertEquals(1, row_0.getHeight());
        assertEquals(3, row_0.getWidth());
        assertEquals(CELL1, row_0.getCell(0, 0).getStringValue());
        assertEquals(CELL2, row_0.getCell(1, 0).getStringValue());
        assertEquals(CELL3, row_0.getCell(2, 0).getStringValue());
    }

    @Test
    public void test2() {
        String[][] mas = new String[2][3];
        mas[0][0] = "Datatype Test";
        mas[0][1] = null;
        mas[0][2] = null;
        mas[1][0] = CELL4;
        mas[1][1] = CELL5;
        mas[1][2] = CELL6;

        MockGridTable t = new MockGridTable(mas);

        ILogicalTable logT = LogicalTableHelper.logicalTable(t);

        assertEquals(1, logT.getWidth());
        assertEquals(2, logT.getHeight());
    }

}
