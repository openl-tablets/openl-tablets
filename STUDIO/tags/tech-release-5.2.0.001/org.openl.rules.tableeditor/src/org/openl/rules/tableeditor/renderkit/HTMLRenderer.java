package org.openl.rules.tableeditor.renderkit;

import java.util.List;

import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.TableEditorModel;
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

    public String renderJS(String jsPath) {
        StringBuilder result = new StringBuilder();
        result.append("<script type=\"text/javascript\" src=\"")
            .append(WebUtil.internalPath(jsPath))
            .append("\"></script>");
        return result.toString();
    }

    public String renderJSBody(String jsBody) {
        StringBuilder result = new StringBuilder();
        result.append("<script type=\"text/javascript\">")
            .append(jsBody)
            .append("</script>");
        return result.toString();
    }

    public String renderCSS(String cssPath) {
        StringBuilder result = new StringBuilder();
        result.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"")
            .append(WebUtil.internalPath(cssPath))
            .append("\"></link>");
        return result.toString();
    }

    protected String renderViewer(IGridTable table, List<ActionLink> actionLinks,
            boolean editable) {
        StringBuilder result = new StringBuilder();
        if (table != null) {
            TableModel tableModel = TableModel.initializeTableModel(
                    new TableEditorModel(table).getUpdatedTable());
            if (tableModel != null) {
                TableRenderer tableRenderer = new TableRenderer(tableModel);
                if (editable || (actionLinks != null && !actionLinks.isEmpty())) {
                    result.append(renderCSS("css/menu.css"))
                        .append(renderJS("js/popup/popupmenu.js"))
                        .append(renderJS("js/tableEditorMenu.js"))
                        .append(tableRenderer.renderWithMenu())
                        .append(renderActionMenu(editable, actionLinks));
                } else {
                    result.append(tableRenderer.render());
                }
            }
        }
        return result.toString();
    }

    public String render(String mode, IGridTable table, List<ActionLink> actionLinks,
            boolean editable, String cellToEdit, boolean inner) {
        StringBuilder result = new StringBuilder();
            result.append("<div>");
            if (!inner) {
                result.append(renderCSS("css/common.css"))
                    .append(renderCSS("css/toolbar.css"))
                    .append(renderJS("js/prototype/prototype-1.5.1.js"))
                    .append(renderJS("js/ScriptLoader.js"))
                    .append("<div id='te_' class='te_'>");
        }
        if (mode == null || mode.equals(Constants.MODE_VIEW)) {
            result.append(renderViewer(table, actionLinks, editable));
        } else if (mode.equals(Constants.MODE_EDIT)) {
            result.append(renderEditor(cellToEdit))
                .append("<div id='contextMenu'></div>");
        }
        if (!inner) {
            result.append("</div>");
        }
        result.append("</div>");
        return result.toString();
    }

    public String render(IGridTable table) {
        return render(null, table, null, false, null, false);
    }

    protected String renderViewer(IGridTable table, List<ActionLink> actionLinks) {
        return renderViewer(table, actionLinks, false);
    }
    
    protected String renderViewer(IGridTable table) {
        return renderViewer(table, null, false);
    }
    
    protected String renderActionMenu(boolean editable, List<ActionLink> actionLinks) {
        StringBuilder result = new StringBuilder();
        
        String editLinks = "<tr><td><a href=\"javascript:triggerEdit('" + WebUtil.internalPath("ajax/edit")
            + "')\">Edit</a></td></tr>"
            + "<tr><td><a href=\"javascript:triggerEditXls('" + WebUtil.internalPath("ajax/editXls")
            + "')\">Edit in Excel</a></td></tr>";
        String menuBegin =
            "<div id=\"contextMenu\" style=\"display:none;\">"
                + "<table cellpadding=\"1px\">"
                    + (editable ? editLinks : "");
        String menuEnd = "</table>" + "</div>";

        result.append(menuBegin)
            .append(actionLinks == null ? "" : renderAddActionLinks(actionLinks))
            .append(menuEnd);
        
        return result.toString();
    }

    protected String renderAddActionLinks(List<ActionLink> links) {
        if (links == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        
        for (ActionLink link : links) {
            result.append("<tr><td>")
                .append("<a href=\"").append(link.getAction())
                .append("\">").append(link.getName()).append("</a>")
                .append("</td></tr>");
            }
        
        return result.toString();
    }

    protected String renderEditor(String cellToEdit) {
        StringBuilder result = new StringBuilder();
        cellToEdit = cellToEdit == null ? "" : cellToEdit;
        final String editorDataId = "_te_data"; 
        result
            .append(renderJS("js/TableEditor.js"))
            .append(renderJSBody("var jsPath = \"" + WebUtil.internalPath("js/") + "\""))
            .append(renderEditorToolbar())
            .append(renderJS("js/BaseEditor.js"))
            .append(renderJS("js/TextEditor.js"))
            .append(renderJS("js/MultiLineEditor.js"))
            .append(renderJS("js/NumericEditor.js"))
            .append(renderJS("js/DropdownEditor.js"))
            //.append(renderJS("js/SuggestEditor.js"))
            //.append(renderJS("js/DateEditor.js"))
            //.append(renderJS("js/PriceEditor.js"))
            //.append(renderJS("js/MultipleChoiceEditor.js"))
            .append("<div id=\"").append(editorDataId).append("\"></div>")
            .append(renderJS("js/initTableEditor.js"))
            .append(renderJSBody("setTimeout(function(){initTableEditor(\"" //setTimeout for IE
                + editorDataId + "\", \""
                + WebUtil.internalPath("ajax/") + "\",\"" + cellToEdit + "\")},10);"));
        return result.toString();
    }

    protected String renderEditorToolbar() {
        StringBuilder result = new StringBuilder();

        final String toolbarItemSeparator = "<img src=" + WebUtil.internalPath("img/toolbarSeparator.gif")
            + " class=\"item_separator\"></img>";
        
        result.append(renderJS("js/IconManager.js"))
            .append("<div class=\"te_toolbar\">")
            .append(renderEditorToolbarItem("save_all", "img/Save.gif", "tableEditor.save()", "Save"))
            .append(renderEditorToolbarItem("undo", "img/Undo.gif", "tableEditor.undoredo()", "Undo"))
            .append(renderEditorToolbarItem("redo", "img/Redo.gif", "tableEditor.undoredo(true)", "Redo"))
            .append(toolbarItemSeparator)
            .append(renderEditorToolbarItem("add_row_before_button", "img/b_row_ins.gif",
                    "tableEditor.doRowOperation(TableEditor.Constants.ADD_BEFORE)", "Add row"))
            .append(renderEditorToolbarItem("remove_row_button", "img/row_del.gif",
                    "tableEditor.doRowOperation(TableEditor.Constants.REMOVE)", "Remove row"))
            .append(renderEditorToolbarItem("move_row_down_button", "img/b_row_ins.gif",
                    "tableEditor.doRowOperation(TableEditor.Constants.MOVE_DOWN)", "Move row down"))
            .append(renderEditorToolbarItem("move_row_up_button", "img/b_row_ins.gif",
                    "tableEditor.doRowOperation(TableEditor.Constants.MOVE_UP)", "Move row up"))
            .append(toolbarItemSeparator)
            .append(renderEditorToolbarItem("add_column_before_button", "img/b_col_ins.gif",
                    "tableEditor.doColOperation(TableEditor.Constants.ADD_BEFORE)", "Add column"))
            .append(renderEditorToolbarItem("remove_column_button", "img/col_del.gif",
                    "tableEditor.doColOperation(TableEditor.Constants.REMOVE)", "Remove column"))
            .append(renderEditorToolbarItem("move_column_right_button", "img/b_row_ins.gif",
                    "tableEditor.doColOperation(TableEditor.Constants.MOVE_DOWN)", "Move column right"))
            .append(renderEditorToolbarItem("move_column_left_button", "img/b_row_ins.gif",
                    "tableEditor.doColOperation(TableEditor.Constants.MOVE_UP)", "Move column left"))
            .append(toolbarItemSeparator)
            .append(renderEditorToolbarItem("align_left", "img/alLeft.gif",
                    "tableEditor.setAlignment('left')", "Align left"))
            .append(renderEditorToolbarItem("align_center", "img/alCenter.gif",
                    "tableEditor.setAlignment('center')", "Align center"))
            .append(renderEditorToolbarItem("align_right", "img/alRight.gif",
                    "tableEditor.setAlignment('right')", "Align right"))
            .append(toolbarItemSeparator)
            .append(renderEditorToolbarItem("help", "img/help.gif",
                    "window.open('" + WebUtil.internalPath("docs/help.html") + "');", "Help"))
            .append("</div>")
            .append(renderJS("js/initIconManager.js"));
        
        return result.toString();
    }

    protected String renderEditorToolbarItem(String id, String imgSrc, String action, String title) {
        StringBuilder result = new StringBuilder();
        result.append("<img id=\"").append(id)
            .append("\" src=\"").append(WebUtil.internalPath(imgSrc))
            .append("\" title=\"").append(title)
            .append("\" onclick=\"").append(action)
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
            final String prefix = cellIdPrefix != null ? cellIdPrefix : "cell-";

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
                    id.append(prefix).append(String.valueOf(i + 1)).append(":").append(j + 1);

                    s.append(" id=\"").append(id).append("\">");
                    if (embedCellURI) {
                        s.append("<input name=\"uri\" type=\"hidden\" value=\"").append(table.getUri(j, i)).append("\"></input>");
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

        public String renderWithMenu() {
            return render("onmouseover=\"try {cellMouseOver(this,event)} catch (e){}\" onmouseout=\"try {cellMouseOut(this)} catch(e){}\"", true);
        }
    }

}


