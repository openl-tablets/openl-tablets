package org.openl.rules.tableeditor.event;

import java.io.IOException;

import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.mapper.MapperException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.formula.FormulaParseException;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.formatters.FormulaFormatter;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ICell;
import org.openl.rules.tableeditor.model.CellEditorSelector;
import org.openl.rules.tableeditor.model.ICellEditor;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.tableeditor.renderkit.TableEditor;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.util.formatters.DefaultFormatter;
import org.openl.util.formatters.IFormatter;

/**
 * Table editor controller.
 *
 * @author Andrey Naumenko
 */
public class TableEditorController extends BaseTableEditorController implements ITableEditorController {

    private static final Log LOG = LogFactory.getLog(TableEditorController.class);

    private static final String ERROR_SET_NEW_VALUE = "Error on setting new value to the cell. ";
    private static final String ERROR_SAVE_TABLE = "Failed to save table.";
    private static final String ERROR_SET_STYLE = "Failed to set style.";
    private static final String ERROR_OPENED_EXCEL = ERROR_SAVE_TABLE
        + " Please close project Excel file and try again.";

    public String edit() {
        String editorId = getEditorId();
        String cellToEdit = getRequestParam(Constants.REQUEST_PARAM_CELL);
        String errorCell = getRequestParam(Constants.REQUEST_PARAM_ERROR_CELL);
        TableEditorModel editorModel = getEditorModel(editorId);
        TableEditor editor = editorModel.getTableEditor();
        if (editor.isEditable()) {
            editor.setMode("edit");
            return new HTMLRenderer().renderEditor(editor, cellToEdit, errorCell);
        }
        return StringUtils.EMPTY;
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

    public String getCellEditor() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        if (editorModel != null) {
            ICell cell = editorModel.getOriginalGridTable().getCell(getCol(), getRow());
            ICellEditor editor = new CellEditorSelector().selectEditor(cell);
            EditorTypeResponse editorResponse = editor.getEditorTypeAndMetadata();

            String editorType = editorResponse.getEditor();
            String initValue = getCellEditorInitValue(editorType, cell);
            editorResponse.setInitValue(initValue);

            return pojo2json(editorResponse);
        }
        return "";
    }

