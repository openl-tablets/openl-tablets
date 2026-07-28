package org.openl.rules.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.URLSourceCodeModule;

class LookupTest {

    @Test
    void testMergeBounds() throws Exception {

        var url = this.getClass().getClassLoader().getResource("org/openl/rules/table/TestLookup.xls");

        var source = new URLSourceCodeModule(url);
        var wbsrc = new XlsWorkbookSourceCodeModule(source);
        var wb = wbsrc.getWorkbook();

        var nsheets = wb.getNumberOfSheets();

        for (var i = 0; i < nsheets; i++) {

            var name = wb.getSheetName(i);

            var sheetSrc = new XlsSheetSourceCodeModule(i, wbsrc);
            var xsGrid = new XlsSheetGridModel(sheetSrc);

            var tables = xsGrid.getTables();

            if (name.equals("Sheet1")) {
                testSheet1(tables);
            }
        }
    }

    @Test
    private void testSheet1(IGridTable[] tables) {
        assertEquals(2, tables.length);

        ILogicalTable lookupTable = LogicalTableHelper.logicalTable(tables[0]);
        var t1 = lookupTable.getRows(1);
        var lookupRow1 = t1.getRow(0);
        var t2 = t1.getRows(1);
        var lookupColumn1 = t2.getColumn(0);
        ILogicalTable body = LogicalTableHelper.mergeBounds(lookupColumn1, lookupRow1);

        assertEquals(5, body.getHeight());
        assertEquals(3, body.getWidth());

        assertEquals("1", body.getSubtable(0, 0, 1, 1).getSource().getCell(0, 0).getStringValue());
        assertEquals("7", body.getSubtable(0, 2, 1, 1).getSource().getCell(0, 0).getStringValue());
        assertEquals("15", body.getSubtable(2, 4, 1, 1).getSource().getCell(0, 0).getStringValue());
    }
}
