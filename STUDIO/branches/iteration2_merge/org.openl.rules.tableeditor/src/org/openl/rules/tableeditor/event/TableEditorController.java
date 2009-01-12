package org.openl.rules.tableeditor.event;

import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.mapper.MapperException;

import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.CellEditorSelector;
import org.openl.rules.tableeditor.model.ComboBoxCellEditor;
import org.openl.rules.tableeditor.model.EditorHelper;
import org.openl.rules.tableeditor.model.ICellEditor;
import org.openl.rules.tableeditor.model.MultiChoiceCellEditor;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.rules.util.net.NetUtils;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webtools.XlsUrlParser;

import java.io.IOException;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;

/**
 * Table editor controller. It should be a managed bean with <b>request</b>
 * scope.
 * 
 * @author Andrey Naumenko
 */
public class TableEditorController extends BaseTableViewController implements JSTableEditor {
    private int row, col;

    private CellEditorSelector selector = new CellEditorSelector();

    public String load() throws Exception {
        readRequestParams();
        render();
        IGridTable gridTable = getGridTable();
        response = pojo2json(new LoadResponse(response, gridTable.getGrid().getCellUri(gridTable.getGridColumn(0, 0),
                gridTable.getGridRow(0, 0)), getHelper().getModel()));
        return response;
    }

    private void render() {
        TableModel tableModel = initializeTableModel();
        response = new HTMLRenderer.TableRenderer(tableModel).render();
    }

    /**
     * Handles request saving new cell value.
     * 
     * @return {@link #OUTCOME_SUCCESS} jsf navigation case outcome
     */
    public String save() {
        readRequestParams();
        String value = getRequestParameter(Constants.REQUEST_PARAM_VALUE);

        EditorHelper editorHelper = getHelper();
        if (editorHelper != null) {
            editorHelper.getModel().setCellValue(row, col, value);
            response = pojo2json(new TableModificationResponse(null, editorHelper.getModel()));
        }
        return response;
    }

    public String edit() {
        String cellToEdit = getRequestParameter(Constants.REQUEST_PARAM_CELL);
        String result = new HTMLRenderer().render("edit", null, null, false, cellToEdit, true);
        return result;
    }

