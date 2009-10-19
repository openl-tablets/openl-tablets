package org.openl.rules.tableeditor.event;

import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.mapper.MapperException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.formula.FormulaParser.FormulaParseException;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.CellEditorSelector;
import org.openl.rules.tableeditor.model.ICellEditor;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.rules.web.jsf.util.FacesUtils;

/**
 * Table editor controller.
 *
 * @author Andrey Naumenko
 */
public class TableEditorController extends BaseTableEditorController implements ITableEditorController {
    
    public static Log LOG = LogFactory.getLog(TableEditorController.class);
    private static String ERROR_SET_NEW_VALUE = "Error on setting new value to the cell. ";

    public String edit() {
        String editorId = getRequestParam(Constants.REQUEST_PARAM_EDITOR_ID);
        String cellToEdit = getRequestParam(Constants.REQUEST_PARAM_CELL);
        TableEditorModel editorModel = getEditorModel(editorId);
        return new HTMLRenderer().render("edit", editorModel.getTable(), null, null, false, cellToEdit, true,
                editorId, null, editorModel.isShowFormulas(), editorModel.isCollapseProps());
    }

    public String insertRowBefore() throws Exception {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            TableModificationResponse tmResponse = new TableModificationResponse(null, editorModel);
            try {
                if (row >= 0) {
                    editorModel.insertRows(1, row, col);
                    tmResponse.setResponse(render(editorId));
                } else {
                    tmResponse.setStatus("Can not insert row");
                }
            } catch (Exception e) {
                tmResponse.setStatus("Internal server error");
                e.printStackTrace();
            }
            return pojo2json(tmResponse);
        }
        return null;
    }

    public String insertColumnBefore() throws Exception {
        int col = getCol();
        int row = getRow();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            TableModificationResponse tmResponse = new TableModificationResponse(null, editorModel);
            try {
                if (col >= 0) {
                    editorModel.insertColumns(1, col, row);
                    tmResponse.setResponse(render(editorId));
                } else {
                    tmResponse.setStatus("Can not insert column");
                }
            } catch (Exception e) {
                tmResponse.setStatus("Internal server error");
                e.printStackTrace();
            }
            return pojo2json(tmResponse);
        }
        return null;
    }

    public String getCellType() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        if (editorModel != null) {
            ICellEditor editor = new CellEditorSelector().selectEditor(getRow(), getCol(), editorModel);
            EditorTypeResponse typeResponse = editor.getEditorTypeAndMetadata();
            return pojo2json(typeResponse);
        }
        return "";
    }

    private int getCol() {
        return getRequestIntParam(Constants.REQUEST_PARAM_COL) - 1;
    }

    private String getEditorId() {
        return getRequestParam(Constants.REQUEST_PARAM_EDITOR_ID);
    }

    private int getRequestIntParam(String name) {
        int param = -1;
        try {
            String requestParam = getRequestParam(name);
            if (requestParam != null) {
                param = Integer.parseInt(requestParam);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return param;
    }

    private String getRequestParam(String name) {
        return FacesUtils.getRequestParameter(name);
    }

    private int getRow() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        int numberOfNonShownRows = editorModel.getNumberOfNonShownRows();
        return getRequestIntParam(Constants.REQUEST_PARAM_ROW) - 1 + numberOfNonShownRows;
    }

    public String load() throws Exception {
        String editorId = getEditorId();
        String response = render(editorId);
        IGridTable gridTable = getGridTable(editorId);
        response = pojo2json(new LoadResponse(response, gridTable.getGrid().getCell(gridTable.getGridColumn(0, 0),
                gridTable.getGridRow(0, 0)).getUri(), getEditorModel(editorId)));
        return response;
    }

    public String removeRow() throws Exception {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            if (row >= 0 && col >= 0) {
                editorModel.removeRows(1, row, col);
                return pojo2json(new TableModificationResponse(render(editorId), editorModel));
            }
        }
        return null;
    }

    public String removeColumn() throws Exception {
        int col = getCol();
        int row = getRow();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            if (col >= 0 && row >= 0) {
                editorModel.removeColumns(1, col, row);
                return pojo2json(new TableModificationResponse(render(editorId), editorModel));
            }
        }
        return null;
    }

    public String setIndent() throws Exception {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            int indent = getRequestIntParam(Constants.REQUEST_PARAM_INDENT);
            ICellStyle style = editorModel.getCell(row, col).getStyle();
            int currentIndent = style.getIdent();
            int resultIndent = currentIndent + indent;
            CellStyle newStyle = new CellStyle(style);
            newStyle.setIdent(resultIndent >= 0 ? resultIndent : 0);
            editorModel.setStyle(row, col, newStyle);
            return pojo2json(new TableModificationResponse(null, editorModel));
        }
        return null;
    }

    public String setCellValue() {
        String value = getRequestParam(Constants.REQUEST_PARAM_VALUE);
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            String message = null;
            try {
                editorModel.setCellValue(getRow(), getCol(), value);
            } catch (FormulaParseException ex) {  
                LOG.warn("ERROR_SET_NEW_VALUE", ex);
                message = ERROR_SET_NEW_VALUE + ex.getMessage();   
            }
            TableModificationResponse tmResponse = new TableModificationResponse(null, editorModel);
            // rerender the table if value is a formula
            
            if (message != null) {
                tmResponse.setStatus(message);
            } else {
                if (value.startsWith("=")) {
                    tmResponse.setResponse(render(editorId));
                }
            }
            return pojo2json(tmResponse);
        }
        return null;
    }

    public String setProperty() throws Exception {
        String name = getRequestParam(Constants.REQUEST_PARAM_PROP_NAME);
        String value = getRequestParam(Constants.REQUEST_PARAM_PROP_VALUE);
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            editorModel.setProperty(name, value);
            TableModificationResponse tmResponse = new TableModificationResponse(null, editorModel);
            if (!editorModel.isBusinessView()) {
                tmResponse.setResponse(render(editorId));
            }
            return pojo2json(tmResponse);
        }
        return null;
    }

    public String setAlign() throws Exception {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            String align = getRequestParam(Constants.REQUEST_PARAM_ALIGN);
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
                ICellStyle style = editorModel.getCell(row, col).getStyle();
                if (style.getHorizontalAlignment() != halign) {
                    CellStyle newStyle = new CellStyle(style);
                    newStyle.setHorizontalAlignment(halign);
                    editorModel.setStyle(row, col, newStyle);
                }
            }
            return pojo2json(new TableModificationResponse(null, editorModel));
        }
        return null;
    }

    public String undo() throws Exception {
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            TableModificationResponse tmResponse = new TableModificationResponse(null, editorModel);
            if (editorModel.hasUndo()) {
                editorModel.undo();
                tmResponse.setResponse(render(editorId));
            } else {
                tmResponse.setStatus("No actions to undo");
            }
            return pojo2json(tmResponse);
        }
        return null;
    }

    public String redo() throws Exception {
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            TableModificationResponse tmResponse = new TableModificationResponse(null, editorModel);
            if (editorModel.hasRedo()) {
                editorModel.redo();
                tmResponse.setResponse(render(editorId));
            } else {
                tmResponse.setStatus("No actions to redo");
            }
            return pojo2json(tmResponse);
        }
        return null;
    }

    public String saveTable() throws Exception {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        if (editorModel != null) {
            editorModel.save();
            return pojo2json(new TableModificationResponse("", editorModel));
        }
        return null;
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

        public Object getParams() {
            return params;
        }

        public void setEditor(String editor) {
            this.editor = editor;
        }

        public void setParams(Object params) {
            this.params = params;
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

        public String getTopLeftCell() {
            return topLeftCell;
        }

        public boolean isHasRedo() {
            return model != null && model.hasRedo();
        }

        public boolean isHasUndo() {
            return model != null && model.hasUndo();
        }

        // required for POJO->JSON mapping
        public void setHasRedo(boolean hasRedo) {
        }

        // required for POJO->JSON mapping
        public void setHasUndo(boolean hasUndo) {
        }

        public void setTableHTML(String tableHTML) {
            this.tableHTML = tableHTML;
        }

        public void setTopLeftCell(String topLeftCell) {
            this.topLeftCell = topLeftCell;
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

        public Long getMin() {
            return min;
        }

        public void setMax(Long max) {
            this.max = max;
        }

        public void setMin(Long min) {
            this.min = min;
        }
    }

    public static class TableModificationResponse {
        private String response;
        private String status;
        private TableEditorModel model;

        public TableModificationResponse(String response, String status, TableEditorModel model) {
            this.response = response;
            this.status = status;
            this.model = model;
        }

        public TableModificationResponse(String response, TableEditorModel model) {
            this.response = response;
            this.model = model;
        }

        public String getResponse() {
            return response;
        }

        public String getStatus() {
            return status;
        }

        public boolean isHasRedo() {
            return model != null && model.hasRedo();
        }

        public boolean isHasUndo() {
            return model != null && model.hasUndo();
        }

        // required for POJO->JSON mapping
        public void setHasRedo(boolean hasRedo) {
        }

        // required for POJO->JSON mapping
        public void setHasUndo(boolean hasUndo) {
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public void setStatus(String status) {
            this.status = status;
        }

    }

}
