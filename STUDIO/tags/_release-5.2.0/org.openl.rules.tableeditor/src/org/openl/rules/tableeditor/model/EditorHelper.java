/**
 * Created Feb 17, 2007
 */
package org.openl.rules.tableeditor.model;

import org.openl.rules.table.IGridTable;

/**
 * @author snshor
 *
 */
public class EditorHelper {

    private TableEditorModel model;

    public EditorHelper() {
    }

    public void init(IGridTable table, boolean cancel) {
        if (model != null && cancel) model.cancel();

        TableEditorModel newModel = new TableEditorModel(table);

        if (!cancel && model != null) newModel.getUndoableActions(model);

        model = newModel;
    }

    public void init(IGridTable table) {
        init(table, true);
    }

    public TableEditorModel getModel()
    {
        return this.model;
    }

    public void setModel(TableEditorModel model)
    {
        this.model = model;
    }

    public void studioReset() {
        if (model != null) {
            model.cancel();
            model = null;
        }
    }
}
