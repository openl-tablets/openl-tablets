package org.openl.rules.tableeditor.event;

import java.io.IOException;

import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.mapper.MapperException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.formula.FormulaParseException;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.CellEditorSelector;
import org.openl.rules.tableeditor.model.ICellEditor;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.tableeditor.renderkit.TableEditor;
import org.openl.rules.tableeditor.util.Constants;

/**
 * Table editor controller.
 *
 * @author Andrey Naumenko
 */
public class TableEditorController extends BaseTableEditorController implements ITableEditorController {

    private static final Log LOG = LogFactory.getLog(TableEditorController.class);

    private static final String ERROR_SET_NEW_VALUE = "Error on setting new value to the cell. ";
    private static final String ERROR_SAVE_TABLE = "Failed to save table.";
    private static final String ERROR_OPENED_EXCEL = ERROR_SAVE_TABLE
        + " Please close project Excel file and try again.";

    public String edit() {
        String editorId = getEditorId();
        String cellToEdit = getRequestParam(Constants.REQUEST_PARAM_CELL);
        String errorCell = getRequestParam(Constants.REQUEST_PARAM_ERROR_CELL);
        TableEditorModel editorModel = getEditorModel(editorId);
        TableEditor editor = editorModel.getTableEditor();
        editor.setMode("edit");
        return new HTMLRenderer().render(editor, true, cellToEdit, null, errorCell);
    }

    public String insertRowBefore() {
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
                LOG.error("Internal server error", e);
            }
            return pojo2json(tmResponse);
        }
        return null;
    }

    public String insertColumnBefore() {
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
                LOG.error("Internal server error", e);
            }
            return pojo2json(tmResponse);
        }
        return null;
    }

    public String getCellType() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        if (editorModel != null) {
            ICell cell = editorModel.getOriginalGridTable().getCell(getCol(), getRow());
            ICellEditor editor = new CellEditorSelector().selectEditor(cell);
            EditorTypeResponse typeResponse = editor.getEditorTypeAndMetadata();
            return pojo2json(typeResponse);
        }
        return "";
    }

    private int getCol() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        int numberOfNonShownColumns = editorModel.getNumberOfNonShownCols();        
        return getRequestIntParam(Constants.REQUEST_PARAM_COL) - 1 + numberOfNonShownColumns;
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
            LOG.error("Error when trying to get param", e);
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

    public String load() {
        String editorId = getEditorId();
        String response = render(editorId);
        IGridTable gridTable = getGridTable(editorId);
        response = pojo2json(new LoadResponse(response, gridTable.getGrid().getCell(gridTable.getGridColumn(0, 0),
                gridTable.getGridRow(0, 0)).getUri(), getEditorModel(editorId)));
        return response;
    }

    public String removeRow() {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null
                && row >= 0 && col >= 0) {
            editorModel.removeRows(1, row, col);
            return pojo2json(new TableModificationResponse(render(editorId), editorModel));
        }
        return null;
    }

    public String removeColumn() {
        int col = getCol();
        int row = getRow();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null
                && col >= 0 && row >= 0) {
            editorModel.removeColumns(1, col, row);
            return pojo2json(new TableModificationResponse(render(editorId), editorModel));
        }
        return null;
    }

    public String setIndent() {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            int indent = getRequestIntParam(Constants.REQUEST_PARAM_INDENT);
            ICellStyle style = editorModel.getOriginalGridTable().getCell(col, row).getStyle();
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
            PropertyModificationResponse response = new PropertyModificationResponse(editorModel);
            if (!editorModel.isBusinessView()) {
                response.setResponse(render(editorId));
            }
            if (StringUtils.isBlank(value)) {
                ITableProperties props = editorModel.getTable().getProperties();
                InheritanceLevel inheritanceLevel = props.getPropertyLevelDefinedOn(name);
                if (InheritanceLevel.MODULE.equals(inheritanceLevel)
                        || InheritanceLevel.CATEGORY.equals(inheritanceLevel)) {
                    String inheritedValue = props.getPropertyValueAsString(name);
                    response.setInheritedValue(inheritedValue);
                }
            }
            return pojo2json(response);
        }
        return null;
    }

    public String setAlign() {
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
                ICellStyle style = editorModel.getOriginalGridTable().getCell(col, row).getStyle();
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

    public String setFillColor() {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            String color = getRequestParam(Constants.REQUEST_PARAM_COLOR);

            if (color != null) {
                ICellStyle style = editorModel.getOriginalGridTable().getCell(col, row).getStyle();
                short[] newColor = parseColor(color);
                short[] currentColor = style.getFillForegroundColor();

                if (newColor.length == 3 &&
                        (currentColor == null ||
                                (newColor[0] != currentColor[0] || // red
                                 newColor[1] != currentColor[1] || // green
                                 newColor[2] != currentColor[2]))) { // blue
                    CellStyle newStyle = new CellStyle(style);
                    newStyle.setFillForegroundColor(newColor);
                    newStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
                    editorModel.setStyle(row, col, newStyle);
                }
            }
            return pojo2json(new TableModificationResponse(null, editorModel));
        }
        return null;
    }

    private short[] parseColor(String colorStr) {
        short[] rgb = new short[3];
        String reg = "^rgb\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)$";
        if (colorStr.matches(reg)) {
            String[] rgbStr = colorStr.replaceAll("rgb\\(", "").replaceAll("\\)", "").split(",");
            for (int i = 0; i < rgbStr.length; i++) {
                rgb[i] = Short.parseShort(rgbStr[i].trim());
            }
        }
        return rgb;
    }

    public String undo() {
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

    public String redo() {
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

    public String saveTable() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        if (editorModel != null) {
            TableModificationResponse tmResponse = new TableModificationResponse(null, editorModel);
            try {
                if (beforeSave()) {
                    String newUri = editorModel.save();
                    afterSave(newUri);
                    tmResponse.setResponse(newUri);
                }
            } catch (IOException e) {
                LOG.warn(ERROR_SAVE_TABLE, e);
                tmResponse.setStatus(ERROR_OPENED_EXCEL);
            } catch (Exception e) {
                LOG.error(ERROR_SAVE_TABLE, e);
                tmResponse.setStatus(ERROR_SAVE_TABLE);
            }
            return pojo2json(tmResponse);
        }
        return null;
    }

    private boolean beforeSave() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        String beforeSaveAction = editorModel.getBeforeSaveAction();
        if (beforeSaveAction != null) {
            boolean result = (Boolean) FacesUtils.invokeMethodExpression(beforeSaveAction);
            return result;
        }
        return true;
    }

    private void afterSave(String newUri) {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        String afterSaveAction = editorModel.getAfterSaveAction();
        if (afterSaveAction != null) {
            FacesUtils.invokeMethodExpression(
                    afterSaveAction,
                    StringUtils.isNotBlank(newUri) ? new String[] { newUri } : null,
                    StringUtils.isNotBlank(newUri) ? new Class[] { String.class } : null);
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

    public static class PropertyModificationResponse extends TableModificationResponse {

        private String inheritedValue;

        public PropertyModificationResponse(TableEditorModel model) {
            super(null, null, model);
        }

        public PropertyModificationResponse(String response, String status, TableEditorModel model,
                String inheritedValue) {
            super(response, status, model);
            this.inheritedValue = inheritedValue;
        }

        public String getInheritedValue() {
            return inheritedValue;
        }

        public void setInheritedValue(String inheritedValue) {
            this.inheritedValue = inheritedValue;
        }

    }

}
