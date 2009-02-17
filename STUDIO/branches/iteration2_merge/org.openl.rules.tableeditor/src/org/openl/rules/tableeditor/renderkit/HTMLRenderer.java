package org.openl.rules.tableeditor.renderkit;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.model.ui.ActionLink;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.ICellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.rules.tableeditor.util.WebUtil;
import org.openl.rules.web.jsf.util.FacesUtils;

/**
 * Render TableEditor HTML.
 * 
 * @author Andrei Astrouski
 */
public class HTMLRenderer {

    /** New line */
    public static final String NL = "\n";

    @SuppressWarnings("unchecked")
    protected Set getResourcesWritten() {
        Map requestMap = FacesUtils.getRequestMap();
        Set resources = (Set) requestMap.get(Constants.TABLE_EDITOR_RESOURCES);
        if (resources == null) {
            resources = new HashSet();
            requestMap.put(Constants.TABLE_EDITOR_RESOURCES, resources);
        }
        return resources;
    }

    @SuppressWarnings("unchecked")
    public String renderJS(String jsPath) {
        Set resources = getResourcesWritten();
        if (resources.add(jsPath)) {
            StringBuilder result = new StringBuilder();
            result.append("<script type=\"text/javascript\" src=\"")
                .append(WebUtil.internalPath(jsPath))
                .append("\"></script>");
            return result.toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public String renderJSBody(String jsBody) {
        Set resources = getResourcesWritten();
        if (resources.add(jsBody)) {
            StringBuilder result = new StringBuilder();
            result.append("<script type=\"text/javascript\">")
                .append(jsBody)
                .append("</script>");
            return result.toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public String renderCSS(String cssPath) {
        Set resources = getResourcesWritten();
        if (resources.add(cssPath)) {
            StringBuilder result = new StringBuilder();
            result.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"")
                .append(WebUtil.internalPath(cssPath))
                .append("\"></link>");
            return result.toString();
        }
        return "";
    }

    protected String renderViewer(IGridTable table,
            List<ActionLink> actionLinks, boolean editable, String editorId) {
        StringBuilder result = new StringBuilder();
        if (table != null) {
            TableModel tableModel = TableModel.initializeTableModel(
                    new TableEditorModel(table).getUpdatedTable());
            if (tableModel != null) {
                String menuId = editorId + Constants.MENU_ID_POSTFIX;
                TableRenderer tableRenderer = new TableRenderer(tableModel);
                tableRenderer.setCellIdPrefix(editorId + Constants.CELL_ID_POSTFIX);
                if (editable || (actionLinks != null && !actionLinks.isEmpty())) {
                    result
                        .append(renderJS("js/popup/popupmenu.js"))
                        .append(renderJS("js/tableEditorMenu.js"))
                        .append(tableRenderer.renderWithMenu(menuId))
                        .append(renderActionMenu(menuId, editable, actionLinks));
                } else {
                    result.append(tableRenderer.render());
                }
            }
        }
        return result.toString();
    }

    public String render(String mode, IGridTable table,
            List<ActionLink> actionLinks, boolean editable, String cellToEdit,
            boolean inner, String editorId) {
        StringBuilder result = new StringBuilder();
        result.append("<div>")
            .append(renderCSS("css/common.css"))
            .append(renderCSS("css/menu.css"))
            .append(renderCSS("css/toolbar.css"))
            .append(renderJS("js/prototype/prototype-1.5.1.js"))
            .append(renderJS("js/ScriptLoader.js"));
        if (!inner) {
            result.append("<div id='").append(editorId).append("' class='te_'>");
        }
        if (mode == null || mode.equals(Constants.MODE_VIEW)) {
            result.append(renderViewer(table, actionLinks, editable, editorId));
        } else if (mode.equals(Constants.MODE_EDIT)) {
            result.append(renderEditor(editorId, cellToEdit));
        }
        if (!inner) {
            result.append("</div>");
        }
        result.append("</div>");
        return result.toString();
    }

    public String render(IGridTable table, String editorId) {
        return render(null, table, null, false, null, false, editorId);
    }

    protected String renderActionMenu(String menuId, boolean editable,
            List<ActionLink> actionLinks) {
        StringBuilder result = new StringBuilder();

        String editLinks = "<tr><td><a href=\"javascript:triggerEdit('"
                + menuId.replaceFirst(Constants.MENU_ID_POSTFIX, "") + "','"
                + WebUtil.internalPath("ajax/edit") + "')\">Edit</a></td></tr>"
                + "<tr><td><a href=\"javascript:triggerEditXls('"
                + WebUtil.internalPath("ajax/editXls")
                + "')\">Edit in Excel</a></td></tr>";
        String menuBegin = "<div id=\"" + menuId
                + "\" style=\"display:none;\">" + "<table cellpadding=\"1px\">"
                + (editable ? editLinks : "");
        String menuEnd = "</table>" + "</div>";

        result.append(menuBegin).append(
                actionLinks == null ? "" : renderAddActionLinks(actionLinks))
                .append(menuEnd);

        return result.toString();
    }

    protected String renderAddActionLinks(List<ActionLink> links) {
        if (links == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();

        for (ActionLink link : links) {
            result.append("<tr><td>").append("<a href=\"")
                .append(link.getAction()).append("\">")
                .append(link.getName()).append("</a>")
                .append("</td></tr>");
        }

        return result.toString();
    }

    protected String renderEditor(String editorId, String cellToEdit) {
        StringBuilder result = new StringBuilder();
        cellToEdit = cellToEdit == null ? "" : cellToEdit;
        final String tableId = editorId + Constants.TABLE_ID_POSTFIX;
        String editor = Constants.TABLE_EDITOR_PREFIX + editorId;
        result.append(renderJSBody("var " + editor + ";"))
            .append(renderJS("js/IconManager.js"))
            .append(renderJS("js/TableEditor.js"))
            .append(renderJSBody("var jsPath = \"" + WebUtil.internalPath("js/") + "\""))
            .append(renderEditorToolbar(editorId ,editor))
            .append(renderJS("js/BaseEditor.js"))
            .append(renderJS("js/TextEditor.js"))
            .append(renderJS("js/MultiLineEditor.js"))
            .append(renderJS("js/NumericEditor.js"))
            .append(renderJS("js/DropdownEditor.js"))
            //.append(renderJS("js/SuggestEditor.js"))
            //.append(renderJS("js/DateEditor.js"))
            //.append(renderJS("js/PriceEditor.js"))
            //.append(renderJS("js/MultipleChoiceEditor.js"))
            .append("<div id=\"").append(tableId).append("\"></div>")
            .append(renderJS("js/initTableEditor.js"))
            .append(renderJSBody("setTimeout(function(){" + editor // setTimeout for IE
                    + " = initTableEditor(\"" + editorId + "\", \""
                    + WebUtil.internalPath("ajax/") + "\",\"" + cellToEdit + "\")},10);"));
        return result.toString();
    }

    protected String renderEditorToolbar(String editorId, String editor) {
        StringBuilder result = new StringBuilder();

        final String toolbarItemSeparator = "<img src=" + WebUtil.internalPath("img/toolbarSeparator.gif")
            + " class=\"item_separator\"></img>";

        result
            .append("<div class=\"te_toolbar\">")
            .append(renderEditorToolbarItem(editorId + "_save_all", editor, "img/Save.gif", "save()", "Save"))
            .append(renderEditorToolbarItem(editorId + "_undo", editor, "img/Undo.gif", "undoredo()", "Undo"))
            .append(renderEditorToolbarItem(editorId + "_redo", editor, "img/Redo.gif", "undoredo(true)", "Redo"))
            .append(toolbarItemSeparator)
            .append(renderEditorToolbarItem(editorId + "_add_row_before", editor, "img/b_row_ins.gif",
                    "doRowOperation(TableEditor.Constants.ADD_BEFORE)", "Add row"))
            .append(renderEditorToolbarItem(editorId + "_remove_row", editor, "img/row_del.gif",
                    "doRowOperation(TableEditor.Constants.REMOVE)", "Remove row"))
            .append(renderEditorToolbarItem(editorId + "_move_row_down", editor, "img/b_row_ins.gif",
                    "doRowOperation(TableEditor.Constants.MOVE_DOWN)", "Move row down"))
            .append(renderEditorToolbarItem(editorId + "_move_row_up", editor, "img/b_row_ins.gif",
                    "doRowOperation(TableEditor.Constants.MOVE_UP)", "Move row up"))
            .append(toolbarItemSeparator)
            .append(renderEditorToolbarItem(editorId + "_add_column_before", editor, "img/b_col_ins.gif",
                    "doColOperation(TableEditor.Constants.ADD_BEFORE)", "Add column"))
            .append(renderEditorToolbarItem(editorId + "_remove_column", editor, "img/col_del.gif",
                    "doColOperation(TableEditor.Constants.REMOVE)", "Remove column"))
            .append(renderEditorToolbarItem(editorId + "_move_column_right", editor, "img/b_row_ins.gif",
                    "doColOperation(TableEditor.Constants.MOVE_DOWN)", "Move column right"))
            .append(renderEditorToolbarItem(editorId + "_move_column_left", editor, "img/b_row_ins.gif",
                    "doColOperation(TableEditor.Constants.MOVE_UP)", "Move column left"))
            .append(toolbarItemSeparator)
            .append(renderEditorToolbarItem(editorId + "_align_left", editor, "img/alLeft.gif",
                    "setAlignment('left')", "Align left"))
            .append(renderEditorToolbarItem(editorId + "_align_center", editor, "img/alCenter.gif",
                    "setAlignment('center')", "Align center"))
            .append(renderEditorToolbarItem(editorId + "_align_right", editor, "img/alRight.gif",
                    "setAlignment('right')", "Align right"))
            .append(toolbarItemSeparator)
            .append(renderEditorToolbarItem(editorId + "_decrease_indent", editor, "img/indent_left.gif",
                    "indent('-1')", "Decrease indent"))
            .append(renderEditorToolbarItem(editorId + "_increase_indent", editor, "img/indent_right.gif",
                    "indent('1')", "Increase indent"))
            .append(toolbarItemSeparator)
            .append(renderEditorToolbarItem(editorId + "_help", null,
                    "img/help.gif", "window.open('" + WebUtil.internalPath("docs/help.html") + "');", "Help"))
            .append("</div>");

        return result.toString();
    }

    protected String renderEditorToolbarItem(String itemId, String editor, String imgSrc,
            String action, String title) {
        editor = (editor == null || editor.equals("")) ? "" : editor + ".";
        StringBuilder result = new StringBuilder();
        result.append("<img id=\"").append(itemId)
            .append("\" src=\"").append(WebUtil.internalPath(imgSrc))
            .append("\" title=\"").append(title)
            .append("\" onclick=\"").append(editor).append(action)
            .append("\" onmouseover=\"this.className='item_over'\"")
            .append(" onmouseout=\"this.className='item_enabled'\"")
            .append("></img>");
        return result.toString();
    }

    /**
     * Render HTML table by table model.
     * 
     * @author Andrey Naumenko
     */
    public static class TableRenderer {
        private final TableModel tableModel;
        private String cellIdPrefix;

        public TableRenderer(TableModel tableModel) {
            this.tableModel = tableModel;
        }

        public void setCellIdPrefix(String prefix) {
            cellIdPrefix = prefix;
        }

        public String render(String extraTDText, boolean embedCellURI) {
            String tdPrefix = "<td";
            if (extraTDText != null) {
                tdPrefix += " ";
                tdPrefix += extraTDText;
            }
            final String prefix = cellIdPrefix != null ? cellIdPrefix : Constants.CELL_ID_POSTFIX;

            IGridTable table = tableModel.getGridTable();
            StringBuffer s = new StringBuffer();
            s.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");

            for (int i = 0; i < tableModel.getCells().length; i++) {
                s.append("<tr>\n");
                for (int j = 0; j < tableModel.getCells()[i].length; j++) {
                    ICellModel cell = tableModel.getCells()[i][j];
                    if ((cell == null) || !cell.isReal()) {
                        continue;
                    }

                    s.append(tdPrefix);
                    if (cell instanceof CellModel) {
                        ((CellModel) (cell)).atttributesToHtml(s, tableModel);
                    }

                    StringBuilder id = new StringBuilder();
                    id.append(prefix).append(String.valueOf(i + 1)).append(":")
                            .append(j + 1);

                    s.append(" id=\"").append(id).append("\">");
                    if (embedCellURI) {
                        s.append("<input name=\"uri\" type=\"hidden\" value=\"")
                            .append(table.getUri(j, i)).append("\"></input>");
                    }
                    s.append(cell.getContent()).append("</td>\n");
                }
                s.append("</tr>\n");
            }
            s.append("</table>");
            return s.toString();
        }

        public String render() {
            return render(null, false);
        }

        public String renderWithMenu(String menuId) {
            menuId = menuId == null ? "" : menuId;
            return render("onmouseover=\"openMenu('" + menuId
                    + "',this,event)\" onmouseout=\"closeMenu(this)\"", true);
        }
    }

}
