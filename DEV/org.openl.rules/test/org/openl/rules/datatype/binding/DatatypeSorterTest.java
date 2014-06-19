package org.openl.rules.datatype.binding;

import org.junit.Test;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.*;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.syntax.impl.IdentifierNode;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNull;

/**
 * Created by dl on 6/17/14.
 */
public class DatatypeSorterTest {
    public static final String CHILD = "Child";
    public static final String INDEPENDENT = "Independent";
    public static final String PARENT = "Parent";

    public static final String DATATYPE_PARENT = String.format("%s %s", XlsNodeTypes.XLS_DATATYPE, PARENT);
    public static final String DATATYPE_CHILD_EXTENDS_PARENT = String.format("%s %s extends %s", XlsNodeTypes.XLS_DATATYPE, CHILD, PARENT);
    public static final String DATATYPE_INDEPENDENT = String.format("%s %s", XlsNodeTypes.XLS_DATATYPE, INDEPENDENT);


    @Test
    public void testOrderDatatypes_Inheritance_Null() {
        assertNull(new DatatypesSorter().sort(null, null));
    }

    @Test
    public void testOrderDatatypes_InheritanceDependency() {
        TableSyntaxNode parentNode = createStubTSN(DATATYPE_PARENT);
        TableSyntaxNode childNode = createStubTSN(DATATYPE_CHILD_EXTENDS_PARENT);
        TableSyntaxNode independentNode = createStubTSN(DATATYPE_INDEPENDENT);

        TableSyntaxNode[] ordered = new DatatypesSorter().sort(
                DatatypeHelper.createTypesMap(
                        new TableSyntaxNode[]{
                                childNode,
                                independentNode,
                                parentNode}),
                null);
        assertEquals(3, ordered.length);
        assertEquals("Parent should be compiled first", DATATYPE_PARENT, ordered[0].getHeader().getModule().getCode());
        assertEquals("Independent datatype position is not changed", DATATYPE_INDEPENDENT, ordered[1].getHeader().getModule().getCode());
        assertEquals("Child position goes after parent", DATATYPE_CHILD_EXTENDS_PARENT, ordered[2].getHeader().getModule().getCode());
    }

    @Test
    public void testOrderDatatypes_fieldsDependency() {
        String[][] table1 = new String[3][2];
        table1[0][0] = "Datatype Type1";
        table1[0][1] = null;
        table1[1][0] = "String";
        table1[1][1] = "name";
        table1[2][0] = "Type2";
        table1[2][1] = "type2Obj";
        GridTable gridTable1 = new GridTable(table1);
        gridTable1.setGrid(new TestGrid(gridTable1));

        String[][] table2 = new String[3][2];
        table2[0][0] = "Datatype Type3";
        table2[0][1] = null;
        table2[1][0] = "Integer";
        table2[1][1] = "num";
        table2[2][0] = "Boolean";
        table2[2][1] = "flag";
        GridTable gridTable2 = new GridTable(table2);
        gridTable2.setGrid(new TestGrid(gridTable2));

        String[][] table3 = new String[3][2];
        table3[0][0] = "Datatype Type2";
        table3[0][1] = null;
        table3[1][0] = "Integer";
        table3[1][1] = "num";
        table3[2][0] = "Boolean";
        table3[2][1] = "flag";
        GridTable gridTable3 = new GridTable(table3);
        gridTable3.setGrid(new TestGrid(gridTable3));

        TableSyntaxNode[] ordered = new DatatypesSorter().sort(
                DatatypeHelper.createTypesMap(
                        new TableSyntaxNode[]{
                                getTableSyntaxNode(gridTable1, gridTable1.getRow(0)),
                                getTableSyntaxNode(gridTable2, gridTable2.getRow(0)),
                                getTableSyntaxNode(gridTable3, gridTable3.getRow(0))}),
                null);
        assertEquals(3, ordered.length);
        assertEquals("Datatype Type3", ordered[0].getHeader().getModule().getCode());
        assertEquals("Datatype Type2", ordered[1].getHeader().getModule().getCode());
        assertEquals("Datatype Type1", ordered[2].getHeader().getModule().getCode());
    }

    @Test
    public void testOrderDatatypes_fieldsDependency_Recursion() {
        String[][] table1 = new String[4][2];
        table1[0][0] = "Datatype Type1";
        table1[0][1] = null;
        table1[1][0] = "String";
        table1[1][1] = "name";
        table1[2][0] = "Type2";
        table1[2][1] = "type2Obj";

        // Added Recursion dependency
        //
        table1[3][0] = "Type1";
        table1[3][1] = "type1Obj";

        GridTable gridTable1 = new GridTable(table1);
        gridTable1.setGrid(new TestGrid(gridTable1));

        String[][] table2 = new String[3][2];
        table2[0][0] = "Datatype Type3";
        table2[0][1] = null;
        table2[1][0] = "Integer";
        table2[1][1] = "num";
        table2[2][0] = "Boolean";
        table2[2][1] = "flag";
        GridTable gridTable2 = new GridTable(table2);
        gridTable2.setGrid(new TestGrid(gridTable2));

        String[][] table3 = new String[3][2];
        table3[0][0] = "Datatype Type2";
        table3[0][1] = null;
        table3[1][0] = "Integer";
        table3[1][1] = "num";
        table3[2][0] = "Boolean";
        table3[2][1] = "flag";
        GridTable gridTable3 = new GridTable(table3);
        gridTable3.setGrid(new TestGrid(gridTable3));

        TableSyntaxNode[] ordered = new DatatypesSorter().sort(
                DatatypeHelper.createTypesMap(
                        new TableSyntaxNode[]{
                                getTableSyntaxNode(gridTable1, gridTable1.getRow(0)),
                                getTableSyntaxNode(gridTable2, gridTable2.getRow(0)),
                                getTableSyntaxNode(gridTable3, gridTable3.getRow(0))}),
                null);
        assertEquals(3, ordered.length);
        assertEquals("Datatype Type3", ordered[0].getHeader().getModule().getCode());
        assertEquals("Datatype Type2", ordered[1].getHeader().getModule().getCode());
        assertEquals("Datatype Type1", ordered[2].getHeader().getModule().getCode());
    }

