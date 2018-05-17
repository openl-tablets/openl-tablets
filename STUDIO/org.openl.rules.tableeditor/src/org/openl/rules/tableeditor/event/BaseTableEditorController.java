package org.openl.rules.tableeditor.event;

import java.util.Map;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.tableeditor.renderkit.TableEditor;
import org.openl.rules.tableeditor.util.Constants;

public class BaseTableEditorController {

    public BaseTableEditorController() {
    }

    @SuppressWarnings("unchecked")
    protected TableEditorModel getEditorModel(String editorId) {
        Map editorModelMap = (Map) FacesUtils.getSessionParam(Constants.TABLE_EDITOR_MODEL_NAME);
        if (editorModelMap != null) {
            return (TableEditorModel) editorModelMap.get(editorId);
        }
        return null;
    }

    protected TableEditorModel startEditing(String editorId) {
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            String beforeEditAction = editorModel.getBeforeEditAction();
            if (beforeEditAction != null) {
                boolean successful = (Boolean) FacesUtils.invokeMethodExpression(beforeEditAction);
                if (!successful) {
                    return null;
                }
            }

            prepareForEdit(editorModel);
        }

        return editorModel;
    }

    private void prepareForEdit(TableEditorModel editorModel) {
        TableEditor tableEditor = editorModel.getTableEditor();
        String mode = tableEditor.getMode();
        if ((mode == null || Constants.MODE_EDIT.equals(mode)) && tableEditor.isEditable()) {
            // Prepare workbook for edit (load it to memory before editing starts)
            editorModel.getGridTable().edit();
        }
    }

    protected void removeEditorModel() {
        FacesUtils.removeSessionParam(Constants.TABLE_EDITOR_MODEL_NAME);
    }

    private TableModel initializeTableModel(String editorId) {
        TableEditorModel editorModel = getEditorModel(editorId);
        IGridTable table = editorModel.getGridTable();

        int numRows = HTMLRenderer.getMaxNumRowsToDisplay(table);

        TableEditor editor = editorModel.getTableEditor();
        String mode = editor.getMode();
        if (mode == null && editor.isEditable()) {
            // This method is invoked only while editing the table. So we can assume that mode is EDIT.
            mode = Constants.MODE_EDIT;
        }
        return TableModel.initializeTableModel(table, null, numRows, editor.getLinkBuilder(), mode, editor.getView(),
                editorModel.getMetaInfoReader());
    }

    protected String render(String editorId) {
        TableModel tableModel = initializeTableModel(editorId);
        TableEditorModel editorModel = getEditorModel(editorId);
        HTMLRenderer.TableRenderer tableRenderer = new HTMLRenderer.TableRenderer(tableModel);
        return tableRenderer.render(editorModel.isShowFormulas(), null, editorId, null);
    }
}
