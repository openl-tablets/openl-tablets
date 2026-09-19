package org.openl.rules.table.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.URLSourceCodeModule;

class RemoveMergedRegionsActionTest {

    private XlsSheetGridModel grid;
    private IGridTable table;

    @BeforeEach
    void before() {
        var source = new URLSourceCodeModule("./test/rules/XlsSheetGridModelTest.xls");
        var workbook = new XlsWorkbookSourceCodeModule(source);
        var sheet = new XlsSheetSourceCodeModule(0, workbook);
        grid = new XlsSheetGridModel(sheet);
        table = grid.getTables()[0];
    }

    @Test
    void removesMergesWithinRegion() {
        assertEquals(13, grid.getNumberOfMergedRegions());
        // A merged region is known to cover this cell in the fixture.
        assertNotNull(grid.getRegionContaining(3, 2));

        new RemoveMergedRegionsAction(new GridRegion(0, 0, 1000, 1000)).doAction(table);

        assertEquals(0, grid.getNumberOfMergedRegions());
        assertNull(grid.getRegionContaining(3, 2));
    }

    @Test
    void keepsMergesWhoseOriginIsOutsideRegion() {
        var initial = grid.getNumberOfMergedRegions();

        // A region far from any merged region's top-left cell removes nothing.
        new RemoveMergedRegionsAction(new GridRegion(500, 500, 510, 510)).doAction(table);

        assertEquals(initial, grid.getNumberOfMergedRegions());
    }
}
