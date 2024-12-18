/**
 * Created Apr 27, 2007
 */
package org.openl.rules.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.table.xls.XlsSheetGridModel;

/**
 * An {@link IGrid} implementation that composes several {@link IGridTable} together.<br>
 * It is possible to compose from top to bottom and from left to right by {@link #vertical} flag.<br>
 * Tables are composing one by one, without gaps.<br>
 *
 * @author snshor
 */
public class CompositeGrid extends AGrid {

    private final IGridTable[] gridTables;

    /**
     * Regions on current grid, to which each grid table belongs to. So, the first gridTable belongs to first
     * mappedRegion
     */
    private IGridRegion[] mappedRegions;

    private IGridRegion[] mergedRegions;

    /**
     * Indicates in which direction we are going to compose the tables. If true, we are going to compose up to down. If
     * false, we are going to compose from left to right.
     */
    private final boolean vertical;

    private int width;

    private int height;

    public IGridTable[] getGridTables() {
        return gridTables;
    }

    public IGridRegion getMappedRegion(int i) {
        return mappedRegions[i];
    }

    /**
     * @param tables   Tables to be composed.
     * @param vertical see {@link #vertical}
     */
    public CompositeGrid(IGridTable[] tables, boolean vertical) {
        this.gridTables = tables;
        this.vertical = vertical;
        init();
    }

    @Override
    public ICell getCell(int column, int row) {
        Transform t = transform(column, row);
        if (t == null) {
            return null;
        }
        ICell delegate = t.grid().getCell(t.getCol(), t.getRow());
        return new CompositeCell(column, row, getRegionContaining(column, row), delegate, t.getGridTable());
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
        if (t1.grid() != t2.grid() || isGenerated(t1.grid()) || isGenerated(t2.grid())) {
            // try to find some range in real table
            int h = 0;
            int w = 0;
            for (IGridTable gridTable : gridTables) {
                if (isGenerated(gridTable.getGrid())) {
                    h = h + gridTable.getHeight();
                    w = w + gridTable.getWidth();
                } else {
                    Transform g1;
                    Transform g2;
                    if (vertical) {
                        g1 = transform(colStart, h);
                        g2 = transform(colEnd, h);
                    } else {
                        g1 = transform(w, rowStart);
                        g2 = transform(w, rowEnd);
                    }
                    return g1.grid().getRangeUri(g1.getCol(), g1.getRow(), g2.getCol(), g2.getRow());
                }
            }
        }
        return t1.grid().getRangeUri(t1.getCol(), t1.getRow(), t2.getCol(), t2.getRow());
    }

    private boolean isGenerated(IGrid grid) {
        if (grid instanceof XlsSheetGridModel) {
            return ((XlsSheetGridModel) grid).getSheetSource().getWorkbookSource().getUri() == null;
        }
        return false;
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
     */
    private void initMergedRegions() {
        List<IGridRegion> mergedRegionsList = new ArrayList<>();

        // hash set of source grids for every table
        Set<IGrid> gridSet = getGridSet();

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
                        if (!gridTables[j].isNormalOrientation()) {
                            int left = intersection.getTop();
                            int top = intersection.getLeft();

                            int right = intersection.getBottom();
                            int bottom = intersection.getRight();

                            intersection = new GridRegion(top, left, bottom, right);
                        }
                        // there is an intersection between merged region and
                        // table region
                        // and we need to move merged region to current grid.
                        // calculate horizontal and vertical steps for moving.
                        int dx;
                        int dy;
                        if (!gridTables[j].isNormalOrientation()) {
                            dx = mappedRegions[j].getLeft() - tableRegion.getTop();
                            dy = mappedRegions[j].getTop() - tableRegion.getLeft();
                        } else {
                            dx = mappedRegions[j].getLeft() - tableRegion.getLeft();
                            dy = mappedRegions[j].getTop() - tableRegion.getTop();
                        }

                        // move intersection from one place to another.
                        IGridRegion moved = IGridRegion.Tool.move(intersection, dx, dy);
                        mergedRegionsList.add(moved);
                    }
                }
            }
        }
        mergedRegions = mergedRegionsList.toArray(IGridRegion.EMPTY_REGION);
    }

    private Set<IGrid> getGridSet() {
        Set<IGrid> gridSet = new HashSet<>();
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
    public Transform transform(int col, int row) {
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
                return new Transform(gridTables[i].getGrid(), gridTables[i], transformedCol, transformedRow);
            }
        }

        return null;
    }

    /**
     * Handles the grid and coordinates of the cell in this grid.
     */
    protected static class Transform {
        /**
         * grid
         */
        private final IGrid grid;

        /**
         * column index on grid
         */
        private final int col;

        /**
         * row index on grid.
         */
        private final int row;

        private final IGridTable gridTable;

        public Transform(IGrid grid, IGridTable gridTable, int col, int row) {
            this.grid = grid;
            this.gridTable = gridTable;
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

        public IGridTable getGridTable() {
            return gridTable;
        }
    }
}
