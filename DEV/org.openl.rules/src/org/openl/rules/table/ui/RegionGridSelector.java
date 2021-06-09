/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui;

import org.openl.rules.table.IGridRegion;

/**
 * @author snshor
 *
 */
public class RegionGridSelector implements IGridSelector {

    private final IGridRegion[] regions;

    private final boolean exclude;

    public RegionGridSelector(IGridRegion region, boolean exclude) {
        regions = new IGridRegion[] { region };
        this.exclude = exclude;
    }

    public RegionGridSelector(IGridRegion[] regions, boolean exclude) {
        this.regions = regions;
        this.exclude = exclude;
    }

    private boolean contains(int col, int row) {
        for (IGridRegion region : regions) {
            if (IGridRegion.Tool.contains(region, col, row)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean selectCoords(int col, int row) {
        return contains(col, row) ^ exclude;
    }

}
