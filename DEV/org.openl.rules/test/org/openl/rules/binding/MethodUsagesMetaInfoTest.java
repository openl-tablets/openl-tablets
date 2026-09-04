package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.openl.binding.impl.MethodUsage;
import org.openl.binding.impl.NodeUsage;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;

class MethodUsagesMetaInfoTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/binding/MethodUsagesMetaInfoTest.xlsx";

    public MethodUsagesMetaInfoTest() {
        super(SRC);
    }

    @Test
    void testMetaInfoInDT() {
        // method in return expression
        var firstTable = findTable("Rules String testDT(int arg)", null);
        var returnExpressionCell = firstTable.getGridTable().getCell(1, 2);
        assertTrue(
                CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(firstTable.getMetaInfoReader(), returnExpressionCell)));
        // method in return values
        var secondTable = findTable("Rules String testDT(int arg)", "test");
        var secondMetaReader = secondTable.getMetaInfoReader();
        var firstRetCell = secondTable.getGridTable().getCell(1, 7);
        var secondRetCell = secondTable.getGridTable().getCell(1, 8);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(secondMetaReader, firstRetCell)));
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(secondMetaReader, secondRetCell)));
    }

    @Test
    void testMetaInfoInDispatcherTable() {
        var dispatcherTable = findDispatcherForMethod("testDT");
        var metaInfoReader = dispatcherTable.getMetaInfoReader();
        var returnColumn = ((DecisionTable) dispatcherTable.getMember()).getActionRows()[0];

        var firstMethodCell = returnColumn.getValueCell(0).getSource().getCell(0, 0);
        var firstMethodMeta = getMetaInfo(metaInfoReader, firstMethodCell);
        var firstMethodInOverloading = ((MethodUsage) firstMethodMeta.getUsedNodes().getFirst()).getMethod();

        var secondMethodCell = returnColumn.getValueCell(1).getSource().getCell(0, 0);
        var secondMethodMeta = getMetaInfo(metaInfoReader, secondMethodCell);
        var secondMethodInOverloading = ((MethodUsage) secondMethodMeta.getUsedNodes().getFirst()).getMethod();
        assertNotSame(firstMethodInOverloading.getInfo().getSourceUrl(),
                secondMethodInOverloading.getInfo().getSourceUrl());
    }

    @Test
    void testMetaInfoInDecisionTableWithMergedCells() {
        var testDT1Table = findTable("Rules String[] testDT1(int x)", null);
        int[][] retCells1 = {{1, 5}, {1, 6}};
        for (int[] cell : retCells1) {
            var retCell = testDT1Table.getGridTable().getCell(cell[0], cell[1]);
            assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(testDT1Table.getMetaInfoReader(), retCell)));
        }

        var testDT2Table = findTable("Rules String[] testDT2(int x)", null);
        int[][] retCells2 = {{1, 5}, {1, 6}, {2, 5}, {2, 6}};
        for (int[] cell : retCells2) {
            var retCell = testDT2Table.getGridTable().getCell(cell[0], cell[1]);
            assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(testDT2Table.getMetaInfoReader(), retCell)));
        }

        var testDT3Table = findTable("Rules String[][] testDT3(int x)", null);
        int[][] retCells3 = {{1, 5}, {1, 6}, {2, 5}, {2, 6}, {3, 5}, {3, 6}};
        for (int[] cell : retCells3) {
            var retCell = testDT3Table.getGridTable().getCell(cell[0], cell[1]);
            assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(testDT3Table.getMetaInfoReader(), retCell)));
        }

        var testDT4Table = findTable("Rules String[][] testDT4(int x)", null);
        int[][] retCells4 = {{1, 5}, {1, 6}, {1, 7}, {2, 5}, {2, 6}, {2, 7}};
        for (int[] cell : retCells4) {
            var retCell = testDT4Table.getGridTable().getCell(cell[0], cell[1]);
            assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(testDT4Table.getMetaInfoReader(), retCell)));
        }
    }

    @Test
    void testMetaInfoInTBasic() {
        var table = findTable("TBasic String testTBasic()");
        var spreadsheetGrid = table.getGridTable();
        var cell = spreadsheetGrid.getCell(4, 6);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(table.getMetaInfoReader(), cell)));
    }

    @Test
    void testMetaInfoInSpreadsheet() {
        var table = findTable("Spreadsheet SpreadsheetResult testSpreadsheet()");
        var spreadsheetGrid = table.getGridTable();
        var cell = spreadsheetGrid.getCell(1, 3);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(table.getMetaInfoReader(), cell)));
    }

    @Test
    void testMetaInfoMultiSourceMethodTable() {
        var table = findTable("Method String testMethod()");
        var metaInfoReader = table.getMetaInfoReader();

        var methodTableGrid = table.getGridTable();
        var usedMethods = new ArrayList<String>(2);
        var cellWithMethodUsagesCount = 0;
        for (var row = 0; row < methodTableGrid.getHeight(); row++) {
            for (var col = 0; col < methodTableGrid.getWidth(); col++) {
                var cell = methodTableGrid.getCell(col, row);
                var metaInfo = getMetaInfo(metaInfoReader, cell);
                if (CellMetaInfo.isCellContainsNodeUsages(metaInfo)) {
                    for (NodeUsage methodUsage : metaInfo.getUsedNodes()) {
                        cellWithMethodUsagesCount++;
                        if (methodUsage instanceof MethodUsage usage) {
                            usedMethods.add(usage.getMethod().getName());
                        }
                    }
                }
            }
        }
        assertEquals(8, cellWithMethodUsagesCount);
        assertEquals(2, usedMethods.size());
        assertTrue(usedMethods.contains("testTBasic"));
        assertTrue(usedMethods.contains("testDT"));
    }

    private TableSyntaxNode findTable(String name, String lob) {
        for (TableSyntaxNode tsn : getTableSyntaxNodes()) {
            if (name.equals(tsn.getDisplayName())) {
                var tableLobs = tsn.getTableProperties().getLob();
                if (contains(tableLobs, lob)) {
                    return tsn;
                }
            }
        }
        throw new RuntimeException("unreachable code");
    }

    private CellMetaInfo getMetaInfo(MetaInfoReader metaInfoReader, ICell cell) {
        return metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
    }

    private boolean contains(String[] sourceArr, String target) {
        if (sourceArr == null) {
            return target == null;
        } else {
            for (String source : sourceArr) {
                if (Objects.equals(target, source)) {
                    return true;
                }
            }
        }
        return false;
    }
}
