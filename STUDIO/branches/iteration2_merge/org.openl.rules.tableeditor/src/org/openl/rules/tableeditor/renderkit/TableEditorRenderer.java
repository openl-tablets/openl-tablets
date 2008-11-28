package org.openl.rules.tableeditor.renderkit;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.ServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.EditorHelper;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.model.ui.TableRenderer;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.rules.tableeditor.util.WebUtil;
import org.openl.rules.util.net.NetUtils;
import org.openl.rules.webtools.XlsUrlParser;

public class TableEditorRenderer extends TableViewerRenderer {

    @SuppressWarnings("unchecked")
    @Override
    public void encodeBegin(FacesContext context, UIComponent component)
            throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.write("<div class='_te'>");
        encodeCSS(component, writer, "css/common.css");
        Boolean readonly = new Boolean((String) component.getAttributes()
                .get("readonly"));
        if (readonly != null && readonly) {
            super.encodeTableViewer(component, writer);
        } else {
            ExternalContext externalContext = context.getExternalContext();
            initEditorHelper(externalContext, component);
            
            Map<String, String> requestMap = externalContext.getRequestParameterMap();
            String mode = (String) requestMap.get("mode");
            if (StringUtils.isNotBlank(mode) && mode.equals("edit")) {
                String cellToEdit = (String) requestMap.get("cell");
                encodeTableEditor(cellToEdit, component, writer);
            } else if (StringUtils.isNotBlank(mode) && mode.equals("editExcel")) {
                boolean local = NetUtils.isLocalRequest(
                        (ServletRequest) externalContext.getRequest());
                String cellUri = (String) requestMap.get("cellUri");
                encodeXlsEditor(cellUri, local, writer);
                encodeTableViewer(component, writer);
            } else {
                encodeTableViewer(component, writer);
            }
        }
        writer.write("</div>");
    }

    @SuppressWarnings("unchecked")
    private void initEditorHelper (ExternalContext externalContext,
            UIComponent component) {
        Map sessionMap = externalContext.getSessionMap();
        EditorHelper editorHelper;
        synchronized (sessionMap) {
            editorHelper = (EditorHelper) sessionMap.get(
                    Constants.TABLE_EDITOR_HELPER_NAME);
            if (editorHelper == null) {
                sessionMap.put(Constants.TABLE_EDITOR_HELPER_NAME,
                        editorHelper = new EditorHelper());
            }
        }
        final Map attributes = component.getAttributes();
        IGridTable table = (IGridTable) attributes.get("table");
        editorHelper.init(table);
    }

    protected void encodeXlsEditor(String cellUri, boolean local,
            ResponseWriter writer) throws IOException {
        boolean wantURI = StringUtils.isNotBlank(cellUri);
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
            writer.write(
                    "<script type='text/javascript'>alert('This action is available only from the machine server runs at.')</script>");
        }
    }

    @Override
    protected void encodeTableViewer(UIComponent component, ResponseWriter writer) throws IOException {
        encodeCSS(component, writer, "css/menu.css");
        encodeJS(component, writer, "js/prototype/prototype-1.5.1.js");
        encodeJS(component, writer, "js/popup/popupmenu.js");
        encodeJS(component, writer, "js/tableEditorMenu.js");
        String html = "<input type='hidden' name='mode' value='' />"
            + "<input type='hidden' name='cell' value='' />"
            + "<input type='hidden' name='cellUri' value='' />";
        writer.write(html);

        IGridTable table = (IGridTable) component.getAttributes().get("table");
        TableModel tableModel = TableModel.initializeTableModel(table);
        if (tableModel != null) {
            String htmlTable = new TableRenderer(tableModel).renderWithMenu();
            writer.write(htmlTable);
        }
        encodeEditMenu(writer);
    }

    protected void encodeEditMenu(ResponseWriter writer) throws IOException {
        String htmlMenu =
            "<div id='contextMenu' style='display:none;'>"
                + "<table cellpadding='1px'>"
                    + "<tr><td><a href='javascript:triggerEdit()'>Edit</a></td></tr>"
                    + "<tr><td><a href='javascript:triggerEditXls()'>Edit in Excel</a></td></tr>"
                + "</table>"
          + "</div>";
        writer.write(htmlMenu);
    }

    protected void encodeTableEditor(String cellToEdit, UIComponent component,
            ResponseWriter writer) throws IOException {

        writer.write("<script type='text/javascript'>var jsPath = '"
                + WebUtil.internalPath("js/") + "';</script>");
        encodeJS(component, writer, "js/prototype/prototype-1.5.1.js");
        encodeJS(component, writer, "js/IconManager.js");

        encodeTableEditorToolbar(component, writer);

        encodeJS(component, writer, "js/TableEditor.js");
        encodeJS(component, writer, "js/BaseEditor.js");
        encodeJS(component, writer, "js/TextEditor.js");
        encodeJS(component, writer, "js/DropdownEditor.js");
        encodeJS(component, writer, "js/SuggestEditor.js");
        encodeJS(component, writer, "js/MultiLineEditor.js");
        encodeJS(component, writer, "js/DateEditor.js");
        encodeJS(component, writer, "js/PriceEditor.js");
        encodeJS(component, writer, "js/NumericEditor.js");
        encodeJS(component, writer, "js/MultipleChoiceEditor.js");
        writer.write("<div id='_tableEditor' />");
        writer.write("<script type='text/javascript'>"
                + "var tableEditor = new TableEditor('_tableEditor', '"
                + WebUtil.internalPath("ajax/") + "', 3, '" + cellToEdit + "');"
                + "</script>");
        encodeJS(component, writer, "js/initTableEditor.js");
    }

    protected void encodeTableEditorToolbar(UIComponent component, ResponseWriter writer) throws IOException {
        final String toolbarItemSeparator = "<img src=" + WebUtil.internalPath("img/toolbarItemTile.gif") + " class='item_separator'></img>";
        
        encodeCSS(component, writer, "css/toolbar.css");

        writer.write("<div class='_te_toolbar'>");
        writer.write(buildTableEditorToolbarItemHtml("save_all", "img/Save.gif", "tableEditor.save()", "Save"));
        writer.write(buildTableEditorToolbarItemHtml("undo", "img/Undo.gif", "tableEditor.undoredo()", "Undo"));
        writer.write(buildTableEditorToolbarItemHtml("redo", "img/Redo.gif", "tableEditor.undoredo(true)", "Redo"));
        writer.write(toolbarItemSeparator);
        writer.write(buildTableEditorToolbarItemHtml("add_row_before_button", "img/b_row_ins.gif", "tableEditor.doRowOperation(TableEditor.Constants.ADD_BEFORE)", "Add row"));
        writer.write(buildTableEditorToolbarItemHtml("remove_row_button", "img/row_del.gif", "tableEditor.doRowOperation(TableEditor.Constants.REMOVE)", "Remove row"));
        writer.write(buildTableEditorToolbarItemHtml("move_row_down_button", "img/b_row_ins.gif", "tableEditor.doRowOperation(TableEditor.Constants.MOVE_DOWN)", "Move row down"));
        writer.write(buildTableEditorToolbarItemHtml("move_row_up_button", "img/b_row_ins.gif", "tableEditor.doRowOperation(TableEditor.Constants.MOVE_UP)", "Move row up"));
        writer.write(toolbarItemSeparator);
        writer.write(buildTableEditorToolbarItemHtml("add_column_before_button", "img/b_col_ins.gif", "tableEditor.doColOperation(TableEditor.Constants.ADD_BEFORE)", "Add column"));
        writer.write(buildTableEditorToolbarItemHtml("remove_column_button", "img/col_del.gif", "tableEditor.doColOperation(TableEditor.Constants.REMOVE)", "Remove column"));
        writer.write(buildTableEditorToolbarItemHtml("move_column_right_button", "img/b_row_ins.gif", "tableEditor.doColOperation(TableEditor.Constants.MOVE_DOWN)", "Move column right"));
        writer.write(buildTableEditorToolbarItemHtml("move_column_left_button", "img/b_row_ins.gif", "tableEditor.doColOperation(TableEditor.Constants.MOVE_UP)", "Move column left"));
        writer.write(toolbarItemSeparator);
        writer.write(buildTableEditorToolbarItemHtml("align_left", "img/alLeft.gif", "tableEditor.setAlignment('left')", "Align left"));
        writer.write(buildTableEditorToolbarItemHtml("align_center", "img/alCenter.gif", "tableEditor.setAlignment('center')", "Align center"));
        writer.write(buildTableEditorToolbarItemHtml("align_right", "img/alRight.gif", "tableEditor.setAlignment('right')", "Align right"));
        writer.write("</div>");

        encodeJS(component, writer, "js/initIconManager.js");
    }
    
    private String buildTableEditorToolbarItemHtml(String id, String imgSrc, String action, String title) {
        StringBuilder result = new StringBuilder();
        result.append("<img id='").append(id)
              .append("' src='").append(WebUtil.internalPath(imgSrc))
              .append("' title='").append(title)
              .append("' onclick=\"").append(action)
              .append("\" onmouseover=\"this.className='item_over'\"")
              .append(" onmouseout=\"this.className='item_enabled'\"")
              .append("></img>");
        return result.toString();
    }

}
