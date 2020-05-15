/**
 * Created Apr 27, 2007
 */
package org.openl.rules.table;

import java.util.ArrayList;
import java.util.HashSet;

import org.openl.rules.table.properties.PropertiesHelper;

/**
 * An {@link IGrid} implementation that composes several {@link IGridTable} together.<br>
 * It is possible to compose from top to bottom and from left to right by {@link #vertical} flag.<br>
 * Tables are composing one by one, without gaps.<br>
 *
 * @author snshor
 *
 */
public class CompositeGrid extends AGrid {

    private IGridTable[] gridTables;

    /**
     * Regions on current grid, to which each grid table belongs to. So, the first gridTable belongs to first
     * mappedRegion
     *
     */
    private IGridRegion[] mappedRegions;

    private IGridRegion[] mergedRegions;

    /**
     * Indicates in which direction we are going to compose the tables. If true, we are going to compose up to down. If
     * false, we are going to compose from left to right.
     */
    private boolean vertical;

    private int width = 0;

    private int height = 0;

    public IGridTable[] getGridTables() {
        return gridTables;
    }

    public IGridRegion getMappedRegion(int i) {
        return mappedRegions[i];
    }

    /**
     *
     * @param tables Tables to be composed.
     * @param vertical see {@link #vertical}
     *
     */
    public CompositeGrid(IGridTable[] tables, boolean vertical) {
        gridTables = tables;
        this.vertical = vertical;
        init();
    }

    @Override
    public ICell getCell(int column, int row) {
        if (!vertical) { // Merge header for horizontal table parts
            Transform t = transform(0, 0);
            if (t == null) {
                return null;
            }
            ICell delegate = t.grid().getCell(t.getCol(), t.getRow());
            if (row < delegate.getHeight()) {
                IGridRegion reg = getRegionContaining(0, 0);
                IGridRegion region;
                if (reg != null) {
                    region = new GridRegion(reg.getTop(),
                        reg.getLeft(),
                        reg.getBottom(),
                        reg.getLeft() + getWidth() - 1);
                } else {
                    region = new GridRegion(row, column, row, column + getWidth() - 1);
                }
                return new CompositeCell(column, row, region, delegate);
            } else {
                Transform t1 = transform(0, delegate.getHeight());// Properties parsing and merge
                if (t1 != null) {
                    ICell delegate1 = t1.grid().getCell(t1.getCol(), t1.getRow());
                    if (row < delegate.getHeight() + delegate1.getHeight() && PropertiesHelper.PROPERTIES_HEADER
                        .equals(delegate1.getStringValue())) {
                        Transform t2 = transform(delegate1.getWidth(), row);
                        if (t2 != null) {
                            ICell delegate2 = t2.grid().getCell(t2.getCol(), t2.getRow());
                            Transform t3 = transform(delegate1.getWidth() + delegate2.getWidth(), row);
                            if (t3 != null) {
                                ICell delegate3 = t3.grid().getCell(t3.getCol(), t3.getRow());
                                if (column >= delegate1.getWidth() + delegate2.getWidth()) {
                                    IGridRegion reg = getRegionContaining(delegate1.getWidth() + delegate2.getWidth(),
                                        row);
                                    IGridRegion region;
                                    if (reg != null) {
                                        region = new GridRegion(reg.getTop(),
                                            reg.getLeft(),
                                            reg.getBottom(),
                                            reg.getLeft() + getWidth() - 1 - (delegate1.getWidth() + delegate2
                                                .getWidth()));
                                    } else {
                                        region = new GridRegion(row,
                                            column,
                                            row,
                                            column + getWidth() - 1 - (delegate1.getWidth() + delegate2.getWidth()));
                                    }
                                    return new CompositeCell(column, row, region, delegate3);
                                }
                            }
                        }
                    }
                }
            }
        }
        Transform t = transform(column, row);
        if (t == null) {
            return null;
        }
        ICell delegate = t.grid().getCell(t.getCol(), t.getRow());
        return new CompositeCell(column, row, getRegionContaining(column, row), delegate);
    }

    @Override
    public int getColumnWidth(int col) {
        Transform t = transform(col, 0);
        return t == null ? 100 : t.grid().getColumnWidth(t.getCol());
    }

    public int getHeight() {
        return height;
    }

    @Override
    public int getMaxColumnIndex(int row) {
        return width - 1;
    }

    @Override
    public int getMaxRowIndex() {
        return height - 1;
    }

    @Override
    public IGridRegion getMergedRegion(int i) {
        return mergedRegions[i];
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
        return mergedRegions.length;
    }

    @Override
    public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {
        Transform t1 = transform(colStart, rowStart);
        Transform t2 = transform(colEnd, rowEnd);
        if (t1 == null || t2 == null) {
            return null;
        }
        if (t1.grid() != t2.grid()) {
            for (IGridTable gridTable : gridTables) {
                // check if table belongs to grid
                if (gridTable.getGrid() != t2.grid()) {
                    continue;
                }
                IGridRegion region = gridTable.getRegion();
                return t2.grid().getRangeUri(region.getLeft(), region.getTop(), region.getRight(), region.getBottom());
            }
        }
        return t1.grid().getRangeUri(t1.getCol(), t1.getRow(), t2.getCol(), t2.getRow());
    }

    @Override
    public String getUri() {
        Transform t = transform(0, 0);
        return t == null ? null : t.grid().getUri();
    }

