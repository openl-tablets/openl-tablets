package org.openl.rules.tableeditor.event;

import java.io.IOException;
import java.util.Date;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.context.FacesContext;

import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.service.TableServiceException;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.formatters.FormulaFormatter;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.formatters.XlsDataFormatterFactory;
import org.openl.rules.tableeditor.model.CellEditorSelector;
import org.openl.rules.tableeditor.model.ICellEditor;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.util.BooleanUtils;
import org.openl.util.StringUtils;
import org.openl.util.formatters.DefaultFormatter;
import org.openl.util.formatters.IFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.mapper.MapperException;

/**
 * Table editor controller.
 *
 * @author Andrey Naumenko
 * @author Andrei Ostrovski
 */
public class TableEditorController extends BaseTableEditorController {

    private final Logger log = LoggerFactory.getLogger(TableEditorController.class);

    private static final String SERVER_ERROR = "Internal server error.";
    private static final String ERROR_SET_NEW_VALUE = "Error on setting new value to the cell. ";
    private static final String ERROR_SAVE_TABLE = "Failed to save table.";
    private static final String ERROR_SET_STYLE = "Failed to set style.";
    private static final String ERROR_INSERT_ROW = "Cannot insert row.";
    private static final String ERROR_INSERT_COLUMN = "Cannot insert column.";
    private static final String ERROR_OPENED_EXCEL = ERROR_SAVE_TABLE + " Please close module Excel file and try again.";

    public static Object invokeMethodExpression(String expressionString) {
        return invokeMethodExpression(expressionString, null, null);
    }

    private static Object invokeMethodExpression(String expressionString, Object[] params, Class<?>[] paramTypes) {
        FacesContext context = FacesContext.getCurrentInstance();
        ELContext elContext = context.getELContext();
        MethodExpression methodExpression = context.getApplication()
            .getExpressionFactory()
            .createMethodExpression(elContext, expressionString, null, paramTypes == null ? new Class[0] : paramTypes);
        return methodExpression.invoke(
            FacesContext.getCurrentInstance().getELContext(), params == null ? new Object[0] : params);
    }

    public void startEditing() {
        startEditing(getEditorId());
    }

