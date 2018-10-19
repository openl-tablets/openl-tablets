package org.openl.rules.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.Test;
import org.openl.binding.impl.MethodUsagesSearcher.MethodUsage;
import org.openl.binding.impl.NodeUsage;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IBaseAction;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.types.IOpenMethod;

import static org.junit.Assert.*;

public class MethodUsagesMetaInfoTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/binding/MethodUsagesMetaInfoTest.xlsx";

    public MethodUsagesMetaInfoTest() {
        super(SRC);
    }

    @Test
    public void testMetaInfoInDT() {
        // method in return expression
        TableSyntaxNode firstTable = findTable("Rules String testDT(int arg)", null);
        ICell returnExpressionCell = firstTable.getGridTable().getCell(1, 2);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(firstTable.getMetaInfoReader(),
                returnExpressionCell)));
        // method in return values
        TableSyntaxNode secondTable = findTable("Rules String testDT(int arg)", "test");
        MetaInfoReader secondMetaReader = secondTable.getMetaInfoReader();
        ICell firstRetCell = secondTable.getGridTable().getCell(1, 7);
        ICell secondRetCell = secondTable.getGridTable().getCell(1, 8);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(secondMetaReader, firstRetCell)));
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(secondMetaReader, secondRetCell)));
    }

    @Test
    public void testMetaInfoInDispatcherTable() {
        TableSyntaxNode dispatcherTable = findDispatcherForMethod("testDT");
        MetaInfoReader metaInfoReader = dispatcherTable.getMetaInfoReader();
        IBaseAction returnColumn = ((DecisionTable) dispatcherTable.getMember()).getActionRows()[0];

        ICell firstMethodCell = returnColumn.getValueCell(0).getSource().getCell(0, 0);
        CellMetaInfo firstMethodMeta = getMetaInfo(metaInfoReader, firstMethodCell);
        IOpenMethod firstMethodInOveloading = ((MethodUsage) firstMethodMeta.getUsedNodes().get(0)).getMethod();

        ICell secondMethodCell = returnColumn.getValueCell(1).getSource().getCell(0, 0);
        CellMetaInfo secondMethodMeta =  getMetaInfo(metaInfoReader, secondMethodCell);
        IOpenMethod secondMethodInOveloading = ((MethodUsage) secondMethodMeta.getUsedNodes().get(0)).getMethod();
        assertNotSame(firstMethodInOveloading.getInfo().getSourceUrl(), secondMethodInOveloading.getInfo()
                .getSourceUrl());
    }

    @Test
    public void testMetaInfoInTBasic() {
        TableSyntaxNode table = findTable("TBasic String testTBasic()");
        IGridTable spreadsheetGrid = table.getGridTable();
        ICell cell = spreadsheetGrid.getCell(4, 6);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(table.getMetaInfoReader(), cell)));
    }

    @Test
    public void testMetaInfoInSpreadsheet() {
        TableSyntaxNode table = findTable("Spreadsheet SpreadsheetResult testSpreadsheet()");
        IGridTable spreadsheetGrid = table.getGridTable();
        ICell cell = spreadsheetGrid.getCell(1, 3);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(getMetaInfo(table.getMetaInfoReader(), cell)));
    }

    @Test
    public void testMetaInfoMultiSourceMethodTable() {
        TableSyntaxNode table = findTable("Method String testMethod()");
        MetaInfoReader metaInfoReader = table.getMetaInfoReader();

        IGridTable methodTableGrid = table.getGridTable();
        List<String> usedMethods = new ArrayList<>(2);
        int cellWithMethodUsagesCount = 0;
        for (int row = 0; row < methodTableGrid.getHeight(); row++) {
            for (int col = 0; col < methodTableGrid.getWidth(); col++) {
                ICell cell = methodTableGrid.getCell(col, row);
                CellMetaInfo metaInfo = getMetaInfo(metaInfoReader, cell);
                if (CellMetaInfo.isCellContainsNodeUsages(metaInfo)) {
                    for (NodeUsage methodUsage : metaInfo.getUsedNodes()) {
                        cellWithMethodUsagesCount++;
                        if (methodUsage instanceof MethodUsage) {
                            usedMethods.add(((MethodUsage)methodUsage).getMethod().getName());
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
                String[] tableLobs = tsn.getTableProperties().getLob();
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
