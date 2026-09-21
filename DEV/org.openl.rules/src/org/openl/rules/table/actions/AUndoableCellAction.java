package org.openl.rules.table.actions;

import lombok.Getter;

import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;

/**
 * @author snshor
 */
public abstract class AUndoableCellAction implements IUndoableGridTableAction {

    @Getter
    private final int col;
    @Getter
    private final int row;
    protected final MetaInfoWriter metaInfoWriter;

    protected AUndoableCellAction(int col, int row, MetaInfoWriter metaInfoWriter) {
        this.col = col;
        this.row = row;
        this.metaInfoWriter = metaInfoWriter;
    }
}
