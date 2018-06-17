package org.openl.rules.table.actions;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ICellComment;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.ICellStyle;

/**
 * @author snshor
 *
 */
public abstract class AUndoableCellAction implements IUndoableGridTableAction {

    private int col;
    private int row;

    private Object prevValue;
    private String prevFormula;
    private ICellStyle prevStyle;
    private ICellComment prevComment;
    private CellMetaInfo prevMetaInfo;
    protected final MetaInfoWriter metaInfoWriter;

    public AUndoableCellAction(int col, int row, MetaInfoWriter metaInfoWriter) {
        this.col = col;
        this.row = row;
        this.metaInfoWriter = metaInfoWriter;
    }

    protected void savePrevCell(IWritableGrid grid) {
        ICell cell = grid.getCell(col, row);

        setPrevValue(cell.getObjectValue());
        setPrevFormula(cell.getFormula());
        setPrevStyle(cell.getStyle());
        setPrevComment(cell.getComment());
        setPrevMetaInfo(metaInfoWriter.getMetaInfo(row, col));
    }

    protected void restorePrevCell(IWritableGrid grid) {
        if (prevValue != null || prevStyle != null) {
            grid.createCell(col, row, prevValue, prevFormula, prevStyle, prevComment);
        } else {
            grid.clearCell(col, row);
        }
        metaInfoWriter.setMetaInfo(row, col, prevMetaInfo);
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public Object getPrevValue() {
        return prevValue;
    }

    public void setPrevValue(Object prevValue) {
        this.prevValue = prevValue;
    }

    public String getPrevFormula() {
        return prevFormula;
    }

    public void setPrevFormula(String prevFormula) {
        this.prevFormula = prevFormula;
    }

    public ICellStyle getPrevStyle() {
        return prevStyle;
    }

    public void setPrevStyle(ICellStyle prevStyle) {
        this.prevStyle = prevStyle;
    }

    public ICellComment getPrevComment() {
        return prevComment;
    }

    public void setPrevComment(ICellComment prevComment) {
        this.prevComment = prevComment;
    }

    public CellMetaInfo getPrevMetaInfo() {
        return prevMetaInfo;
    }

    public void setPrevMetaInfo(CellMetaInfo prevMetaInfo) {
        this.prevMetaInfo = prevMetaInfo;
    }
}
