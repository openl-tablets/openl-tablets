package org.openl.rules.table;

import org.junit.Test;
import org.openl.rules.datatype.binding.GridTable;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by ymolchan on 14.07.2014.
 */
public class GridTableUtilsTest {

    @Test
    public void getGridRegionsSimple() {
        String[][] arr = new String[2][2];
        arr[0][0] = "cell_0_0";
        arr[0][1] = "cell_0_1";
        arr[1][0] = "cell_1_0";
        arr[1][1] = "cell_1_1";
        IGridTable table = new GridTable(arr);
        List<IGridRegion> regions = GridTableUtils.getGridRegions(table);
        assertEquals(4, regions.size());
        List<IGridRegion> expected = Arrays.asList(sr(0, 0), sr(0, 1), sr(1, 0), sr(1, 1));
        assertEquals(expected, regions);
    }

    @Test
    public void getGridRegionsFullMeged() {
        String[][] arr = new String[2][2];
        arr[0][0] = "cell_0_0";
        IGridTable table = new GridTable(arr);
        List<IGridRegion> regions = GridTableUtils.getGridRegions(table);
        assertEquals(1, regions.size());
        List<IGridRegion> expected = Arrays.asList(mr(0, 0, 1, 1));
        assertEquals(expected, regions);
    }

    @Test
    public void getGridRegionsHorizontalMerged() {
        String[][] arr = new String[2][2];
        arr[0][0] = "cell_0_0";
        arr[1][0] = "cell_1_0";
        IGridTable table = new GridTable(arr);
        List<IGridRegion> regions = GridTableUtils.getGridRegions(table);
        assertEquals(2, regions.size());
        List<IGridRegion> expected = Arrays.asList(mr(0, 0, 0, 1), mr(1, 0, 1, 1));
        assertEquals(expected, regions);
    }

    @Test
    public void getGridRegionsVerticalMerged() {
        String[][] arr = new String[2][2];
        arr[0][0] = "cell_0_0";
        arr[0][1] = "cell_0_1";
        IGridTable table = new GridTable(arr);
        List<IGridRegion> regions = GridTableUtils.getGridRegions(table);
        assertEquals(2, regions.size());
        List<IGridRegion> expected = Arrays.asList(mr(0, 0, 1, 0), mr(0, 1, 1, 1));
        assertEquals(expected, regions);
    }

    // Simple Region
    private static IGridRegion sr(int top, int left) {
        return new GridRegion(top, left, top, left);
    }

    // Merged Region
    private static IGridRegion mr(int top, int left, int bottom, int right) {
        return new GridRegion(top, left, bottom, right);
    }
}
