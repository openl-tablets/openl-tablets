/**
 * Created Apr 27, 2007
 */
package org.openl.rules.table;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import org.openl.rules.table.ui.FormattedCell;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

/**
 * @author snshor
 *
 */
public class CompositeGrid extends AGridModel {

    // static final public String VERTICAL_DIR = "Vertical", HORIZONTAL_DIR =
    // "Horizontal";

    static class CompositeCellInfo implements ICellInfo {

        ICellInfo delegate;
        IGridRegion region;
        int column, row;

        public CompositeCellInfo(ICellInfo delegate, IGridRegion region, int column, int row) {
            this.delegate = delegate;
            this.region = region;
            this.column = column;
            this.row = row;
        }

        public ICellStyle getCellStyle() {
            return delegate.getCellStyle();
        }

        public int getColumn() {
            return column;
        }

        public ICellFont getFont() {
            return delegate.getFont();
        }

        public int getRow() {
            return row;
        }

        public IGridRegion getSurroundingRegion() {
            return region;
        }

        public boolean isTopLeft() {
            return region != null && region.getLeft() == column && region.getTop() == row;
        }

    }
    static class Transform {
        IGridTable gridTable;
        int col;
        int row;

        public Transform(IGridTable table, int col, int row) {
            gridTable = table;
            this.col = col;
            this.row = row;
        }

        IGrid grid() {
            return gridTable.getGrid();
        }
    }

    IGridTable[] gridTables;

    IGridRegion[] mappedRegions;

    IGridRegion[] mergedRegions;
    boolean vertical;

    int width = 0;

    int height = 0;

    public CompositeGrid(IGridTable[] tables, boolean vertical) {
        gridTables = tables;
        this.vertical = vertical;
        init();
    }

    public IGridTable asGridTable() {
        return new GridTable(0, 0, height - 1, width - 1, this);
    }

    public int getCellHeight(int column, int row) {
        Transform t = transform(column, row);
        return t == null ? 1 : t.grid().getCellHeight(t.col, t.row);
    }

    public ICellInfo getCellInfo(int column, int row) {
        Transform t = transform(column, row);
        if (t == null) {
            return null;
        }

        ICellInfo delegate = t.grid().getCellInfo(t.col, t.row);

        return new CompositeCellInfo(delegate, getGridRegionContaining(column, row), column, row);
    }

    public ICellStyle getCellStyle(int column, int row) {
        Transform t = transform(column, row);
        return t == null ? null : t.grid().getCellStyle(t.col, t.row);
    }

    public int getCellType(int column, int row) {
        Transform t = transform(column, row);
        return t == null ? CELL_TYPE_BLANK : t.grid().getCellType(t.col, t.row);
    }

    public String getCellUri(int column, int row) {
        Transform t = transform(column, row);
        return t == null ? null : t.grid().getCellUri(t.col, t.row);
    }

    public int getCellWidth(int column, int row) {
        Transform t = transform(column, row);
        return t == null ? 1 : t.grid().getCellWidth(t.col, t.row);
    }

    public int getColumnWidth(int col) {
        Transform t = transform(col, 0);
        return t == null ? 100 : t.grid().getColumnWidth(t.col);
    }

    public Date getDateCellValue(int column, int row) {
        Transform t = transform(column, row);
        return t == null ? null : t.grid().getDateCellValue(t.col, t.row);
    }

    public double getDoubleCellValue(int column, int row) {
        Transform t = transform(column, row);
        return t == null ? 0 : t.grid().getDoubleCellValue(t.col, t.row);
    }

    public FormattedCell getFormattedCell(int column, int row) {
        Transform t = transform(column, row);
        return t == null ? null : t.grid().getFormattedCell(t.col, t.row);
    }

    public String getFormattedCellValue(int column, int row) {
        Transform t = transform(column, row);
        return t == null ? null : t.grid().getFormattedCellValue(t.col, t.row);
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

    public Object getObjectCellValue(int column, int row) {
        Transform t = transform(column, row);
        return t == null ? null : t.grid().getObjectCellValue(t.col, t.row);
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
        return t1.grid().getRangeUri(t1.col, t1.row, t2.col, t2.row);

    }

    public String getStringCellValue(int column, int row) {
        Transform t = transform(column, row);
        return t == null ? null : t.grid().getStringCellValue(t.col, t.row);
    }

    public String getUri() {
        Transform t = transform(0, 0);
        return t == null ? null : t.grid().getUri();
    }

    public int getWidth() {
        return width;
    }

    void init() {
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
        return t == null || t.grid().isEmpty(t.col, t.row);
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

}
