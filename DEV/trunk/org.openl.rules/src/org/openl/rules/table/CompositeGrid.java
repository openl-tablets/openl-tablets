/**
 * Created Apr 27, 2007
 */
package org.openl.rules.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author snshor
 *
 */
public class CompositeGrid extends AGrid {   

    private IGridTable[] gridTables;

    private IGridRegion[] mappedRegions;

    private IGridRegion[] mergedRegions;
    
    private boolean vertical;

    private int width = 0;

    private int height = 0;

    public CompositeGrid(IGridTable[] tables, boolean vertical) {
        gridTables = tables;
        this.vertical = vertical;
        init();
    }

    public IGridTable asGridTable() {
        return new GridTable(0, 0, height - 1, width - 1, this);
    }

    public ICell getCell(int column, int row) {
        Transform t = transform(column, row);
        if (t == null) {
            return null;
        }

        ICell delegate = t.grid().getCell(t.getCol(), t.getRow());

        return new CompositeCell(column, row, getRegionContaining(column, row), delegate);
    }

    public int getColumnWidth(int col) {
        Transform t = transform(col, 0);
        return t == null ? 100 : t.grid().getColumnWidth(t.getCol());
    }

    public int getHeight() {
        return height;
    }

    public int getMaxColumnIndex(int row) {
        return width - 1;
    }

    public int getMaxRowIndex() {
        return height - 1;
    }

    public IGridRegion getMergedRegion(int i) {
        return mergedRegions[i];
    }

    public int getMinColumnIndex(int row) {
        return 0;
    }

    public int getMinRowIndex() {
        return 0;
    }

    public int getNumberOfMergedRegions() {
        return mergedRegions.length;
    }

    public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {
        Transform t1 = transform(colStart, rowStart);
        Transform t2 = transform(colEnd, rowEnd);
        if (t1 == null || t2 == null) {
            return null;
        }
        if (t1.grid() != t2.grid()) {
            return null;
        }
        return t1.grid().getRangeUri(t1.getCol(), t1.getRow(), t2.getCol(), t2.getRow());

    }

    public String getUri() {
        Transform t = transform(0, 0);
        return t == null ? null : t.grid().getUri();
    }

    public int getWidth() {
        return width;
    }

    private void init() {
        for (int i = 0; i < gridTables.length; i++) {
            IGridRegion reg = gridTables[i].getRegion();
            if (vertical) {
                height += IGridRegion.Tool.height(reg);
                width = Math.max(width, IGridRegion.Tool.width(reg));
            } else {
                width += IGridRegion.Tool.width(reg);
                height = Math.max(height, IGridRegion.Tool.height(reg));
            }

        }

        mappedRegions = new GridRegion[gridTables.length];

        int w = 0;
        int h = 0;

        for (int i = 0; i < gridTables.length; i++) {
            IGridRegion reg = gridTables[i].getRegion();
            GridRegion mapped = null;
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

        HashSet<IGrid> gridSet = new HashSet<IGrid>();

        for (int i = 0; i < gridTables.length; i++) {
            gridSet.add(gridTables[i].getGrid());
        }

        ArrayList<IGridRegion> mergedRegionsList = new ArrayList<IGridRegion>();

        for (Iterator<IGrid> iter = gridSet.iterator(); iter.hasNext();) {
            IGrid grid = iter.next();

            int n = grid.getNumberOfMergedRegions();
            for (int i = 0; i < n; i++) {
                IGridRegion m = grid.getMergedRegion(i);

                for (int j = 0; j < gridTables.length; j++) {
                    if (gridTables[j].getGrid() != grid) {
                        continue;
                    }
                    IGridRegion reg = gridTables[j].getRegion();

                    IGridRegion intersection = IGridRegion.Tool.intersect(m, reg);
                    if (intersection != null) {
                        int dx = mappedRegions[j].getLeft() - reg.getLeft();
                        int dy = mappedRegions[j].getTop() - reg.getTop();
                        IGridRegion moved = IGridRegion.Tool.move(intersection, dx, dy);
                        mergedRegionsList.add(moved);
                    }
                }
            }

        }

        mergedRegions = mergedRegionsList.toArray(new IGridRegion[0]);
    }

    public boolean isEmpty(int col, int row) {
        Transform t = transform(col, row);
        return t == null || t.grid().isEmpty(t.getCol(), t.getRow());
    }

    public Transform transform(int col, int row) {
        for (int i = 0; i < mappedRegions.length; i++) {
            if (IGridRegion.Tool.contains(mappedRegions[i], col, row)) {
                IGridRegion reg = gridTables[i].getRegion();
                return new Transform(gridTables[i], reg.getLeft() + col - mappedRegions[i].getLeft(), reg.getTop()
                        + row - mappedRegions[i].getTop());
            }
        }

        return null;
    }
    
    private static class Transform {
        private IGridTable gridTable;
        private int col;
        private int row;

        public Transform(IGridTable table, int col, int row) {
            gridTable = table;
            this.col = col;
            this.row = row;
        }

        public IGrid grid() {
            return gridTable.getGrid();
        }

        public int getCol() {
            return col;
        }

        public int getRow() {
            return row;
        }
        
    }

}
