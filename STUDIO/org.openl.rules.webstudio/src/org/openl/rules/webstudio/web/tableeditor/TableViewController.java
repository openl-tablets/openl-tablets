package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.rules.ui.EditorHelper;
import org.openl.rules.ui.TableModel;
import org.openl.rules.ui.TableViewer;
import org.openl.rules.ui.WebStudio;
import org.openl.jsf.Util;

import java.util.Map;

public class TableViewController {
    protected int elementID;
    protected String response;

    public void setElementID(String elementID) {
        try {
            this.elementID = Integer.parseInt(elementID);
        } catch (NumberFormatException e) {
            this.elementID = -1;
        }
    }

    protected TableModel initializeTableModel(int elementID) {
        IGridTable gt = getGridTable(elementID);
        if (gt == null) return null;

        IGrid htmlGrid = gt.getGrid();
        if (!(htmlGrid instanceof FilteredGrid)) {
            int N = 1;
            IGridFilter[] f1 = new IGridFilter[N];
            f1[0] = new SimpleXlsFormatter();
            htmlGrid = new FilteredGrid(gt.getGrid(), f1);
        }

        TableViewer tv = new TableViewer(htmlGrid, gt.getRegion());
        return tv.buildModel();
    }

    protected IGridTable getGridTable(int elementID) {
       return getHelper(elementID).getModel().getUpdatedTable();
    }

    private EditorHelper getHelper(int elementId) {
        Map sessionMap = Util.getSessionMap();
        WebStudio webStudio = Util.getWebStudio();
        synchronized (sessionMap) {
            if (sessionMap.containsKey("editorHelper")) {
                EditorHelper editorHelper = (EditorHelper) sessionMap.get("editorHelper");
                if (editorHelper.getElementID() != elementId) {
                    editorHelper.setTableID(elementId, webStudio.getModel());
                }
                return editorHelper;
            }
            EditorHelper editorHelper = new EditorHelper();
            editorHelper.setTableID(elementId, webStudio.getModel());
            sessionMap.put("editorHelper", editorHelper);
            return editorHelper;
        }
    }

    public String getResponse() {
        return response;
    }

    /**
     * Returns html view of current table as a string.
     * It is just a sequence of calls to <code>render()</code> and <code>getResponse()</code> methdods.
     *
     * @return html representation of current table
     * @throws Exception if an error building response occurres
     */
    public String getTableView() throws Exception {
        render();
        return getResponse();
    }

    private String render() throws Exception {
        if (elementID != -1) {
            TableModel tableModel = initializeTableModel(elementID);
            response = TableRenderer.render(tableModel, "onmouseover=\"try {cellMouseOver(this,event)} catch (e){}\" onmouseout='try {cellMouseOut(this)} catch(e){}'");
        } else {
            response = "";
        }
        return null;
    }
}