    public int getWidth() {
        return width;
    }

    private void init() {
        initWidthAndHeight();

        initMappedRegions();

        initMergedRegions();
    }

    /**
     * If there were merged regions on the source table grids, that were belonging to tables we need to move it to
     * current grid.
     *
     */
    private void initMergedRegions() {
        ArrayList<IGridRegion> mergedRegionsList = new ArrayList<>();

        // hash set of source grids for every table
        HashSet<IGrid> gridSet = getGridSet();

        for (IGrid grid : gridSet) {
            for (int i = 0; i < grid.getNumberOfMergedRegions(); i++) {

                // get each merged region from the grid
                IGridRegion mergedRegion = grid.getMergedRegion(i);

                for (int j = 0; j < gridTables.length; j++) {
                    // check if table belongs to grid
                    if (gridTables[j].getGrid() != grid) {
                        continue;
                    }

                    IGridRegion tableRegion = gridTables[j].getRegion();

                    // check if merged region on the sheet belongs to table
                    // region
                    IGridRegion intersection = IGridRegion.Tool.intersect(mergedRegion, tableRegion);
                    if (intersection != null) {
                        // there is an intersection between merged region and
                        // table region
                        // and we need to move merged region to current grid.
                        // calculate horizontal and vertical steps for moving.
                        int dx = mappedRegions[j].getLeft() - tableRegion.getLeft();
                        int dy = mappedRegions[j].getTop() - tableRegion.getTop();

                        // move intersection from one place to another.
                        IGridRegion moved = IGridRegion.Tool.move(intersection, dx, dy);
                        mergedRegionsList.add(moved);
                    }
                }
            }
        }
        mergedRegions = mergedRegionsList.toArray(IGridRegion.EMPTY_REGION);
    }

    private HashSet<IGrid> getGridSet() {
        HashSet<IGrid> gridSet = new HashSet<>();
        for (IGridTable gridTable : gridTables) {
            gridSet.add(gridTable.getGrid());
        }
        return gridSet;
    }

    /**
     * Gets the regions from grid tables, and according it initialize regions on current grid.
     */
    private void initMappedRegions() {
        mappedRegions = new GridRegion[gridTables.length];

        int w = 0;
        int h = 0;

        for (int i = 0; i < gridTables.length; i++) {
            IGridRegion reg = gridTables[i].getRegion();
            GridRegion mapped;
            int last = i == gridTables.length - 1 ? 1 : 0;

            if (vertical) {
                int rh = IGridRegion.Tool.height(reg);
                mapped = new GridRegion(h, 0, h + rh - 1 + last, width);

                h += rh;
            } else {
                int rw = IGridRegion.Tool.width(reg);
                mapped = new GridRegion(0, w, height, w + rw - 1 + last);

                w += rw;

            }
            mappedRegions[i] = mapped;
        }
    }

    /**
     * Initialize width and height of current grid. According to the compose direction.
     *
     */
    private void initWidthAndHeight() {
        for (IGridTable gridTable : gridTables) {
            IGridRegion reg = gridTable.getRegion();
            if (vertical) {
                height += IGridRegion.Tool.height(reg);
                width = Math.max(width, IGridRegion.Tool.width(reg));
            } else {
                width += IGridRegion.Tool.width(reg);
                height = Math.max(height, IGridRegion.Tool.height(reg));
            }
        }
    }

    @Override
    public boolean isEmpty(int col, int row) {
        Transform t = transform(col, row);
        return t == null || t.grid().isEmpty(t.getCol(), t.getRow());
    }

    /**
     * Transformes current grid coordinates to appropriate table coordinates.
     *
     * @param col grid column index
     * @param row grid row index
     * @return {@link Transform} that contains coordinates to cell in the appropriate grid.
     */
    private Transform transform(int col, int row) {
        for (int i = 0; i < mappedRegions.length; i++) {
            // find the region to which this coordinates belong to
            if (IGridRegion.Tool.contains(mappedRegions[i], col, row)) {
                // according to the found region, get the appropriate table
                // region.
                IGridRegion reg = gridTables[i].getRegion();

                // transform grid coordinates to table grid one.
                int transformedCol;
                int transformedRow;
                if (gridTables[i].isNormalOrientation()) {
                    transformedCol = reg.getLeft() + col - mappedRegions[i].getLeft();
                    transformedRow = reg.getTop() + row - mappedRegions[i].getTop();
                } else {
                    transformedCol = gridTables[i].transpose().getRegion().getLeft() + row - mappedRegions[i].getTop();
                    transformedRow = gridTables[i].transpose().getRegion().getTop() + col - mappedRegions[i].getLeft();
                }

                // return the transformer, with coordinates to source cell.
                return new Transform(gridTables[i].getGrid(), transformedCol, transformedRow);
            }
        }

        return null;
    }

    /**
     * Handles the grid and coordinates of the cell in this grid.
     *
     */
    private static class Transform {
        /**
         * grid
         */
        private IGrid grid;

        /**
         * column index on grid
         */
        private int col;

        /**
         * row index on grid.
         */
        private int row;

        public Transform(IGrid grid, int col, int row) {
            this.grid = grid;
            this.col = col;
            this.row = row;
        }

        public IGrid grid() {
            return grid;
        }

        public int getCol() {
            return col;
        }

        public int getRow() {
            return row;
        }
    }
}