    public String editXls() {
        String cellUri = getRequestParameter(Constants.REQUEST_PARAM_CELL_URI);
        boolean local = NetUtils.isLocalRequest(
                (ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
        boolean wantURI = (cellUri == null || cellUri.equals("")) ? false : true;
        if (local) {
            if (wantURI) {
                XlsUrlParser parser = new XlsUrlParser();
                parser.parse(cellUri);
                try {
                    org.openl.rules.webtools.ExcelLauncher.launch("LaunchExcel.vbs",
                            parser.wbPath, parser.wbName, parser.wsName, parser.range);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (wantURI) {
            return "<script type='text/javascript'>"
                + "alert('This action is available only from the machine server runs at.')</script>";
        }
        return null;
    }

    /**
     * Generates JSON response for cell type: editor type and editor specific
     * setup javascript object.
     * 
     * @return {@link #OUTCOME_SUCCESS} jsf navigation case outcome
     * 
     * Modified by snshor to reflect new Cell Editor creation/selection
     * framework
     */

    public String getCellType() {
        readRequestParams();

        response = "";

        EditorHelper editorHelper = getHelper();
        if (editorHelper != null) {
            TableEditorModel model = editorHelper.getModel();
            ICellEditor editor = selector.selectEditor(row, col, model);
            EditorTypeResponse typeResponse = editor.getEditorTypeAndMetadata();
            response = pojo2json(typeResponse);
        }

        return response;

    }

    /**
     * Generates JSON response for cell type: editor type and editor specific
     * setup javascript object.
     * 
     * @return {@link #OUTCOME_SUCCESS} jsf navigation case outcome
     * 
     * todo: remove
     */
    public String getCellTypeOld() {
        readRequestParams();

        response = "";
        EditorHelper editorHelper = getHelper();
        if (editorHelper != null) {
            EditorTypeResponse typeResponse = new EditorTypeResponse("text");
            TableEditorModel editorModel = editorHelper.getModel();

            if (editorModel.updatedTableCellInsideTableRegion(row, col)) {

                TableEditorModel.CellType type = editorModel.getCellType(row, col);
                if (type == TableEditorModel.CellType.CA_ENUMERATION_CELL_TYPE) {
                    String[] metadata = (String[]) editorModel.getCellEditorMetadata(row, col);
                    typeResponse = new ComboBoxCellEditor(metadata, metadata).getEditorTypeAndMetadata();
                }

                if (col == 3 && row == 1) {
                    typeResponse = new EditorTypeResponse("multilineText");
                }
                if (col == 2 && row == 1) {
                    typeResponse = new EditorTypeResponse("date");
                }

                if (col == 1 && row == 1) {
                    typeResponse = new EditorTypeResponse("numeric");
                    typeResponse.setParams(new RangeParam(-1000L, 1000L));
                }

                if (col == 0 && row == 1) {
                    typeResponse = new EditorTypeResponse("price");
                }

                if (col == 2 && row == 2) {
                    typeResponse = new EditorTypeResponse("suggestText");
                    typeResponse.setParams(new SuggestParam(3, 1000));
                }

                if (col == 3 && row == 2) {
                    String[] metadata = (String[]) editorModel.getCellEditorMetadata(row, col);
                    typeResponse = new MultiChoiceCellEditor(metadata, metadata).getEditorTypeAndMetadata();
                }

                response = pojo2json(typeResponse);
            }
        }
        return response;
    }

    public String undo() throws Exception {
        readRequestParams();
        EditorHelper editorHelper = getHelper();
        if (editorHelper != null) {
            TableModificationResponse tmResponse = new TableModificationResponse(null, editorHelper.getModel());
            if (editorHelper.getModel().hasUndo()) {
                editorHelper.getModel().undo();
                render();
                tmResponse.setResponse(response);
            } else {
                tmResponse.setStatus("No actions to undo");
            }
            response = pojo2json(tmResponse);
        }
        return response;
    }

    public String redo() throws Exception {
        readRequestParams();
        EditorHelper editorHelper = getHelper();
        if (editorHelper != null) {
            TableModificationResponse tmResponse = new TableModificationResponse(null, editorHelper.getModel());
            if (editorHelper.getModel().hasRedo()) {
                editorHelper.getModel().redo();
                render();
                tmResponse.setResponse(response);
            } else {
                tmResponse.setStatus("No actions to redo");
            }
            response = pojo2json(tmResponse);
        }
        return response;
    }

    public String addRowColBefore() throws Exception {
        readRequestParams();

        EditorHelper editorHelper = getHelper();
        if (editorHelper != null) {
            TableEditorModel editorModel = editorHelper.getModel();

            TableModificationResponse tmResponse = new TableModificationResponse(null, editorHelper.getModel());
            try {
                if (row >= 0)
                    if (editorModel.canAddRows(1))
                        editorModel.insertRows(1, row);
                    else
                        tmResponse.setStatus("Can not add row");
                else if (editorModel.canAddCols(1))
                    editorModel.insertColumns(1, col);
                else
                    tmResponse.setStatus("Can not add column");
            } catch (Exception e) {
                tmResponse.setStatus("Internal server error");
                e.printStackTrace();
            }

            render();
            tmResponse.setResponse(response);
            response = pojo2json(tmResponse);
        }
        return response;
    }

    public String removeRowCol() throws Exception {
        readRequestParams();
        EditorHelper editorHelper = getHelper();
        if (editorHelper != null) {
            TableEditorModel editorModel = editorHelper.getModel();
            boolean move = Boolean.valueOf(getRequestParameter(
                    Constants.REQUEST_PARAM_MOVE));

            if (row >= 0) {
                if (move)
                    ;
                else
                    editorModel.removeRows(1, row);
            } else {
                if (move)
                    ;
                else
                    editorModel.removeColumns(1, col);
            }
            render();
            response = pojo2json(new TableModificationResponse(response, editorHelper.getModel()));
        }
        return response;
    }

    public String saveTable() throws IOException {
        readRequestParams();
        EditorHelper editorHelper = getHelper();
        if (editorHelper != null) {
            editorHelper.getModel().save();
            response = pojo2json(new TableModificationResponse("", editorHelper.getModel()));
        }
        return response;
    }

    public String setAlign() throws Exception {
        readRequestParams();
        EditorHelper editorHelper = getHelper();
        if (editorHelper != null) {
            String align = getRequestParameter(Constants.REQUEST_PARAM_ALIGN);
            int halign = -1;
            if ("left".equalsIgnoreCase(align)) {
                halign = ICellStyle.ALIGN_LEFT;
            } else if ("center".equalsIgnoreCase(align)) {
                halign = ICellStyle.ALIGN_CENTER;
            } else if ("right".equalsIgnoreCase(align)) {
                halign = ICellStyle.ALIGN_RIGHT;
            } else if ("justify".equalsIgnoreCase(align)) {
                halign = ICellStyle.ALIGN_JUSTIFY;
            }

            if (halign != -1) {
                ICellStyle style = editorHelper.getModel().getCellStyle(row, col);
                if (style.getHorizontalAlignment() != halign) {
                    CellStyle newStyle = new CellStyle(style);
                    newStyle.setHorizontalAlignment(halign);
                    editorHelper.getModel().setStyle(row, col, newStyle);
                }
            }
            response = pojo2json(new TableModificationResponse(null, editorHelper.getModel()));
        }
        return response;
    }

    private String getRequestParameter(String name) {
        return FacesUtils.getRequestParameter(name);
    }

    @SuppressWarnings("unchecked")
    private void readRequestParams() {
        Map<String, String> paramMap = FacesUtils.getRequestParameterMap(); 
        row = col = -1;

        try {
            row = Integer.parseInt(paramMap.get(Constants.REQUEST_PARAM_ROW)) - 1;
        } catch (NumberFormatException e) {
        }
        try {
            col = Integer.parseInt(paramMap.get(Constants.REQUEST_PARAM_COL)) - 1;
        } catch (NumberFormatException e) {
        }
    }

    private static String pojo2json(Object pojo) {
        try {
            return new StringBuilder().append("(").append(JSONMapper.toJSON(pojo).render(true)).append(")").toString();
        } catch (MapperException e) {
            return null;
        }
    }

    public static class EditorTypeResponse {
        private String editor;
        private Object params;

        public EditorTypeResponse(String editor) {
            this.editor = editor;
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
    }

    public static class TableModificationResponse {
        private String response;
        private String status;
        private TableEditorModel model;

        public TableModificationResponse(String response, TableEditorModel model) {
            this.response = response;
            this.model = model;
        }

        public TableModificationResponse(String response, String status, TableEditorModel model) {
            this.response = response;
            this.status = status;
            this.model = model;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public boolean isHasUndo() {
            return model != null && model.hasUndo();
        }

        @SuppressWarnings("unused")
        public void setHasUndo(boolean hasUndo) {
        }

        public boolean isHasRedo() {
            return model != null && model.hasRedo();
        }

        @SuppressWarnings("unused")
        public void setHasRedo(boolean hasRedo) {
        }

    }

    public static class RangeParam {
        private Long min, max;

        public RangeParam(Long min, Long max) {
            this.min = min;
            this.max = max;
        }

        public Long getMax() {
            return max;
        }

        public void setMax(Long max) {
            this.max = max;
        }

        public Long getMin() {
            return min;
        }

        public void setMin(Long min) {
            this.min = min;
        }
    }

    public static class SuggestParam {
        private Integer minChars;
        private Integer delay;

        public SuggestParam(Integer minChars, Integer delay) {
            this.minChars = minChars;
            this.delay = delay;
        }

        public Integer getMinChars() {
            return minChars;
        }

        public void setMinChars(Integer minChars) {
            this.minChars = minChars;
        }

        public Integer getDelay() {
            return delay;
        }

        public void setDelay(Integer delay) {
            this.delay = delay;
        }
    }

    public static class LoadResponse {
        private String tableHTML;
        private String topLeftCell;
        private TableEditorModel model;

        public LoadResponse(String tableHTML, String topLeftCell, TableEditorModel model) {
            this.tableHTML = tableHTML;
            this.topLeftCell = topLeftCell;
            this.model = model;
        }

        public String getTableHTML() {
            return tableHTML;
        }

        public void setTableHTML(String tableHTML) {
            this.tableHTML = tableHTML;
        }

        public String getTopLeftCell() {
            return topLeftCell;
        }

        public void setTopLeftCell(String topLeftCell) {
            this.topLeftCell = topLeftCell;
        }

        public boolean isHasUndo() {
            return model != null && model.hasUndo();
        }

        @SuppressWarnings("unused")
        public void setHasUndo(boolean hasUndo) {
        }
        
        public boolean isHasRedo() {
            return model != null && model.hasRedo();
        }
        
        @SuppressWarnings("unused")
        public void setHasRedo(boolean hasRedo) {
        }

    }

}
