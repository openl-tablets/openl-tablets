package org.openl.rules.webstudio.web.tableeditor;

import java.io.IOException;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.ui.SimpleHtmlFilter;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.rules.ui.OpenLWrapperInfo;
import org.openl.rules.ui.TableEditorModel;
import org.openl.rules.ui.TableModel;
import org.openl.rules.ui.TableViewer;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.util.net.NetUtils;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.webtools.indexer.FileIndexer;

public class BaseTableViewController {
    protected int elementID;
    protected String response;
    private String view;
    private String mode;
    private String wrapper;

    private final TableEditorEnvironment environment;

    public BaseTableViewController(TableEditorEnvironment environment) {
        this.environment = environment;
    }

    public void setElementID(String elementID) {
        try {
            this.elementID = Integer.parseInt(elementID);
        } catch (NumberFormatException e) {
            this.elementID = -1;
        }
    }

    public void setElementID(int elementID) {
        this.elementID = elementID;
    }

    public void setView(String view) {
        this.view = view;
    }

    protected TableModel initializeTableModel(int elementID) {
        IGridTable gt = getGridTable(elementID);
        if (gt == null) {
            return null;
        }
        IGrid htmlGrid = gt.getGrid();
        if (!(htmlGrid instanceof FilteredGrid)) {
            int N = 2;
            IGridFilter[] f1 = new IGridFilter[N];
            f1[0] = new SimpleXlsFormatter();
            f1[1] = new SimpleHtmlFilter();
            htmlGrid = new FilteredGrid(gt.getGrid(), f1);
        }
        TableViewer tv = new TableViewer(htmlGrid, gt.getRegion());
        return tv.buildModel(gt);
    }

    protected IGridTable getGridTable(int elementID) {
        return new TableEditorModel(getWebstudio().getModel().getTableWithMode(elementID, view))
                .getUpdatedTable();
    }

    public String getResponse() {
        return response;
    }

    public String getMode() {
        return getWebstudio().getModel().getTableView(view);
    }

    protected WebStudio getWebstudio() {
        return environment.getWebstudio();
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
        elementID = getElementId();
        render();
        return getResponse();
    }

    protected int getElementId() {
        return environment.getElementId();
    }

    public String getUrl() {
        elementID = getElementId();
        if (elementID == -1) {
            return null;
        }
        return getWebstudio().getModel().makeXlsUrl(elementID);
    }

    public String getUri() {
        elementID = getElementId();
        if (elementID == -1) {
            return null;
        }
        return getWebstudio().getModel().getUri(elementID);
    }

    public String getText() {
        elementID = getElementId();
        if (elementID == -1) {
            return null;
        }
        return FileIndexer.showElementHeader(getUri());
    }

    public String getDisplayNameFull() {
        elementID = getElementId();
        if (elementID == -1) {
            return null;
        }
        return getWebstudio().getModel().getDisplayNameFull(elementID);
    }

    private String render() throws Exception {
        if (elementID != -1) {
            TableModel tableModel = initializeTableModel(elementID);
            response = new TableRenderer(tableModel).renderWithMenu();
        } else {
            response = "";
        }
        return null;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setInit(boolean init) throws Exception {
        WebStudio studio = getWebstudio();
        if (studio == null) {
            studio = WebStudioUtils.getWebStudio(true);
            if (studio.getWrappers().length > 0) {
                studio.select(studio.getWrappers()[0].getWrapperClassName());
            }
        }
        String reload = FacesUtils.getRequestParameter("reload");
        if (reload != null) {
            studio.reset();
        }
        if (StringUtils.isNotEmpty(mode)) {
            getWebstudio().setMode(mode);
        }
    }

    public boolean isLocalRequest() {
        return NetUtils.isLocalRequest((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
    }

    public SelectItem[] getWrappers() throws IOException {
        WebStudio studio = getWebstudio();
        OpenLWrapperInfo[] wrappers = studio.getWrappers();
        SelectItem[] selectItems = new SelectItem[wrappers.length];
        for (int i = 0; i < wrappers.length; i++) {
            selectItems[i] = new SelectItem(wrappers[i].getWrapperClassName(), studio.getMode().getDisplayName(
                    wrappers[i]));
        }
        return selectItems;
    }

    public String getWrapper() {
        return wrapper;
    }

    public void setWrapper(String wrapper) {
        this.wrapper = wrapper;
    }

    public TableEditorEnvironment getEnvironment() {
        return environment;
    }
}
