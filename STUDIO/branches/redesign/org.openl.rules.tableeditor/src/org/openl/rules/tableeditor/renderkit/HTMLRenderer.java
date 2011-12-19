package org.openl.rules.tableeditor.renderkit;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.tableeditor.model.ui.ActionLink;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.ICellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.rules.tableeditor.util.WebUtil;

/**
 * Render TableEditor HTML.
 * 
 * @author Andrei Astrouski
 */
public class HTMLRenderer {

    /** New line */
    public static final String NL = "\n";

    public static final int ALL_ROWS = -1;
    public static final int MAX_NUM_CELLS = 1500;

    @SuppressWarnings("unchecked")
    protected Set getResourcesWritten() {
        Map<String, Object> requestMap = FacesUtils.getRequestMap();
        Set resources = (Set) requestMap.get(Constants.TABLE_EDITOR_RESOURCES);
        if (resources == null) {
            resources = new HashSet();
            requestMap.put(Constants.TABLE_EDITOR_RESOURCES, resources);
        }
        return resources;
    }

    public String render(TableEditor editor) {
        return render(editor, null, null, null);
    }

    public String render(TableEditor editor, String cellToEdit,
            List<ActionLink> actionLinks, String errorCell) {
        StringBuilder result = new StringBuilder();
        result.append("<div>")
            .append(renderCSS("css/common.css"))
            .append(renderCSS("css/menu.css"))
            .append(renderCSS("css/tooltip.css"));
        if (editor.isEditable()) {
            result.append(renderCSS("css/toolbar.css"))
                .append(renderCSS("css/datepicker.css"))
                .append(renderCSS("css/multiselect.css"))
                .append(renderCSS("css/colorPicker.css"))
                .append(renderCSS("css/popup.css"));
        }
        if (!Constants.THIRD_PARTY_LIBS_PROTOTYPE.equalsIgnoreCase(editor.getExcludeScripts())) {
            result.append(renderJS("js/prototype/prototype-1.6.1.js"));
        }
        result.append(renderJS("js/tooltip.js"))
            .append(renderJS("js/ScriptLoader.js"))
            .append(renderJS("js/AjaxHelper.js"))
            .append(renderJS("js/TableEditor.js"))
            .append(renderJS("js/popup/popupmenu.js"));
            if (editor.isEditable()) {
                result.append(renderJS("js/BaseEditor.js"))
                    .append(renderJS("js/BaseTextEditor.js"))
                    .append(renderJS("js/datepicker.packed.js"))
                    .append(renderJS("js/TextEditor.js"))
                    .append(renderJS("js/MultiLineEditor.js"))
                    .append(renderJS("js/NumericEditor.js"))
                    .append(renderJS("js/DropdownEditor.js"))
                    .append(renderJS("js/FormulaEditor.js"))
                    .append(renderJS("js/BooleanEditor.js"))
                    .append(renderJS("js/DateEditor.js"))
                    .append(renderJS("js/MultiselectEditor.js"))
                    .append(renderJS("js/ArrayEditor.js"));
            }

        result.append("<div id='").append(editor.getId()).append("' class='te_'>");

        String mode = editor.getMode();

        if (Constants.MODE_EDIT.equals(mode)) {
            result.append(renderEditor(editor, cellToEdit, errorCell));
        } else {
            result.append(renderViewer(editor, actionLinks, errorCell));
        }

        result.append("</div>");

        if (editor.getTable() != null) {
            IGridFilter[] filters = (editor.getFilter() == null) ? null : new IGridFilter[] { editor.getFilter() };
            IGridTable table = editor.getTable().getGridTable(editor.getView());
            int numRows = getMaxNumRowsToDisplay(table);
            TableModel tableModel = TableModel.initializeTableModel(table, filters, numRows,
                    editor.getLinkBase(), editor.getLinkTarget());

            if (tableModel != null) {
                
                TableRenderer tableRenderer = new TableRenderer(tableModel);
                String tableId = editor.getId() + Constants.ID_POSTFIX_TABLE;

                result.append("<div id=\"").append(tableId).append("\">");
                if (editor.isEditable() || CollectionUtils.isNotEmpty(actionLinks)) {
                    String menuId = editor.getId() + Constants.ID_POSTFIX_MENU;
                    result.append(tableRenderer.renderWithMenu(editor, menuId, errorCell));
                } else {
                    result.append(tableRenderer.render(null, editor.isShowFormulas(), null, editor.getId()));
                }
                result.append("</div>");

                String editorJsVar = Constants.TABLE_EDITOR_PREFIX + editor.getId()
                        // name of js variable can't contain ':' symbol
                        .replaceAll(":", "_");

                String beforeSave = getEditorJSAction(editor.getOnBeforeSave());
                String afterSave = getEditorJSAction(editor.getOnAfterSave());
                String saveFailure = getEditorJSAction(editor.getOnSaveFailure());

                String actions = "{beforeSave:" + beforeSave + ",afterSave:" + afterSave + ",saveFailure:" + saveFailure + "}";

                result.append(renderJSBody("var " + editorJsVar + " = initTableEditor(\"" + editor.getId() + "\", \""
                        + WebUtil.internalPath("ajax/") + "\",\"" + cellToEdit + "\"," + actions + ","
                        + (Constants.MODE_EDIT.equals(mode) ? 1 : 0) + "," + editor.isEditable() + ");"));
            }
        }

        result.append("</div>");
        return result.toString();
    }

