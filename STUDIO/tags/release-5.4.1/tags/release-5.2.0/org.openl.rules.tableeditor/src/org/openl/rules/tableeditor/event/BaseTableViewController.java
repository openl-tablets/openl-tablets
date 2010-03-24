package org.openl.rules.tableeditor.event;

import java.util.Map;

import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.EditorHelper;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.rules.web.jsf.util.FacesUtils;

public class BaseTableViewController {

    public BaseTableViewController() {
    }

    protected TableEditorModel getEditorModel(String editorId) {
        return getHelper(editorId).getModel();
    }

    protected IGridTable getGridTable(String editorId) {
        return getHelper(editorId).getModel().getUpdatedTable();
    }

    /**
     * Returns <code>EditorHelper</code> instance from http session or creates
     * new one if not present there. Checks that <code>elementId</code>
     * matches id in this helper. If it does not the method prepares response
     * which notifies a client of the mismatch and returns <code>null</code>.
     * In the latter case calling method may just do nothing as corresponding
     * response is already prepared.
     *
     * @param elementId table id
     * @return <code>EditorHelper</code> instance or <code>null</code> if
     *         <code>elementId</code> does not match element id in an existing
     *         helper.
     */
    @SuppressWarnings("unchecked")
    protected EditorHelper getHelper(String editorId) {
        Map sessionMap = FacesUtils.getSessionMap();
        Map editorHelperMap = (Map) sessionMap.get(Constants.TABLE_EDITOR_HELPER_NAME);
        if (editorHelperMap != null) {
            return (EditorHelper) editorHelperMap.get(editorId);
        }
        return null;
    }

    protected TableModel initializeTableModel(String editorId) {
        IGridTable table = getGridTable(editorId);
        return TableModel.initializeTableModel(table);
    }

    protected String render(String editorId) {
        TableModel tableModel = initializeTableModel(editorId);
        HTMLRenderer.TableRenderer tableRenderer = new HTMLRenderer.TableRenderer(tableModel);
        tableRenderer.setCellIdPrefix(editorId + "_cell-");
        return tableRenderer.render();
    }

}
