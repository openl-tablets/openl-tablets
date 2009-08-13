/**
 * Created Feb 17, 2007
 */
package org.openl.rules.tableeditor.model;

import org.openl.rules.table.ITable;

/**
 * @author snshor
 *
 */
public class EditorHelper {

    private TableEditorModel model;
    private ITable table;
    

    public EditorHelper() {
    }

    public TableEditorModel getModel() {
        return model;
    }

    public void init(ITable table, String view) {
        this.table = table;
        init(view,true, false);
    }

    public void init(ITable table, String view, boolean showFormulas) {
        this.table = table;
        init(view,true, showFormulas);
    }

    public void init(String view,boolean cancel, boolean showFormulas) {
        if (model != null && cancel) {
            model.cancel();
        }

        TableEditorModel newModel = new TableEditorModel(table, view, showFormulas);

        if (!cancel && model != null) {
            newModel.getUndoableActions(model);
        }

        model = newModel;
    }

    public void setModel(TableEditorModel model) {
        this.model = model;
    }

    public void studioReset() {
        if (model != null) {
            model.cancel();
            model = null;
        }
    }

    public ITable getTable() {
        return table;
    }

    public void setTable(ITable table) {
        this.table = table;
    }
}
