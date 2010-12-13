package org.openl.rules.tableeditor.event;

import java.util.Map;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.tableeditor.util.Constants;

public class BaseTableEditorController {

    public BaseTableEditorController() {
    }

    protected IGridTable getGridTable(String editorId) {
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            return editorModel.getGridTable();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected TableEditorModel getEditorModel(String editorId) {
        Map editorModelMap = (Map) FacesUtils.getSessionParam(Constants.TABLE_EDITOR_MODEL_NAME);
        if (editorModelMap != null) {
            return (TableEditorModel) editorModelMap.get(editorId);
        }
        return null;
    }

    protected TableModel initializeTableModel(String editorId) {
        IGridTable table = getGridTable(editorId);
        int numRows = HTMLRenderer.getMaxNumRowsToDisplay(table);
        return TableModel.initializeTableModel(table, numRows);
    }

    protected String render(String editorId) {
        TableModel tableModel = initializeTableModel(editorId);
        TableEditorModel editorModel = getEditorModel(editorId);
        HTMLRenderer.TableRenderer tableRenderer = new HTMLRenderer.TableRenderer(tableModel);
        tableRenderer.setCellIdPrefix(editorId + "_cell-");
        return tableRenderer.render(editorModel.isShowFormulas());
    }
}