    private String getCellEditorInitValue(String editorType, ICell cell) {
        String value = null;

        if (editorType.equals(ICellEditor.CE_NUMERIC)) {
            value = cell.getStringValue();

        } else if (editorType.equals(ICellEditor.CE_FORMULA)) {
            value = "=" + cell.getFormula();
        }

        return value;
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

    private Boolean getRequestBooleanParam(String name) {
        String requestParam = getRequestParam(name);
        return BooleanUtils.toBooleanObject(requestParam);
    }

    private String getRequestParam(String name) {
        return FacesUtils.getRequestParameter(name);
    }

    private int getRow() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        int numberOfNonShownRows = editorModel.getNumberOfNonShownRows();
        return getRequestIntParam(Constants.REQUEST_PARAM_ROW) - 1 + numberOfNonShownRows;
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
            TableModificationResponse response = new TableModificationResponse(null, editorModel);

            int indent = getRequestIntParam(Constants.REQUEST_PARAM_INDENT);

            ICellStyle style = editorModel.getOriginalGridTable().getCell(col, row).getStyle();
            int currentIndent = style != null ? style.getIdent() : 0;
            int resultIndent = currentIndent + indent;

            try {
                editorModel.setIndent(row, col, resultIndent >= 0 ? resultIndent : 0);
            } catch (Exception e) {
                LOG.error(ERROR_SET_STYLE, e);
                response.setStatus(ERROR_SET_STYLE);
            }

            return pojo2json(response);
        }
        return null;
    }

    public String setCellValue() {
        int row = getRow();
        int col = getCol();

        String value = getRequestParam(Constants.REQUEST_PARAM_VALUE);
        String newEditor = getRequestParam(Constants.REQUEST_PARAM_EDITOR);

        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            IFormatter newFormatter = getCellFormatter(newEditor, editorModel, row, col);
            String message = null;
            try {
                editorModel.setCellValue(row, col, value, newFormatter);
            } catch (FormulaParseException ex) {  
                LOG.warn("ERROR_SET_NEW_VALUE", ex);
                message = ERROR_SET_NEW_VALUE + ex.getMessage();   
            }

            TableModificationResponse tmResponse = new TableModificationResponse(null, editorModel);
            if (message != null) {
                tmResponse.setStatus(message);
            } else {
                tmResponse.setResponse(render(editorId));
            }
            return pojo2json(tmResponse);
        }
        return null;
    }

    private IFormatter getCellFormatter(String cellEditor, TableEditorModel editorModel, int row, int col) {
        IFormatter formatter = null;

        if (ICellEditor.CE_FORMULA.equals(cellEditor)) {
            ICell cell = editorModel.getOriginalGridTable().getCell(col, row);

            IFormatter currentFormatter = cell.getDataFormatter();
            IFormatter formulaResultFormatter = null;
            if (!(currentFormatter instanceof FormulaFormatter)) {
                formulaResultFormatter = currentFormatter;
            }
            formatter = new FormulaFormatter(formulaResultFormatter);

        } else if (ICellEditor.CE_TEXT.equals(cellEditor)
                || ICellEditor.CE_MULTILINE.equals(cellEditor)) {
            formatter = new DefaultFormatter();
        }

        return formatter;
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
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
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
                if (style == null || style.getHorizontalAlignment() != halign) {
                    try {
                        editorModel.setAlignment(row, col, halign);
                    } catch (Exception e) {
                        LOG.error(ERROR_SET_STYLE, e);
                        response.setStatus(ERROR_SET_STYLE);
                    }
                }
            }
            return pojo2json(response);
        }
        return null;
    }

    public String setFontBold() {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            Boolean bold = getRequestBooleanParam(Constants.REQUEST_PARAM_FONT_BOLD);

            if (bold != null) {
                ICellFont font = editorModel.getOriginalGridTable().getCell(col, row).getFont();
                if (font == null || font.isBold() != bold) {
                    try {
                        editorModel.setFontBold(row, col, bold);
                    } catch (Exception e) {
                        response.setStatus(ERROR_SET_STYLE);
                        LOG.error(ERROR_SET_STYLE, e);
                    }
                }
            }

            return pojo2json(response);
        }
        return null;
    }

    public String setFontItalic() {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            Boolean italic = getRequestBooleanParam(Constants.REQUEST_PARAM_FONT_ITALIC);

            if (italic != null) {
                ICellFont font = editorModel.getOriginalGridTable().getCell(col, row).getFont();
                if (font == null || font.isItalic() != italic) {
                    try {
                        editorModel.setFontItalic(row, col, italic);
                    } catch (Exception e) {
                        LOG.error(ERROR_SET_STYLE, e);
                        response.setStatus(ERROR_SET_STYLE);
                    }
                }
            }
            return pojo2json(response);
        }
        return null;
    }

    public String setFontUnderline() {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            Boolean underlined = getRequestBooleanParam(Constants.REQUEST_PARAM_FONT_UNDERLINE);

            if (underlined != null) {
                ICellFont font = editorModel.getOriginalGridTable().getCell(col, row).getFont();
                if (font == null || font.isUnderlined() != underlined) {
                    try {
                        editorModel.setFontUnderline(row, col, underlined);
                    } catch (Exception e) {
                        LOG.error(ERROR_SET_STYLE, e);
                        response.setStatus(ERROR_SET_STYLE);
                    }
                }
            }
            return pojo2json(response);
        }
        return null;
    }

    public String setFillColor() {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            String colorStr = getRequestParam(Constants.REQUEST_PARAM_COLOR);

            if (colorStr != null) {
                ICellStyle style = editorModel.getOriginalGridTable().getCell(col, row).getStyle();
                short[] color = parseColor(colorStr);
                short[] currentColor = style != null ? style.getFillForegroundColor() : null;

                if (color.length == 3 &&
                        (currentColor == null ||
                                (color[0] != currentColor[0] || // red
                                 color[1] != currentColor[1] || // green
                                 color[2] != currentColor[2]))) { // blue
                    try {
                        editorModel.setFillColor(row, col, color);
                    } catch (Exception e) {
                        LOG.error(ERROR_SET_STYLE, e);
                        response.setStatus(ERROR_SET_STYLE);
                    }
                }
            }
            return pojo2json(response);
        }
        return null;
    }

    public String setFontColor() {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            String colorStr = getRequestParam(Constants.REQUEST_PARAM_COLOR);

            if (colorStr != null) {
                ICellFont font = editorModel.getOriginalGridTable().getCell(col, row).getFont();
                short[] color = parseColor(colorStr);
                short[] currentColor = font != null ? font.getFontColor() : null;

                if (color.length == 3 &&
                        (currentColor == null ||
                                (color[0] != currentColor[0] || // red
                                 color[1] != currentColor[1] || // green
                                 color[2] != currentColor[2]))) { // blue
                    try {
                        editorModel.setFontColor(row, col, color);
                    } catch (Exception e) {
                        LOG.error(ERROR_SET_STYLE, e);
                        response.setStatus(ERROR_SET_STYLE);
                    }
                }
            }
            return pojo2json(response);
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
        private String initValue;
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

        public String getInitValue() {
            return initValue;
        }

        public void setInitValue(String initValue) {
            this.initValue = initValue;
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
