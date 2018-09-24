package org.openl.rules.tableeditor.renderkit;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.tableeditor.model.ui.ActionLink;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.ICellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

/**
 * Render TableEditor HTML.
 * 
 * @author Andrei Astrouski
 */
public class HTMLRenderer {

    /** New line */
    public static final String NL = "\n";

    public static final int ALL_ROWS = -1;
    public static final int MAX_NUM_CELLS = 5000;

    @SuppressWarnings("unchecked")
    protected Set<String> getResourcesWritten() {
        Map<String, Object> requestMap = FacesUtils.getRequestMap();
        Set<String> resources = (Set<String>) requestMap.get(Constants.TABLE_EDITOR_RESOURCES);
        if (resources == null) {
            resources = new HashSet<>();
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
            .append(renderCSS("css/tableeditor.min.css"));

        if (!Constants.THIRD_PARTY_LIBS_PROTOTYPE.equalsIgnoreCase(editor.getExcludeScripts())) {
            result.append(renderJS("js/prototype/prototype-1.6.1.js"));
            result.append(renderJS("js/tableeditor.min.js"));
        }

        result.append("<div id='").append(editor.getId()).append("' class='te_'>");

        String mode = editor.getMode();

        String editorJsVar = Constants.TABLE_EDITOR_PREFIX + editor.getId()
           // Name of js variable can't contain ':' symbol
          .replaceAll(":", "_");

        result.append(renderEditorToolbar(editor.getId(), editorJsVar, mode));

        result.append("<div id='").append(editor.getId()).append(Constants.TABLE_EDITOR_WRAPPER_PREFIX).append("' class='te_editor_wrapper'></div>");

        IOpenLTable openLTable = editor.getTable();
        if (openLTable != null && (editor.isEditable() || CollectionUtils.isNotEmpty(actionLinks))) {
            String menuId = editor.getId() + Constants.ID_POSTFIX_MENU;
            result.append(renderActionMenu(menuId, editor.isEditable(), actionLinks));
        }

        result.append("</div>");

        if (openLTable != null) {
            IGridFilter[] filters = editor.getFilters();
            IGridTable table = openLTable.getGridTable(editor.getView());
            int numRows = getMaxNumRowsToDisplay(table);
            TableModel tableModel = TableModel.initializeTableModel(table, filters, numRows, editor.getLinkBuilder(),
                    mode, editor.getView(), openLTable.getMetaInfoReader());

            if (tableModel != null) {
                TableRenderer tableRenderer = new TableRenderer(tableModel);
                String tableId = editor.getId() + Constants.ID_POSTFIX_TABLE;

                result.append("<div id=\"").append(tableId).append("\">");
                result.append(tableRenderer.render(editor.isShowFormulas(), errorCell, editor.getId(), editor.getRowIndex()));
                result.append("</div>");

                String beforeEdit = getEditorJSAction(editor.getOnBeforeEdit());
                String beforeSave = getEditorJSAction(editor.getOnBeforeSave());
                String afterSave = getEditorJSAction(editor.getOnAfterSave());
                String error = getEditorJSAction(editor.getOnError());
                String requestStart = getEditorJSAction(editor.getOnRequestStart());
                String requestEnd = getEditorJSAction(editor.getOnRequestEnd());

                String relativeCellToEdit = getRelativeCellToEdit(cellToEdit, table, tableModel);

                String actions = "{beforeEdit:" + beforeEdit
                        + ",beforeSave:" + beforeSave
                        + ",afterSave:" + afterSave
                        + ",error:" + error
                        + ",requestStart:" + requestStart
                        + ",requestEnd:" + requestEnd
                        + "}";

                result.append(renderJSBody("var " + editorJsVar + " = initTableEditor(\"" + editor.getId() + "\", \""
                        + internalPath("ajax/") + "\",\"" + relativeCellToEdit + "\"," + actions + ","
                        + (Constants.MODE_EDIT.equals(mode) ? 1 : 0) + "," + editor.isEditable() + ");"));
            }
        }

        result.append("</div>");
        return result.toString();
    }

    private String getRelativeCellToEdit(String absoluteCellToEdit, IGridTable table, TableModel tableModel) {
        if (absoluteCellToEdit == null) {
            return null;
        }

        for (int row = 0; row < tableModel.getCells().length; row++) {
            for (int col = 0; col < tableModel.getCells()[row].length; col++) {

                ICellModel cell = tableModel.getCells()[row][col];
                if (cell == null || !cell.isReal()) {
                    continue;
                }

                String cellUri = table.getCell(col, row).getUri();
                if (cellUri.equals(absoluteCellToEdit)) {
                    return String.format("%d:%d", row + 1, col + 1);
                }
            }
        }

        return absoluteCellToEdit;
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

    public String renderCSS(String cssPath) {
        Set<String> resources = getResourcesWritten();
        if (resources.add(cssPath)) {
            return "<link rel=\"stylesheet\" href=\"" + internalPath(cssPath) + "\"></link>";
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

        return String.format("new DropdownEditor('', '%s', %s, '%s', '');", id, params, StringEscapeUtils
                .escapeEcmaScript(value));
    }

    public String getMultiSelectComponentCode(String componentId, String[] values, String[] displayValues, String value) {

        String choisesString = "\"" + StringUtils.join(values, "\", \"") + "\"";
        String displayValuesString = "\"" + StringUtils.join(displayValues, "\", \"") + "\"";

        String params = String.format(
                "{choices : [%s], displayValues : [%s], separator : \",\", separatorEscaper : \"&#92;&#92;&#92;&#92;\"}",
                choisesString, displayValuesString);

        String id = componentId == null ? StringUtils.EMPTY : componentId;

        return String.format("new MultiselectEditor('', '%s', %s, '%s', '');", id, params, StringEscapeUtils
                .escapeEcmaScript(value));
    }

    protected String getEditorJSAction(String action) {
        return StringUtils.isBlank(action) ? "''" : "function(data) {" + action + "}";
    }

    protected String renderEditorToolbar(String editorId, String editorJsVar, String mode) {
        StringBuilder result = new StringBuilder();

        final String toolbarItemSeparator = "<img src=" + internalPath("img/toolbarSeparator.gif")
                + " class=\"item_separator\"></img>";

        result.append("<div style=\"").append(mode == null || mode.equals(Constants.MODE_VIEW) ? "display:none" : "").append("\" class=\"te_toolbar\">")
            .append(renderEditorToolbarItem(editorId + "_save_all", editorJsVar, "img/Save.gif", "save()", "Save changes"))
            .append(renderEditorToolbarItem(editorId + "_undo", editorJsVar, "img/Undo.gif", "undoredo()", "Undo changes"))
            .append(renderEditorToolbarItem(editorId + "_redo", editorJsVar, "img/Redo.gif", "undoredo(true)", "Redo changes"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_insert_row_before", editorJsVar, "img/insert_row.gif",
                    "doTableOperation(TableEditor.Operations.INSERT_ROW_BEFORE)", "Insert row after")) // TODO: rename method names and fields to "after"
            .append(renderEditorToolbarItem(editorId + "_remove_row", editorJsVar, "img/delete_row.gif",
                    "doTableOperation(TableEditor.Operations.REMOVE_ROW)", "Remove row"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_insert_column_before", editorJsVar, "img/insert_column.gif",
                    "doTableOperation(TableEditor.Operations.INSERT_COLUMN_BEFORE)", "Insert column before"))
            .append(renderEditorToolbarItem(editorId + "_remove_column", editorJsVar, "img/delete_column.gif",
                    "doTableOperation(TableEditor.Operations.REMOVE_COLUMN)", "Remove column"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_align_left", editorJsVar, "img/alLeft.gif",
                    "setAlignment('left', this)", "Align the text to the left"))
            .append(renderEditorToolbarItem(editorId + "_align_center", editorJsVar, "img/alCenter.gif",
                    "setAlignment('center', this)", "Center the text"))
            .append(renderEditorToolbarItem(editorId + "_align_right", editorJsVar, "img/alRight.gif",
                    "setAlignment('right', this)", "Align the text to the right"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_font_bold", editorJsVar, "img/bold.png",
                    "setFontBold('" + editorId + "_font_bold" + "')", "Make the text bold"))
            .append(renderEditorToolbarItem(editorId + "_font_italic", editorJsVar, "img/italic.png",
                    "setFontItalic('" + editorId + "_font_italic" + "')", "Italicize the text"))
            .append(renderEditorToolbarItem(editorId + "_font_underline", editorJsVar, "img/underline.png",
                    "setFontUnderline('" + editorId + "_font_underline" + "')", "Underline the text"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_fill_color", editorJsVar, "img/fillColor.png",
                    "selectFillColor('" + editorId + "_fill_color" + "')", "Color the cell background"))
            .append(renderEditorToolbarItem(editorId + "_font_color", editorJsVar, "img/fontColor.png",
                    "selectFontColor('" + editorId + "_font_color" + "')", "Color the cell text"))

            .append(toolbarItemSeparator)

            .append(renderEditorToolbarItem(editorId + "_decrease_indent", editorJsVar, "img/indent_left.gif",
                    "indent('-1')", "Decrease indent"))
            .append(renderEditorToolbarItem(editorId + "_increase_indent", editorJsVar, "img/indent_right.gif",
                    "indent('1')", "Increase indent"))

            .append("</div>");

        return result.toString();
    }

    protected String renderEditorToolbarItem(String itemId, String editor, String imgSrc, String action, String title) {
        editor = (editor == null || editor.equals("")) ? "" : editor + ".";
        StringBuilder result = new StringBuilder();
        result.append("<img id=\"").append(itemId)
            .append("\" src=\"").append(internalPath(imgSrc))
            .append("\" title=\"").append(title)
            .append("\" onclick=\"").append(editor).append(action)
            .append("\" class='te_toolbar_item te_toolbar_item_disabled'")
            .append("></img>");
        return result.toString();
    }

    public String renderJS(String jsPath) {
        Set<String> resources = getResourcesWritten();
        if (resources.add(jsPath)) {
            return "<script src=\"" + internalPath(jsPath) + "\"></script>";
        }
        return "";
    }

    public String renderJSBody(String jsBody) {
        Set<String> resources = getResourcesWritten();
        if (resources.add(jsBody)) {
            return "<script>" + jsBody + "</script>";
        }
        return "";
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

        return getMaxNumRowsToDisplay(rows, cols);
    }

    /**
     * Returns max number of rows to display.
     *
     * @return number of rows to display or {@link #ALL_ROWS} for all rows
     */
    public static int getMaxNumRowsToDisplay(int rows, int cols) {
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
            return render(showFormulas, null, "", null);
        }

        public String render(boolean showFormulas, String errorCell, String editorId, Integer rowIndex) {
            String tdPrefix = "<td";

            IGridTable table = tableModel.getGridTable();

            StringBuilder s = new StringBuilder();
            s.append("<table class=\"te_table\">\n");

            for (int row = 0; row < tableModel.getCells().length; row++) {
                s.append("<tr>\n");
                if (rowIndex != null) {
                    s.append("<td style='padding-left:5px; padding-right:5px; border: none; width: 1px; vertical-align: middle; text-align: right;'>");
                    long rowToPrint = row - rowIndex + 2;
                    if (!tableModel.isShowHeader()){
                        rowToPrint = row - rowIndex + 4;
                    }
                    if (rowToPrint > 0) {
                        s.append(rowToPrint);
                    }
                    s.append("</td>\n");
                }
                for (int col = 0; col < tableModel.getCells()[row].length; col++) {

                    ICellModel cell = tableModel.getCells()[row][col];
                    if (cell == null || !cell.isReal()) {
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
                            if (errorCell != null && errorCell.equals(uriParser.getCell())) {
                                selectErrorCell = true;
                            }
                        }
                        ((CellModel) cell).attributesToHtml(s, selectErrorCell);
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
                    s.append(cellContent).append("</td>\n");
                    if (cell.getComment() != null) {
                        s.append("<script type=\"text/javascript\">")
                                .append("new Tooltip('").append(cellId).append("','")
                                .append(StringEscapeUtils.escapeEcmaScript(cell.getComment().replaceAll("\\n", "<br/>")))
                                .append("', {hideOn:['mouseout','dblclick'], position:'right_bottom', maxWidth:'160px'});")
                            .append("</script>");
                    }
                }
                s.append("</tr>\n");
            }
            s.append("</table>");

            if (tableModel.getNumRowsToDisplay() > -1) {
                s.append("<div class='te_bigtable_mes'>")
                .append("<div class='te_bigtable_mes_header'>The table is displayed partially (the first ")
                .append(tableModel.getNumRowsToDisplay())
                .append(" rows).</div>")
                .append("<div>To view the full table, use 'Open In Excel'.</div>")
                .append("</div>");
            }

            return s.toString();
        }
    }

    public static String internalPath(String path) {
        return FacesUtils.getContextPath() + "/faces" + Constants.TABLE_EDITOR_PATTERN + path;
    }
}
