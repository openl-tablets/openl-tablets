package org.openl.rules.webstudio.web.tableeditor;

import org.openl.jsf.Util;
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
import org.openl.rules.ui.TableEditorModel;
import org.apache.commons.collections.map.HashedMap;

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
		  int elementId = Integer.parseInt(Util.getRequestParameter("elementID"));
        TableModel tableModel = initializeTableModel(elementId);
	
        response = TableRenderer.render(tableModel);

        return OUTCOME_SUCCESS;
    }

    public String save() throws Exception {
        String id = Util.getRequestParameter("id");
        String value = Util.getRequestParameter("value");
        System.out.println(id);
        System.out.println(value);
		  response = "";
		  return OUTCOME_SUCCESS;
    }

	public String getCellType() {
		Map paramMap = Util.getRequestParameterMap();
		int row = Integer.parseInt((String) paramMap.get("row")) - 1;
		int col = Integer.parseInt((String) paramMap.get("col")) - 1;
		int elementId = Integer.parseInt((String) paramMap.get("elementID"));

		IGridTable table = getGridTable(elementId);
		response = "inputbox";
		/*if (table != null) {
			IGrid grid = table.getGrid();
			int cellType = grid.getCellType(col, row);
			if (cellType == IGrid.CELL_TYPE_STRING) response = "selectbox"; 
		}   */
		return OUTCOME_SUCCESS;
	}

	public String getResponse() {
		return response;
	}

   public String addRowBefore() throws Exception {
      int row = Integer.parseInt(Util.getRequestParameter("row"));
      int elementId = Integer.parseInt(Util.getRequestParameter("elementID"));

      TableEditorModel editorModel = getHelper().getModel(elementId);
      editorModel.insertRows(1, row-1);

      return load();
   }

   public String removeRow() throws Exception {
      int row = Integer.parseInt(Util.getRequestParameter("row"));
      int elementId = Integer.parseInt(Util.getRequestParameter("elementID"));

      TableEditorModel editorModel = getHelper().getModel(elementId);
      editorModel.removeRows(1, row-1);

      return load();
   }

   private TableModel initializeTableModel(int elementID) {
		  IGridTable gt = getGridTable(elementID);
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

	private IGridTable getGridTable(int elementID) {
      return getHelper().getModel(elementID).getUpdatedTable();
   }

   private synchronized static EditorHelper getHelper() {
      Map sessionMap = Util.getSessionMap();
      if (sessionMap.containsKey("editorHelper")) return (EditorHelper) sessionMap.get("editorHelper");
      EditorHelper editorHelper = new EditorHelper();
      sessionMap.put("editorHelper", editorHelper);
      return editorHelper;
   }

   static class EditorHelper {
      Map<Integer, TableEditorModel> models = new HashedMap();

      synchronized TableEditorModel getModel(int elementId) {
         TableEditorModel editorModel = models.get(elementId);
         if (editorModel == null) {
            models.put(elementId, editorModel = new TableEditorModel(Util.getWebStudio().getModel().getTableWithMode(elementId)));
         }
         return editorModel;
      }
      
   }
}
