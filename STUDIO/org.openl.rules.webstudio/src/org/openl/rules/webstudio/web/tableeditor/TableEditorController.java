package org.openl.rules.webstudio.web.tableeditor;

import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.mapper.MapperException;
import org.openl.jsf.Util;
import static org.openl.jsf.Util.getRequestParameter;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.FilteredGrid;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.xls.SimpleXlsFormatter;
import org.openl.rules.ui.EditorHelper;
import org.openl.rules.ui.TableEditorModel;
import org.openl.rules.ui.TableModel;
import org.openl.rules.ui.TableViewer;

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
		  int elementId = Integer.parseInt(getRequestParameter("elementID"));
        TableModel tableModel = initializeTableModel(elementId);

        response = TableRenderer.render(tableModel);

        return OUTCOME_SUCCESS;
    }

    public String save() throws Exception {
        String id = getRequestParameter("id");
        String value = getRequestParameter("value");
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

      EditorTypeResponse typeResponse = new EditorTypeResponse("inputbox");

      TableEditorModel editorModel = getHelper(elementId).getModel();
      TableEditorModel.CellType type = editorModel.getCellType(row, col);
      if (type == TableEditorModel.CellType.CA_ENUMERATION_CELL_TYPE) {
         String[] metadata = (String[]) editorModel.getCellEditorMetadata(row, col);
         typeResponse.setEditor("selectbox");
         typeResponse.setParams(metadata);
      }

      response = typeResponse.toJSON();
      return OUTCOME_SUCCESS;
	}

	public String getResponse() {
		return response;
	}

   public String addRowColBefore() throws Exception {
      String rowStr = getRequestParameter("row");
      int row = -1, col = -1;
      if (rowStr == null) {
         col = Integer.parseInt(getRequestParameter("col"));
      } else {
         row = Integer.parseInt(rowStr);
      }

      int elementId = Integer.parseInt(getRequestParameter("elementID"));
      TableEditorModel editorModel = getHelper(elementId).getModel();

      if (row > 0)
         editorModel.insertRows(1, row-1);
      else
         editorModel.insertColumns(1, col-1);

      return load();
   }

   public String removeRowCol() throws Exception {
      String rowStr = getRequestParameter("row");
      int row = -1, col = -1;
      if (rowStr == null) {
         col = Integer.parseInt(getRequestParameter("col"));
      } else {
         row = Integer.parseInt(rowStr);
      }

      int elementId = Integer.parseInt(getRequestParameter("elementID"));
      TableEditorModel editorModel = getHelper(elementId).getModel();
      boolean move = Boolean.valueOf(Util.getRequestParameter("move"));

      if (row > 0) {
         if (move); else editorModel.removeRows(1, row-1);
      } else {
         if (move); else editorModel.removeColumns(1, col-1);
      }
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
      return getHelper(elementID).getModel().getUpdatedTable();
   }

   private synchronized static EditorHelper getHelper(int elementId) {
      Map sessionMap = Util.getSessionMap();
      if (sessionMap.containsKey("editorHelper")) return (EditorHelper) sessionMap.get("editorHelper");
      EditorHelper editorHelper = new EditorHelper();
      editorHelper.setTableID(elementId, Util.getWebStudio().getModel());
      sessionMap.put("editorHelper", editorHelper);
      return editorHelper;
   }


   public static class EditorTypeResponse {
      private String editor;
      private Object params;

      public EditorTypeResponse(String editor) {
         this.editor = editor;
      }

      public EditorTypeResponse(String editor, Object params) {
         this.editor = editor;
         this.params = params;
      }

      public String getEditor() {
         return editor;
      }

      public void setEditor(String editor) {
         this.editor = editor;
      }

      public Object getParams() {
         return params;
      }

      public void setParams(Object params) {
         this.params = params;
      }

      String toJSON() {
         try {
            return new StringBuilder().append("(").append(JSONMapper.toJSON(this).render(true)).append(")").toString();
         } catch (MapperException e) {
            return null;
         }
      }
   }
}
