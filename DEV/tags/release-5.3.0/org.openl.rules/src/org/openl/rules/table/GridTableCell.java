package org.openl.rules.table;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

class GridTableCell implements ICell {

    private int column;
    private int row;
    private IGridTable table;
    private ICell cell;

    public GridTableCell(int column, int row, IGridTable table) {
        this.column = column;
        this.row = row;
        this.table = table;
        this.cell = table.getGrid().getCell(table.getGridColumn(column, row), table.getGridRow(column, row));
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public IGridRegion getRegion() {
        return cell.getRegion();
    }

    public ICellStyle getStyle() {
        return cell.getStyle();
    }

    public ICellFont getFont() {
        return cell.getFont();
    }

    public int getHeight() {
        return table.isNormalOrientation() ? cell.getHeight() : cell.getWidth();
    }
    
    public Object getObjectValue() {
        return cell.getObjectValue();
    }
    
    public String getStringValue() {
        return cell.getStringValue();
    }

    public int getWidth() {
        return table.isNormalOrientation() ? cell.getWidth() : cell.getHeight();
    }

    public String getFormula() {
        return cell.getFormula();
    }

    public int getType() {
        return cell.getType();
    }

    public String getUri() {
        return cell.getUri();
    }

}
