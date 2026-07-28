package org.openl.rules.table.actions;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.ICellStyle;

/**
 * @author snshor
 */
public abstract class AUndoableCellAction implements IUndoableGridTableAction {

    @Getter
    @Setter
    private int col;
    @Getter
    @Setter
    private int row;

    @Getter
    @Setter
    private Object prevValue;
    @Getter
    @Setter
    private String prevFormula;
    @Getter
    @Setter
    private ICellStyle prevStyle;
    @Getter
    @Setter
    private String prevComment;
    @Getter
    @Setter
    private String prevCommentAuthor;
    @Getter
    @Setter
    private CellMetaInfo prevMetaInfo;
    protected final MetaInfoWriter metaInfoWriter;

    public AUndoableCellAction(int col, int row, MetaInfoWriter metaInfoWriter) {
        this.col = col;
        this.row = row;
        this.metaInfoWriter = metaInfoWriter;
    }

    protected void savePrevCell(IWritableGrid grid) {
        var cell = grid.getCell(col, row);

        setPrevValue(cell.getObjectValue());
        setPrevFormula(cell.getFormula());
        setPrevStyle(cell.getStyle());
        if (cell.getComment() != null) {
            setPrevComment(cell.getComment().getText());
            setPrevCommentAuthor(cell.getComment().getAuthor());
        }
        setPrevMetaInfo(metaInfoWriter.getMetaInfo(row, col));
    }

    protected void restorePrevCell(IWritableGrid grid) {
        if (prevValue != null || prevStyle != null) {
            grid.createCell(col, row, prevValue, prevFormula, prevStyle, prevComment, prevCommentAuthor);
        } else {
            grid.clearCell(col, row);
        }
        metaInfoWriter.setMetaInfo(row, col, prevMetaInfo);
    }
}
