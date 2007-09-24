package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.ui.SimpleHtmlFilter;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.rules.ui.TableModel;
import org.openl.rules.ui.TableViewer;
import org.openl.rules.ui.WebStudio;

import java.util.Map;

import javax.faces.context.FacesContext;


/**
 * Table editor controller.
 *
 * @author Andrey Naumenko
 */
public class TableEditorController {
	 private String response;
	 public static final String OUTCOME_SUCCESS = "tableEditor_success"; 
	
	 public String load() throws Exception {
        WebStudio webStudio = (WebStudio) (getSessionMap().get("studio"));
        String view = webStudio.getModel().getTableView();
        int elementId = Integer.parseInt((String) getRequestParameterMap().get("elementID"));
        TableModel tableModel = initializeTableModel(elementId, view, webStudio);

        //String result = converter.buildClientTableModel(tableModel);
        response = TableRenderer.render(tableModel);

        return OUTCOME_SUCCESS;
    }

    public String save() throws Exception {
        String id = (String) getRequestParameterMap().get("id");
        String value = (String) getRequestParameterMap().get("value");
        System.out.println(id);
        System.out.println(value);
		  response = "";
		  return OUTCOME_SUCCESS;
    }

	public String getResponse() {
		return response;
	}

	private TableModel initializeTableModel(int elementID, String view, WebStudio studio) {
        //
        //System.out.println("initializeTableModel");
        //::studio.getModel().showTable(elementID, view)
        TableSyntaxNode tsn = studio.getModel().getNode(elementID);
        if (tsn == null) {
            return null; // table is not selected yet
        }
        IGridTable gt = tsn.getTable().getGridTable();
        if (view == null) {
            view = studio.getMode().getTableMode();
        }
        boolean showGrid = studio.getMode().showTableGrid();
        if (view != null) {
            ILogicalTable gtx = (ILogicalTable) tsn.getSubTables().get(view);
            if (gtx != null) {
                gt = gtx.getGridTable();
            }
        }

        //::return showTable(gt, showGrid);
        //::return showTable(gt, (IGridFilter[]) null, showgrid);
        IGrid htmlGrid = gt.getGrid();
        if (!(htmlGrid instanceof FilteredGrid)) {
            int N = 2;
            IGridFilter[] f1 = new IGridFilter[N];
            f1[0] = new SimpleXlsFormatter();
            f1[1] = new SimpleHtmlFilter();
            htmlGrid = new FilteredGrid(gt.getGrid(), f1);
        }

        TableViewer tv = new TableViewer(htmlGrid, gt.getRegion());
        return tv.buildModel();
    }

    protected Map getRequestParameterMap() {
        return FacesContext.getCurrentInstance().getExternalContext()
            .getRequestParameterMap();
    }

    private WebStudio getWebStudio() {
        return (WebStudio) (getSessionMap().get("studio"));
    }

    private Map getSessionMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
    }
}