    public String insertRowBefore() {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            try {
                if (row >= 0) {
                    editorModel.insertRows(1, row, col);
                    response.setHtml(render(editorId));
                } else {
                    response.setMessage(ERROR_INSERT_ROW);
                }
            } catch (Exception e) {
                if (e.getCause() instanceof TableServiceException) {
                    response.setMessage(ERROR_INSERT_ROW);
                } else {
                    response.setMessage(SERVER_ERROR);
                }
                log.error(SERVER_ERROR, e);
            }
            return pojo2json(response);
        }
        return null;
    }

    public String insertColumnBefore() {
        int col = getCol();
        int row = getRow();
        String editorId = getEditorId();
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            try {
                if (col >= 0) {
                    editorModel.insertColumns(1, col, row);
                    response.setHtml(render(editorId));
                } else {
                    response.setMessage(ERROR_INSERT_COLUMN);
                }
            } catch (Exception e) {
                if (e.getCause() instanceof TableServiceException) {
                    response.setMessage(ERROR_INSERT_COLUMN);
                } else {
                    response.setMessage(SERVER_ERROR);
                }
                log.error(SERVER_ERROR, e);
            }
            return pojo2json(response);
        }
        return null;
    }

    public String getCellEditor() {
        TableEditorModel editorModel = startEditing(getEditorId());
        if (editorModel != null) {
            int row = getRow();
            int col = getCol();
            ICell cell = editorModel.getOriginalGridTable().getCell(col, row);
            CellMetaInfo meta = getMetaInfo(editorModel, row, col);
            ICellEditor editor = new CellEditorSelector().selectEditor(cell, meta);
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
        } else if (editorType.equals(ICellEditor.CE_TEXT) || editorType.equals(ICellEditor.CE_MULTILINE)) {
            value = cell.getStringValue();
        } else if (editorType.equals(ICellEditor.CE_DATE)) {
            // Format must be same as in DateEditor.js
            Object objectValue;
            try {
                objectValue = cell.getNativeDate();
            } catch (IllegalStateException e) {
                objectValue = cell.getObjectValue();
            }
            value = FormattersManager.format(objectValue);
        }
        return value;
    }

    private CellMetaInfo getMetaInfo(TableEditorModel editorModel, int row, int col) {
        IGridTable originalGridTable = editorModel.getOriginalGridTable();
        IGridRegion originalRegion = originalGridTable.getRegion();
        int gcol = originalRegion.getLeft() + col;
        int grow = originalRegion.getTop() + row;
        return editorModel.getMetaInfoReader().getMetaInfo(grow, gcol);
    }

    private int getCol() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        int numberOfNonShownColumns = editorModel.getNumberOfNonShownCols();
        return getRequestIntParam("col") - 1 + numberOfNonShownColumns;
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
            log.error("Error when trying to get param", e);
        }
        return param;
    }

    private Boolean getRequestBooleanParam(String name) {
        String requestParam = getRequestParam(name);
        return BooleanUtils.toBooleanObject(requestParam);
    }

    private String getRequestParam(String name) {
        String value = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(name);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return value;
    }

    private int getRow() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        int numberOfNonShownRows = editorModel.getNumberOfNonShownRows();
        return getRequestIntParam("row") - 1 + numberOfNonShownRows;
    }

    public String removeRow() {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null && row >= 0 && col >= 0) {
            editorModel.removeRows(1, row, col);
            return pojo2json(new TableModificationResponse(render(editorId), editorModel));
        }
        return null;
    }

    public String removeColumn() {
        int col = getCol();
        int row = getRow();
        String editorId = getEditorId();
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null && col >= 0 && row >= 0) {
            editorModel.removeColumns(1, col, row);
            return pojo2json(new TableModificationResponse(render(editorId), editorModel));
        }
        return null;
    }

    public String setIndent() {
        int row = getRow();
        int col = getCol();
        String editorId = getEditorId();
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);

            int indent = getRequestIntParam(Constants.REQUEST_PARAM_INDENT);

            ICellStyle style = editorModel.getOriginalGridTable().getCell(col, row).getStyle();
            int currentIndent = style != null ? style.getIndent() : 0;
            int resultIndent = currentIndent + indent;

            try {
                editorModel.setIndent(row, col, resultIndent >= 0 ? resultIndent : 0);
            } catch (Exception e) {
                log.error(ERROR_SET_STYLE, e);
                response.setMessage(ERROR_SET_STYLE);
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
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null) {
            IFormatter newFormatter = getCellFormatter(newEditor, editorModel, row, col);
            String message = null;
            try {
                editorModel.setCellValue(row, col, value, newFormatter);
            } catch (FormulaParseException ex) {
                log.warn("ERROR_SET_NEW_VALUE", ex);
                message = ERROR_SET_NEW_VALUE + ex.getMessage();
            }

            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            if (message != null) {
                response.setMessage(message);
            } else {
                response.setHtml(render(editorId));
            }
            return pojo2json(response);
        }
        return null;
    }

    private IFormatter getCellFormatter(String cellEditor, TableEditorModel editorModel, int row, int col) {
        IFormatter formatter = null;

        if (ICellEditor.CE_FORMULA.equals(cellEditor)) {
            ICell cell = editorModel.getOriginalGridTable().getCell(col, row);

            CellMetaInfo meta = getMetaInfo(editorModel, row, col);
            IFormatter currentFormatter = XlsDataFormatterFactory.getFormatter(cell, meta);
            IFormatter formulaResultFormatter = null;
            if (!(currentFormatter instanceof FormulaFormatter)) {
                formulaResultFormatter = currentFormatter;
            }
            formatter = new FormulaFormatter(formulaResultFormatter);

        } else if (ICellEditor.CE_TEXT.equals(cellEditor) || ICellEditor.CE_MULTILINE.equals(cellEditor)) {
            formatter = new DefaultFormatter();
        } else if (cellEditor == null) {
            // Format must be same as in DateEditor.js
            ICell cell = editorModel.getOriginalGridTable().getCell(col, row);
            CellMetaInfo meta = getMetaInfo(editorModel, row, col);
            ICellEditor editor = new CellEditorSelector().selectEditor(cell, meta);
            if (ICellEditor.CE_DATE.equals(editor.getEditorTypeAndMetadata().getEditor())) {
                return FormattersManager.getFormatter(Date.class);
            }
        }

        return formatter;
    }

    public String setProperty() {
        String name = getRequestParam(Constants.REQUEST_PARAM_PROP_NAME);
        String value = getRequestParam(Constants.REQUEST_PARAM_PROP_VALUE);
        String editorId = getEditorId();
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null) {
            editorModel.setProperty(name, value);
            PropertyModificationResponse response = new PropertyModificationResponse(editorModel);
            if (!editorModel.isBusinessView()) {
                response.setHtml(render(editorId));
            }
            if (StringUtils.isBlank(value)) {
                ITableProperties props = editorModel.getTable().getProperties();
                InheritanceLevel inheritanceLevel = props.getPropertyLevelDefinedOn(name);
                if (InheritanceLevel.MODULE.equals(inheritanceLevel) || InheritanceLevel.CATEGORY
                    .equals(inheritanceLevel)) {
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
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            String align = getRequestParam(Constants.REQUEST_PARAM_ALIGN);
            HorizontalAlignment halign = null;
            if ("left".equalsIgnoreCase(align)) {
                halign = HorizontalAlignment.LEFT;
            } else if ("center".equalsIgnoreCase(align)) {
                halign = HorizontalAlignment.CENTER;
            } else if ("right".equalsIgnoreCase(align)) {
                halign = HorizontalAlignment.RIGHT;
            } else if ("justify".equalsIgnoreCase(align)) {
                halign = HorizontalAlignment.JUSTIFY;
            }

            if (halign != null) {
                ICellStyle style = editorModel.getOriginalGridTable().getCell(col, row).getStyle();
                if (style == null || style.getHorizontalAlignment() != halign) {
                    try {
                        editorModel.setAlignment(row, col, halign);
                    } catch (Exception e) {
                        log.error(ERROR_SET_STYLE, e);
                        response.setMessage(ERROR_SET_STYLE);
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
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            Boolean bold = getRequestBooleanParam(Constants.REQUEST_PARAM_FONT_BOLD);

            if (bold != null) {
                ICellFont font = editorModel.getOriginalGridTable().getCell(col, row).getFont();
                if (font == null || font.isBold() != bold) {
                    try {
                        editorModel.setFontBold(row, col, bold);
                    } catch (Exception e) {
                        response.setMessage(ERROR_SET_STYLE);
                        log.error(ERROR_SET_STYLE, e);
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
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            Boolean italic = getRequestBooleanParam(Constants.REQUEST_PARAM_FONT_ITALIC);

            if (italic != null) {
                ICellFont font = editorModel.getOriginalGridTable().getCell(col, row).getFont();
                if (font == null || font.isItalic() != italic) {
                    try {
                        editorModel.setFontItalic(row, col, italic);
                    } catch (Exception e) {
                        log.error(ERROR_SET_STYLE, e);
                        response.setMessage(ERROR_SET_STYLE);
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
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            Boolean underlined = getRequestBooleanParam(Constants.REQUEST_PARAM_FONT_UNDERLINE);

            if (underlined != null) {
                ICellFont font = editorModel.getOriginalGridTable().getCell(col, row).getFont();
                if (font == null || font.isUnderlined() != underlined) {
                    try {
                        editorModel.setFontUnderline(row, col, underlined);
                    } catch (Exception e) {
                        log.error(ERROR_SET_STYLE, e);
                        response.setMessage(ERROR_SET_STYLE);
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
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            String colorStr = getRequestParam(Constants.REQUEST_PARAM_COLOR);

            if (colorStr != null) {
                ICellStyle style = editorModel.getOriginalGridTable().getCell(col, row).getStyle();
                short[] color = parseColor(colorStr);
                short[] currentColor = style != null ? style.getFillForegroundColor() : null;

                if (color.length == 3 && (currentColor == null || color[0] != currentColor[0] || // red
                        color[1] != currentColor[1] || // green
                        color[2] != currentColor[2])) { // blue
                    try {
                        editorModel.setFillColor(row, col, color);
                    } catch (Exception e) {
                        log.error(ERROR_SET_STYLE, e);
                        response.setMessage(ERROR_SET_STYLE);
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
        TableEditorModel editorModel = startEditing(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            String colorStr = getRequestParam(Constants.REQUEST_PARAM_COLOR);

            if (colorStr != null) {
                ICellFont font = editorModel.getOriginalGridTable().getCell(col, row).getFont();
                short[] color = parseColor(colorStr);
                short[] currentColor = font != null ? font.getFontColor() : null;

                if (color.length == 3 && (currentColor == null || color[0] != currentColor[0] || // red
                        color[1] != currentColor[1] || // green
                        color[2] != currentColor[2])) { // blue
                    try {
                        editorModel.setFontColor(row, col, color);
                    } catch (Exception e) {
                        log.error(ERROR_SET_STYLE, e);
                        response.setMessage(ERROR_SET_STYLE);
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
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            if (editorModel.hasUndo()) {
                editorModel.undo();
                response.setHtml(render(editorId));
            } else {
                response.setMessage("No actions to undo");
            }
            return pojo2json(response);
        }
        return null;
    }

    public String redo() {
        String editorId = getEditorId();
        TableEditorModel editorModel = getEditorModel(editorId);
        if (editorModel != null) {
            TableModificationResponse response = new TableModificationResponse(null, editorModel);
            if (editorModel.hasRedo()) {
                editorModel.redo();
                response.setHtml(render(editorId));
            } else {
                response.setMessage("No actions to redo");
            }
            return pojo2json(response);
        }
        return null;
    }

    public String saveTable() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        if (editorModel == null) {
            return null;
        }
        TableModificationResponse response = new TableModificationResponse(null, editorModel);
        if (hasEmptyRow(editorModel)) {
            response.setMessage("Sorry! Cannot save the table with empty row inside.");
        } else {
            try {
                if (beforeSave()) {
                    String newId = editorModel.save();
                    afterSave(newId);
                    response.setId(newId);
                }
            } catch (IOException e) {
                log.warn(ERROR_SAVE_TABLE, e);
                response.setMessage(ERROR_OPENED_EXCEL);
            } catch (Exception e) {
                log.error(ERROR_SAVE_TABLE, e);
                response.setMessage(ERROR_SAVE_TABLE);
            }
        }
        return pojo2json(response);
    }

    private boolean hasEmptyRow(TableEditorModel editorModel) {
        IGridTable gridTable = editorModel.getOriginalGridTable();
        int height = gridTable.getHeight();
        for (int i = 0; i < height; i++) {
            IGridTable row = gridTable.getRow(i);
            if (row != null && isEmptyRow(row)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmptyRow(IGridTable row) {
        int width = row.getWidth();
        for (int i = 0; i < width; i++) {
            if (row.getCell(i, 0).getStringValue() != null) {
                return false;
            }
        }
        return true;
    }

    public void rollbackTable() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        if (editorModel != null) {
            editorModel.cancel();
            removeEditorModel();
        }
    }

    private boolean beforeSave() {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        String beforeSaveAction = editorModel.getBeforeSaveAction();
        if (beforeSaveAction != null) {
            return (Boolean) invokeMethodExpression(beforeSaveAction);
        }
        return true;
    }

    private void afterSave(String newId) {
        TableEditorModel editorModel = getEditorModel(getEditorId());
        String afterSaveAction = editorModel.getAfterSaveAction();
        if (afterSaveAction != null) {
            invokeMethodExpression(afterSaveAction,
                StringUtils.isNotBlank(newId) ? new String[] { newId } : null,
                StringUtils.isNotBlank(newId) ? new Class[] { String.class } : null);
        }

        removeEditorModel();
    }

    private static String pojo2json(Object pojo) {
        try {
            return "(" + JSONMapper.toJSON(pojo).render(true) + ")";
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
        private String html;
        private String message;
        private String id;
        private final TableEditorModel model;

        public TableModificationResponse(String html, String message, TableEditorModel model) {
            this.html = html;
            this.message = message;
            this.model = model;
        }

        public TableModificationResponse(String html, TableEditorModel model) {
            this.html = html;
            this.model = model;
        }

        public String getHtml() {
            return html;
        }

        public String getMessage() {
            return message;
        }

        public String getId() {
            return id;
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

        public void setHtml(String html) {
            this.html = html;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setId(String id) {
            this.id = id;
        }

    }

    public static class PropertyModificationResponse extends TableModificationResponse {

        private String inheritedValue;

        public PropertyModificationResponse(TableEditorModel model) {
            super(null, null, model);
        }

        public PropertyModificationResponse(String response,
                String status,
                TableEditorModel model,
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