    protected String renderActionMenu(String menuId, boolean editable, List<ActionLink> actionLinks) {
        StringBuilder result = new StringBuilder();

        String editorJsVar = Constants.TABLE_EDITOR_PREFIX + menuId.replaceFirst(Constants.ID_POSTFIX_MENU, "");

        String editLink = "<tr><td><a href=\"javascript:" + editorJsVar + ".toEditMode();\">Edit</a></td></tr>";
        String menuBegin = "<div id=\"" + menuId + "\" style=\"display:none;\">" + "<table cellpadding=\"1px\">"
                + (editable ? editLink : "");
        String menuEnd = "</table>" + "</div>";

        result.append(menuBegin).append(actionLinks == null ? "" : renderAddActionLinks(actionLinks)).append(menuEnd);

        return result.toString();
    }

    protected String renderAddActionLinks(List<ActionLink> links) {
        if (links == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();

        for (ActionLink link : links) {
            result.append("<tr><td>").append("<a href=\"").append(link.getAction()).append("\">")
                    .append(link.getName()).append("</a>").append("</td></tr>");
        }

        return result.toString();
    }

    @SuppressWarnings("unchecked")
    public String renderCSS(String cssPath) {
        Set resources = getResourcesWritten();
        if (resources.add(cssPath)) {
            StringBuilder result = new StringBuilder();
            result.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(WebUtil.internalPath(cssPath))
                    .append("\"></link>");
            return result.toString();
        }
        return "";
    }

    public String getSingleSelectComponentCode(String componentId, String[] values, String[] displayValues, String value) {

        String choisesString = "\"" + StringUtils.join(values, "\", \"") + "\"";
        String displayValuesString = "\"" + StringUtils.join(displayValues, "\", \"") + "\"";

        String params = String.format(
                "{choices : [%s], displayValues : [%s]}",
                choisesString, displayValuesString);

        String id = componentId == null ? StringUtils.EMPTY : componentId;

        String jsCode = String.format("new DropdownEditor('', '%s', %s, '%s', '');", id, params, StringEscapeUtils
                .escapeJavaScript(value));

        return jsCode;
    }

    public String getMultiSelectComponentCode(String componentId, String[] values, String[] displayValues, String value) {

        String choisesString = "\"" + StringUtils.join(values, "\", \"") + "\"";
        String displayValuesString = "\"" + StringUtils.join(displayValues, "\", \"") + "\"";

        String params = String.format(
                "{choices : [%s], displayValues : [%s], separator : \",\", separatorEscaper : \"&#92;&#92;&#92;&#92;\"}",
                choisesString, displayValuesString);

        String id = componentId == null ? StringUtils.EMPTY : componentId;

        String jsCode = String.format("new MultiselectEditor('', '%s', %s, '%s', '');", id, params, StringEscapeUtils
                .escapeJavaScript(value));

        return jsCode;
    }

    public String renderEditor(TableEditor editor, String cellToEdit, String errorCell) {
        StringBuilder result = new StringBuilder();
        cellToEdit = cellToEdit == null ? "" : cellToEdit;

        String editorJsVar = Constants.TABLE_EDITOR_PREFIX + editor.getId();

        result.append(renderEditorToolbar(editor.getId(), editorJsVar))
            .append(renderJS("js/colorPicker.js"))
            .append(renderJS("js/popup.js"));

        return result.toString();
    }

    protected String getEditorJSAction(String action) {
        return StringUtils.isBlank(action) ? "''" : "function() {" + action + "}";
    }

    protected String renderEditorToolbar(String editorId, String editorJsVar) {
        StringBuilder result = new StringBuilder();

        final String toolbarItemSeparator = "<img src=" + WebUtil.internalPath("img/toolbarSeparator.gif")
                + " class=\"item_separator\"></img>";

        result.append("<div class=\"te_toolbar\">")
            .append(renderEditorToolbarItem(editorId + "_save_all", editorJsVar, "img/Save.gif", "save()", "Save"))
            .append(renderEditorToolbarItem(editorId + "_undo", editorJsVar, "img/Undo.gif", "undoredo()", "Undo"))
            .append(renderEditorToolbarItem(editorId + "_redo", editorJsVar, "img/Redo.gif", "undoredo(true)", "Redo"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_insert_row_before", editorJsVar, "img/insert_row.gif",
                    "doTableOperation(TableEditor.Operations.INSERT_ROW_BEFORE)", "Insert row before"))
            .append(renderEditorToolbarItem(editorId + "_remove_row", editorJsVar, "img/delete_row.gif",
                    "doTableOperation(TableEditor.Operations.REMOVE_ROW)", "Remove row"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_insert_column_before", editorJsVar, "img/insert_column.gif",
                    "doTableOperation(TableEditor.Operations.INSERT_COLUMN_BEFORE)", "Insert column before"))
            .append(renderEditorToolbarItem(editorId + "_remove_column", editorJsVar, "img/delete_column.gif",
                    "doTableOperation(TableEditor.Operations.REMOVE_COLUMN)", "Remove column"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_align_left", editorJsVar, "img/alLeft.gif",
                    "setAlignment('left')", "Align left"))
            .append(renderEditorToolbarItem(editorId + "_align_center", editorJsVar, "img/alCenter.gif",
                    "setAlignment('center')", "Align center"))
            .append(renderEditorToolbarItem(editorId + "_align_right", editorJsVar, "img/alRight.gif",
                    "setAlignment('right')", "Align right"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_font_bold", editorJsVar, "img/bold.png",
                    "setFontBold('" + editorId + "_font_bold" + "')", "Bold"))
            .append(renderEditorToolbarItem(editorId + "_font_italic", editorJsVar, "img/italic.png",
                    "setFontItalic('" + editorId + "_font_italic" + "')", "Italic"))
            .append(renderEditorToolbarItem(editorId + "_font_underline", editorJsVar, "img/underline.png",
                    "setFontUnderline('" + editorId + "_font_underline" + "')", "Underline"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_fill_color", editorJsVar, "img/fillColor.png",
                    "selectFillColor('" + editorId + "_fill_color" + "')", "Fill color"))
            .append(renderEditorToolbarItem(editorId + "_font_color", editorJsVar, "img/fontColor.png",
                    "selectFontColor('" + editorId + "_font_color" + "')", "Font color"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_decrease_indent", editorJsVar, "img/indent_left.gif",
                    "indent('-1')", "Decrease indent"))
            .append(renderEditorToolbarItem(editorId + "_increase_indent", editorJsVar, "img/indent_right.gif",
                    "indent('1')", "Increase indent"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_help", null, "img/help.gif", "window.open('"
                                + WebUtil.internalPath("docs/help.html") + "');", "Help"))

            .append("</div>");

        return result.toString();
    }

    protected String renderEditorToolbarItem(String itemId, String editor, String imgSrc, String action, String title) {
        editor = (editor == null || editor.equals("")) ? "" : editor + ".";
        StringBuilder result = new StringBuilder();
        result.append("<img id=\"").append(itemId)
            .append("\" src=\"").append(WebUtil.internalPath(imgSrc))
            .append("\" title=\"").append(title)
            .append("\" onclick=\"").append(editor).append(action)
            .append("\" class='te_toolbar_item te_toolbar_item_disabled'")
            .append("></img>");
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    public String renderJS(String jsPath) {
        Set resources = getResourcesWritten();
        if (resources.add(jsPath)) {
            StringBuilder result = new StringBuilder();
            result.append("<script type=\"text/javascript\" src=\"").append(WebUtil.internalPath(jsPath)).append(
                    "\"></script>");
            return result.toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public String renderJSBody(String jsBody) {
        Set resources = getResourcesWritten();
        if (resources.add(jsBody)) {
            StringBuilder result = new StringBuilder();
            result.append("<script type=\"text/javascript\">").append(jsBody).append("</script>");
            return result.toString();
        }
        return "";
    }

    protected String renderViewer(TableEditor editor, List<ActionLink> actionLinks, String errorCell) {
        StringBuilder result = new StringBuilder();
        if (editor.getTable() != null && (editor.isEditable() || CollectionUtils.isNotEmpty(actionLinks))) {
            String menuId = editor.getId() + Constants.ID_POSTFIX_MENU;
            result.append(renderActionMenu(menuId, editor.isEditable(), actionLinks));
        }
        return result.toString();
    }

    /**
     * Returns max number of rows to display.
     * 
     * @return number of rows to display or -1 for all rows
     */
    public static int getMaxNumRowsToDisplay(IGridTable table) {
        IGridRegion region = table.getRegion();
        int cols = IGridRegion.Tool.width(region);
        int rows = IGridRegion.Tool.height(region);

        int numCells = rows * cols;

        if (numCells > MAX_NUM_CELLS) {
            int extraCells = numCells - MAX_NUM_CELLS;
            int extraRows = extraCells / cols;
            return rows - extraRows;
        }

        return ALL_ROWS;
    }

    /**
     * Render HTML table by table model.
     * 
     * @author Andrey Naumenko
     */
    public static class TableRenderer {

        private final TableModel tableModel;

        public TableRenderer(TableModel tableModel) {
            this.tableModel = tableModel;
        }

        public String render(boolean showFormulas) {
            return render(null, showFormulas, null, "");
        }

        public String render(String extraTDText, boolean showFormulas, String errorCell, String editorId) {
            String tdPrefix = "<td";
            if (extraTDText != null) {
                tdPrefix += " ";
                tdPrefix += extraTDText;
            }

            IGridTable table = tableModel.getGridTable();

            StringBuilder s = new StringBuilder();
            s.append("<table class=\"te_table\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");

            for (int row = 0; row < tableModel.getCells().length; row++) {
                s.append("<tr>\n");
                for (int col = 0; col < tableModel.getCells()[row].length; col++) {

                    ICellModel cell = tableModel.getCells()[row][col];
                    if ((cell == null) || !cell.isReal()) {
                        continue;
                    }

                    String cellUri = null;
                    if (table != null) {
                        cellUri = table.getUri(col, row);
                    }

                    s.append(tdPrefix);
                    if (cell instanceof CellModel) {
                        boolean selectErrorCell = false;
                        if (cellUri != null) {
                            XlsUrlParser uriParser = new XlsUrlParser();
                            uriParser.parse(cellUri);
                            if (uriParser.range.equals(errorCell)) {
                                selectErrorCell = true;
                            }
                        }
                        ((CellModel) (cell)).atttributesToHtml(s, tableModel, selectErrorCell);
                    }

                    StringBuilder cellId = new StringBuilder();
                    cellId.append(editorId).append(Constants.ID_POSTFIX_CELL)
                        .append(String.valueOf(row + 1)).append(":").append(col + 1);

                    s.append(" id=\"").append(cellId).append("\"");
                    if (cell.getComment() != null) {
                        s.append(" class=\"te_comment\"");
                    }
                    s.append(">");
                    String cellContent = cell.getContent(showFormulas);
                    if (cellContent != null) {
                        cellContent.replaceAll("", "");
                    }
                    s.append(cellContent).append("</td>\n");
                    if (cell.getComment() != null) {
                        s.append("<script type=\"text/javascript\">")
                            .append("new Tooltip('" + cellId + "','" + StringEscapeUtils.escapeJavaScript(
                                    cell.getComment().replaceAll("\\n", "<br/>"))
                                    + "', {hideOn:['mouseout','dblclick'], position:'right_bottom', maxWidth:'160px'});")
                            .append("</script>");
                    }
                }
                s.append("</tr>\n");
            }
            s.append("</table>");

            if (tableModel.getNumRowsToDisplay() > -1) {
                s.append("<div class='te_bigtable_mes'>")
                .append("<div class='te_bigtable_mes_header'>The table is displayed partially (the first "
                        + tableModel.getNumRowsToDisplay() + " rows).</div>")
                .append("<div>To view the full table, use 'Edit In Excel'.</div>")
                .append("</div>");
            }

            return s.toString();
        }

        public String renderWithMenu(TableEditor editor, String menuId, String errorCell) {
            menuId = menuId == null ? "" : menuId;
            String eventHandlers = "onmousedown=\"openMenu('" + menuId + "',this,event)\"";
            return render(eventHandlers, editor.isShowFormulas(), errorCell, editor.getId());
        }
    }

}
