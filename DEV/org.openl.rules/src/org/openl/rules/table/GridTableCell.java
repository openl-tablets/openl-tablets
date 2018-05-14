package org.openl.rules.table;

import java.util.Date;

import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;

class GridTableCell implements ICell {

    private int column;
    private int row;
    private IGridTable table;
    private ICell cell;

    public ICell getTopLeftCellFromRegion() {
		return cell.getTopLeftCellFromRegion();
	}

	public GridTableCell(int column, int row, IGridTable table) {
        this.column = column;
        this.row = row;
        this.table = table;
        int gridColumn = table.getGridColumn(column, row);
        int gridRow = table.getGridRow(column, row);
        this.cell = table.getGrid().getCell(gridColumn, gridRow);
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public int getAbsoluteColumn() {
        return cell.getAbsoluteColumn();
    }

    public int getAbsoluteRow() {
        return cell.getAbsoluteRow();
    }

    public IGridRegion getAbsoluteRegion() {
        return cell.getAbsoluteRegion();
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

    public boolean getNativeBoolean() {
        return cell.getNativeBoolean();
    }

    public double getNativeNumber() {
        return cell.getNativeNumber();
    }

    public int getNativeType() {
        return cell.getNativeType();
    }

    public boolean hasNativeType() {
        return cell.hasNativeType();
    }

    public Date getNativeDate() {
        return cell.getNativeDate();
    }

    public ICellComment getComment() {
        return cell.getComment();
    }

}
