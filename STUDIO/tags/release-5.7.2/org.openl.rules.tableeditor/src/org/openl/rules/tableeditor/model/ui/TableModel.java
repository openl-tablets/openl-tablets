package org.openl.rules.tableeditor.model.ui;

import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.ui.filters.SimpleHtmlFilter;
import org.openl.rules.table.ui.filters.SimpleFormatFilter;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;

public class TableModel {

    private static final String EMPTY =
        "<td width=\"50\" style=\"border-style: dashed;border-width:1px; border-color: #C0C0FF\">&nbsp;</td>";

    private static final String EMPTY_TR =
        "<td style=\"border-style: dashed dashed none none;border-width:1px; border-color: #C0C0FF\">&nbsp;</td>";

    private static final String EMPTY_TRB =
        "<td width=\"50\" style=\"border-style: dashed dashed dashed none;border-width:1px; border-color: #C0C0FF\">&nbsp;</td>";

    private static final String EMPTY_BL =
        "<td width=\"50\" style=\"border-style: none none dashed dashed;border-width:1px; border-color: #C0C0FF\">&nbsp;</td>";

    private static final String EMPTY_RB =
        "<td width=\"50\" style=\"border-style: none dashed dashed none;border-width:1px; border-color: #C0C0FF\">&nbsp;</td>";

    private static final String EMPTY_RBL =
        "<td width=\"50\" style=\"border-style: none dashed dashed dashed;border-width:1px; border-color: #C0C0FF\">&nbsp;</td>";

    private ICellModel[][] cells;

    private String attributes = "cellspacing=\"0\" cellpadding=\"1\"";

    private IGridTable gridTable;

    public static TableModel initializeTableModel(IGridTable table) {
        return initializeTableModel(table, null);
    }

    public static TableModel initializeTableModel(IGridTable table, IGridFilter[] filters) {
        if (table == null) {
            return null;
        }
        boolean filtered = filters != null && filters.length > 0;
        IGrid htmlGrid = table.getGrid();
        if (!(htmlGrid instanceof FilteredGrid)) {
            int N = 1;
            IGridFilter[] f1 = new IGridFilter[!filtered ? (N + 1) : (N + filters.length)];
            f1[0] = new SimpleFormatFilter();
            if (!filtered) {
                f1[N] = new SimpleHtmlFilter();
            } else {
                for (int i = N; i < f1.length; i++) {
                    f1[i] = filters[i - N];
                }
            }
            htmlGrid = new FilteredGrid(table.getGrid(), f1);
        }
        return new TableViewer(htmlGrid, table.getRegion()).buildModel(table);
    }

    public TableModel(int width, int height, IGridTable gridTable) {
        cells = new ICellModel[height][];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new ICellModel[width];
        }

        this.gridTable = gridTable;
    }

    public void addCell(ICellModel cm, int row, int column) {
        if (row < cells.length && column < cells[row].length) {
            cells[row][column] = cm;
        }
    }

    public CellModel findCellModel(int col, int row, int border) {
        if (col < 0 || row < 0 || row >= cells.length || col >= cells[0].length) {
            return null;
        }

        ICellModel icm = cells[row][col];

        CellModel cm = null;
        switch (border) {
            case ICellStyle.TOP:
                if (icm instanceof CellModel) {
                    return (CellModel) icm;
                }
                cm = ((CellModelDelegator) icm).getModel();
                return cm.getRow() == row ? cm : null;
            case ICellStyle.LEFT:
                if (icm instanceof CellModel) {
                    return (CellModel) icm;
                }
                cm = ((CellModelDelegator) icm).getModel();
                return cm.getColumn() == col ? cm : null;
            case ICellStyle.RIGHT:
                if (icm instanceof CellModel) {
                    cm = (CellModel) icm;
                    return cm.getColspan() == 1 ? cm : null;
                }
                cm = ((CellModelDelegator) icm).getModel();
                return cm.getColumn() + cm.getColspan() - 1 == col ? cm : null;
            case ICellStyle.BOTTOM:
                if (icm instanceof CellModel) {
                    cm = (CellModel) icm;
                    return cm.getRowspan() == 1 ? cm : null;
                }
                cm = ((CellModelDelegator) icm).getModel();
                return cm.getRow() + cm.getRowspan() - 1 == row ? cm : null;
            default:
                throw new RuntimeException();

        }

    }

    public ICellModel findOnLeft(int row, int column) {
        if (column == 0) {
            return null;
        }
        return cells[row][column - 1];
    }

    public ICellModel findOnTop(int row, int col) {
        if (row == 0) {
            return null;
        }
        return cells[row - 1][col];
    }

    /**
     * Cells property getter
     *
     * @return cells
     */
    public ICellModel[][] getCells() {
        return cells;
    }

    public IGridTable getGridTable() {
        return gridTable;
    }

    public boolean hasCell(int r, int c) {
        return cells[r][c] != null;
    }

    /** @deprecated */
    public void toHtmlString(StringBuilder buf, boolean showGrid) {
        buf.append("<table ").append(attributes).append(">\n");

        if (showGrid) {
            buf.append(EMPTY);

            for (int i = 0; i < cells[0].length; i++) {
                buf.append(EMPTY_TR);
            }
            buf.append(EMPTY_TRB);
        }

        for (int row = 0; row < cells.length; ++row) {
            buf.append("<tr>\n");

            if (showGrid) {
                buf.append(EMPTY_BL);
            }
            ICellModel[] rowCells = cells[row];
            for (int col = 0; col < rowCells.length; ++col) {
                ICellModel cm = rowCells[col];
                if (cm != null && cm.isReal()) {
                    cm.toHtmlString(buf, this);
                }
            }
            if (showGrid) {
                buf.append(EMPTY_RB);
            }

            buf.append("</tr>\n");
        }

        if (showGrid) {
            buf.append("<tr>");
            buf.append(EMPTY_RBL);
            for (int i = 0; i < cells[0].length; i++) {
                buf.append(EMPTY_RB);
            }

            buf.append(EMPTY_RB);
            buf.append("</tr>");
        }

        buf.append("</table>\n");
    }

}