    @Test
    public void testOrderDatatypes_arrayFieldsDependency() {
        String[][] table1 = new String[3][2];
        table1[0][0] = "Datatype Type1";
        table1[0][1] = null;
        table1[1][0] = "String";
        table1[1][1] = "name";
        table1[2][0] = "Boolean";
        table1[2][1] = "boolVal";
        GridTable gridTable1 = new GridTable(table1);
        gridTable1.setGrid(new TestGrid(gridTable1));

        String[][] table2 = new String[3][2];
        table2[0][0] = "Datatype Type3";
        table2[0][1] = null;
        table2[1][0] = "Integer";
        table2[1][1] = "num";
        table2[2][0] = "Type2[]";
        table2[2][1] = "type2Array";
        GridTable gridTable2 = new GridTable(table2);
        gridTable2.setGrid(new TestGrid(gridTable2));

        String[][] table3 = new String[3][2];
        table3[0][0] = "Datatype Type2";
        table3[0][1] = null;
        table3[1][0] = "Integer";
        table3[1][1] = "num";
        table3[2][0] = "Boolean";
        table3[2][1] = "flag";
        GridTable gridTable3 = new GridTable(table3);
        gridTable3.setGrid(new TestGrid(gridTable3));

        TableSyntaxNode[] ordered = new DatatypesSorter().sort(
                DatatypeHelper.createTypesMap(
                        new TableSyntaxNode[]{
                                getTableSyntaxNode(gridTable1, gridTable1.getRow(0)),
                                getTableSyntaxNode(gridTable2, gridTable2.getRow(0)),
                                getTableSyntaxNode(gridTable3, gridTable3.getRow(0))}),
                null);
        assertEquals(3, ordered.length);
        assertEquals("Datatype Type2", ordered[0].getHeader().getModule().getCode());
        assertEquals("Datatype Type3", ordered[1].getHeader().getModule().getCode());
        assertEquals("Datatype Type1", ordered[2].getHeader().getModule().getCode());
    }

    private TableSyntaxNode createStubTSN(final String datatypeHeader) {
        IGridTable gridTable = new org.openl.rules.table.GridTable(1, 1, 1, 1, null);


        IGridTable headerCell = new org.openl.rules.table.GridTable(0, 0, 0, 0, null) {
            @Override
            public ICell getCell(int column, int row) {
                Cell cell = new Cell();
                cell.setStringValue(datatypeHeader);
                return cell;
            }
        };

        return getTableSyntaxNode(gridTable, headerCell);
    }

    private TableSyntaxNode getTableSyntaxNode(IGridTable gridTable, IGridTable headerCell) {
        GridLocation pos = new GridLocation(gridTable);

        GridCellSourceCodeModule headerSrc = new GridCellSourceCodeModule(headerCell);

        HeaderSyntaxNode header = new HeaderSyntaxNode(headerSrc, new IdentifierNode(null, null,
                IXlsTableNames.DATATYPE_TABLE, null));
        return new TableSyntaxNode(XlsNodeTypes.XLS_DATATYPE.toString(), pos, null, gridTable, header);
    }

    /**
     * Stub implementation for the IGrid
     * just getCell is implemented
     * to avoid NPE
     */
    private class TestGrid extends AGrid {

        private IGridTable table;

        public TestGrid(IGridTable table) {
            this.table = table;
        }

        @Override
        public ICell getCell(int column, int row) {
            return table.getCell(column, row);
        }

        @Override
        public int getColumnWidth(int col) {
            return 0;
        }

        @Override
        public int getMaxColumnIndex(int row) {
            return 0;
        }

        @Override
        public int getMaxRowIndex() {
            return 0;
        }

        @Override
        public IGridRegion getMergedRegion(int i) {
            return null;
        }

        @Override
        public int getMinColumnIndex(int row) {
            return 0;
        }

        @Override
        public int getMinRowIndex() {
            return 0;
        }

        @Override
        public int getNumberOfMergedRegions() {
            return 0;
        }

        @Override
        public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {
            return null;
        }

        @Override
        public String getUri() {
            return null;
        }

        @Override
        public boolean isEmpty(int col, int row) {
            return false;
        }
    }
}
