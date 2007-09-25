package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.rules.ui.TableModel;
import org.openl.rules.ui.TableViewer;
import org.openl.rules.ui.WebStudio;

import javax.faces.context.FacesContext;
import java.util.Map;

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
		  int elementId = Integer.parseInt((String) getRequestParameterMap().get("elementID"));
        TableModel tableModel = initializeTableModel(elementId, webStudio);
	
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

	public String getCellType() {
		Map paramMap = getRequestParameterMap();
		int row = Integer.parseInt((String) paramMap.get("row")) - 1;
		int col = Integer.parseInt((String) paramMap.get("col")) - 1;
		int elementId = Integer.parseInt((String) paramMap.get("elementID"));

		IGridTable table = getGridTable(getWebStudio(), elementId);
		response = "inputbox";
		if (table != null) {
			IGrid grid = table.getGrid();
			int cellType = grid.getCellType(col, row);
			if (cellType == IGrid.CELL_TYPE_STRING) response = "selectbox"; 
		}
		return OUTCOME_SUCCESS;
	}

	public String getResponse() {
		return response;
	}

	private TableModel initializeTableModel(int elementID, WebStudio studio) {
		  IGridTable gt = getGridTable(studio, elementID);
		  if (gt == null) return null; 

		  IGrid htmlGrid = gt.getGrid();
        if (!(htmlGrid instanceof FilteredGrid)) {
            int N = 1;
            IGridFilter[] f1 = new IGridFilter[N];
            f1[0] = new SimpleXlsFormatter();
//            f1[1] = new SimpleHtmlFilter();
            htmlGrid = new FilteredGrid(gt.getGrid(), f1);
        }

        TableViewer tv = new TableViewer(htmlGrid, gt.getRegion());
        return tv.buildModel();
    }

	private IGridTable getGridTable(WebStudio studio, int elementID) {
		String view = studio.getModel().getTableView();
		TableSyntaxNode tsn = studio.getModel().getNode(elementID);
		if (tsn == null) {
            return null; // table is not selected yet
		}
		IGridTable gt = tsn.getTable().getGridTable();
		if (view == null) {
			 view = studio.getMode().getTableMode();
		}
		if (view != null) {
			 ILogicalTable gtx = (ILogicalTable) tsn.getSubTables().get(view);
			 if (gtx != null) {
				  gt = gtx.getGridTable();
			 }
		}
		return gt;
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
