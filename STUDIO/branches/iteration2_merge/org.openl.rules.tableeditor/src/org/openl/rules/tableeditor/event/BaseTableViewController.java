package org.openl.rules.tableeditor.event;

import java.util.Map;

import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.EditorHelper;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.rules.web.jsf.util.FacesUtils;

public class BaseTableViewController {
    protected String response;

    public BaseTableViewController() {
    }

    protected TableModel initializeTableModel() {
        IGridTable table = getGridTable();
        return TableModel.initializeTableModel(table); 
    }

    public IGridTable getTable() {
        return getGridTable();
    }

    protected IGridTable getGridTable() {
        return getHelper().getModel().getUpdatedTable();
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
    protected EditorHelper getHelper() {
        Map sessionMap = FacesUtils.getSessionMap();
        synchronized (sessionMap) {
            Object helperObject = sessionMap.get(
                    Constants.TABLE_EDITOR_HELPER_NAME);
            if (helperObject != null) {
                EditorHelper editorHelper = (EditorHelper) helperObject;
                return editorHelper;
            }
            return null;
        }
    }

    public String getResponse() {
        return response;
    }

    /**
     * Returns html view of current table as a string. It is just a sequence of
     * calls to <code>render()</code> and <code>getResponse()</code>
     * methdods.
     *
     * @return html representation of current table
     *
     * @throws Exception if an error building response occurres
     */
    public String getTableView() throws Exception {
        render();
        return getResponse();
    }

    private String render() throws Exception {
        TableModel tableModel = initializeTableModel();
        response = new HTMLRenderer.TableRenderer(tableModel).renderWithMenu();
        return null;
    }

}
