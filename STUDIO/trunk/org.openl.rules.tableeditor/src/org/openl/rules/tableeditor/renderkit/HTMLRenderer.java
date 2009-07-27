package org.openl.rules.tableeditor.renderkit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.binding.TableProperties.Property;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ITable;
import org.openl.rules.table.ui.IGridFilter;
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

        public String render() {
            return render(null, false);
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
                    id.append(prefix).append(String.valueOf(i + 1)).append(":").append(j + 1);

                    s.append(" id=\"").append(id).append("\">");
                    if (embedCellURI) {
                        s.append("<input name=\"uri\" type=\"hidden\" value=\"").append(table.getUri(j, i)).append(
                                "\"></input>");
                    }
                    String content = cell.getContent();
                    if (content != null) {
                        content.replaceAll("", "");
                    }
                    s.append(cell.getContent()).append("</td>\n");
                }
                s.append("</tr>\n");
            }
            s.append("</table>");
            return s.toString();
        }

        public String renderWithMenu(String menuId) {
            menuId = menuId == null ? "" : menuId;
            return render("onmouseover=\"openMenu('" + menuId + "',this,event)\" onmouseout=\"closeMenu(this)\"", true);
        }

        public void setCellIdPrefix(String prefix) {
            cellIdPrefix = prefix;
        }
    }

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

    public String render(ITable table, String view, String editorId, IGridFilter filter) {
        return render(null, table, view, null, false, null, false, editorId, filter);
    }

    public String render(String mode, ITable table, String view, List<ActionLink> actionLinks, boolean editable,
            String cellToEdit, boolean inner, String editorId, IGridFilter filter) {
        StringBuilder result = new StringBuilder();
        result.append("<div>").append(renderCSS("css/common.css")).append(renderCSS("css/menu.css")).append(
                renderCSS("css/toolbar.css")).append(renderJS("js/prototype/prototype-1.5.1.js")).append(
                renderJS("js/ScriptLoader.js")).append(renderJS("js/AjaxHelper.js")).append(
                renderJS("js/IconManager.js")).append(renderJS("js/TableEditor.js")).append(
                renderJS("js/initTableEditor.js")).append(renderJS("js/BaseEditor.js")).append(
                renderJS("js/BaseTextEditor.js"));
        if (!inner) {
            result.append("<div id='").append(editorId).append("' class='te_'>");
        }
        if (mode == null || mode.equals(Constants.MODE_VIEW)) {
            result.append(renderViewer(table, view, actionLinks, editable, editorId, filter));
        } else if (mode.equals(Constants.MODE_EDIT)) {
            result.append(renderEditor(editorId, cellToEdit, table));
        }
        if (!inner) {
            result.append("</div>");
        }
        result.append("</div>");
        return result.toString();
    }

    protected String renderActionMenu(String menuId, boolean editable, List<ActionLink> actionLinks) {
        StringBuilder result = new StringBuilder();

        String editLinks = "<tr><td><a href=\"javascript:triggerEdit('"
                + menuId.replaceFirst(Constants.MENU_ID_POSTFIX, "") + "','" + WebUtil.internalPath("ajax/edit")
                + "')\">Edit</a></td></tr>" + "<tr><td><a href=\"javascript:triggerEditXls('"
                + WebUtil.internalPath("ajax/editXls") + "')\">Edit in Excel</a></td></tr>";
        String menuBegin = "<div id=\"" + menuId + "\" style=\"display:none;\">" + "<table cellpadding=\"1px\">"
                + (editable ? editLinks : "");
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

    protected String renderEditor(String editorId, String cellToEdit, ITable table) {
        StringBuilder result = new StringBuilder();
        cellToEdit = cellToEdit == null ? "" : cellToEdit;
        final String tableId = editorId + Constants.TABLE_ID_POSTFIX;
        String editor = Constants.TABLE_EDITOR_PREFIX + editorId;
        result.append(renderJSBody("var " + editor + ";"))
        // .append(renderJSBody("var jsPath = \"" + WebUtil.internalPath("js/")
        // + "\""))
                .append(renderEditorToolbar(editorId, editor)).append(renderJS("js/TextEditor.js")).append(
                        renderJS("js/MultiLineEditor.js")).append(renderJS("js/NumericEditor.js")).append(
                        renderJS("js/DropdownEditor.js"))
                .append(renderPropsEditor(table))
                // .append(renderJS("js/SuggestEditor.js"))
                // .append(renderJS("js/DateEditor.js"))
                // .append(renderJS("js/PriceEditor.js"))
                // .append(renderJS("js/MultipleChoiceEditor.js"))
                .append("<div id=\"").append(tableId).append("\"></div>").append(
                        renderJSBody(editor + " = initTableEditor(\"" + editorId + "\", \""
                                + WebUtil.internalPath("ajax/") + "\",\"" + cellToEdit + "\");"));
        return result.toString();
    }

    protected String renderEditorToolbar(String editorId, String editor) {
        StringBuilder result = new StringBuilder();

        final String toolbarItemSeparator = "<img src=" + WebUtil.internalPath("img/toolbarSeparator.gif")
                + " class=\"item_separator\"></img>";

        result.append("<div class=\"te_toolbar\">").append(
                renderEditorToolbarItem(editorId + "_save_all", editor, "img/Save.gif", "save()", "Save")).append(
                renderEditorToolbarItem(editorId + "_undo", editor, "img/Undo.gif", "undoredo()", "Undo")).append(
                renderEditorToolbarItem(editorId + "_redo", editor, "img/Redo.gif", "undoredo(true)", "Redo")).append(
                toolbarItemSeparator).append(
                renderEditorToolbarItem(editorId + "_add_row_before", editor, "img/insert_row.gif",
                        "doRowOperation(TableEditor.Constants.ADD_BEFORE)", "Add row")).append(
                renderEditorToolbarItem(editorId + "_remove_row", editor, "img/delete_row.gif",
                        "doRowOperation(TableEditor.Constants.REMOVE)", "Remove row"))
        // .append(renderEditorToolbarItem(editorId + "_move_row_down", editor,
        // "img/b_row_ins.gif",
                // "doRowOperation(TableEditor.Constants.MOVE_DOWN)", "Move row
                // down"))
                // .append(renderEditorToolbarItem(editorId + "_move_row_up",
                // editor, "img/b_row_ins.gif",
                // "doRowOperation(TableEditor.Constants.MOVE_UP)", "Move row
                // up"))
                .append(toolbarItemSeparator).append(
                        renderEditorToolbarItem(editorId + "_add_column_before", editor, "img/insert_column.gif",
                                "doColOperation(TableEditor.Constants.ADD_BEFORE)", "Add column")).append(
                        renderEditorToolbarItem(editorId + "_remove_column", editor, "img/delete_column.gif",
                                "doColOperation(TableEditor.Constants.REMOVE)", "Remove column"))
                // .append(renderEditorToolbarItem(editorId +
                // "_move_column_right", editor, "img/b_row_ins.gif",
                // "doColOperation(TableEditor.Constants.MOVE_DOWN)", "Move
                // column right"))
                // .append(renderEditorToolbarItem(editorId +
                // "_move_column_left", editor, "img/b_row_ins.gif",
                // "doColOperation(TableEditor.Constants.MOVE_UP)", "Move column
                // left"))
                .append(toolbarItemSeparator).append(
                        renderEditorToolbarItem(editorId + "_align_left", editor, "img/alLeft.gif",
                                "setAlignment('left')", "Align left")).append(
                        renderEditorToolbarItem(editorId + "_align_center", editor, "img/alCenter.gif",
                                "setAlignment('center')", "Align center")).append(
                        renderEditorToolbarItem(editorId + "_align_right", editor, "img/alRight.gif",
                                "setAlignment('right')", "Align right")).append(toolbarItemSeparator).append(
                        renderEditorToolbarItem(editorId + "_decrease_indent", editor, "img/indent_left.gif",
                                "indent('-1')", "Decrease indent")).append(
                        renderEditorToolbarItem(editorId + "_increase_indent", editor, "img/indent_right.gif",
                                "indent('1')", "Increase indent")).append(toolbarItemSeparator).append(
                        renderEditorToolbarItem(editorId + "_help", null, "img/help.gif", "window.open('"
                                + WebUtil.internalPath("docs/help.html") + "');", "Help")).append("</div>");

        return result.toString();
    }

    protected String renderEditorToolbarItem(String itemId, String editor, String imgSrc, String action, String title) {
        editor = (editor == null || editor.equals("")) ? "" : editor + ".";
        StringBuilder result = new StringBuilder();
        result.append("<img id=\"").append(itemId).append("\" src=\"").append(WebUtil.internalPath(imgSrc)).append(
                "\" title=\"").append(title).append("\" onclick=\"").append(editor).append(action).append(
                "\" onmouseover=\"this.className='item_over'\"")
                .append(" onmouseout=\"this.className='item_enabled'\"").append("></img>");
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

    protected String renderViewer(ITable table, String view, List<ActionLink> actionLinks, boolean editable,
            String editorId, IGridFilter filter) {
        StringBuilder result = new StringBuilder();
        if (table != null) {
            result.append(renderPropsViever(table));
        }
        if (table != null) {
            IGridFilter[] filters = (filter == null) ? null : new IGridFilter[] { filter };
            TableModel tableModel = TableModel.initializeTableModel(new TableEditorModel(
                    table.getGridTable(view)).getUpdatedTable(), filters);
            if (tableModel != null) {
                String menuId = editorId + Constants.MENU_ID_POSTFIX;
                TableRenderer tableRenderer = new TableRenderer(tableModel);
                tableRenderer.setCellIdPrefix(editorId + Constants.CELL_ID_POSTFIX);
                if (editable || (actionLinks != null && !actionLinks.isEmpty())) {
                    result.append(renderJS("js/popup/popupmenu.js")).append(renderJS("js/tableEditorMenu.js")).append(
                            tableRenderer.renderWithMenu(menuId)).append(
                            renderActionMenu(menuId, editable, actionLinks));
                } else {
                    result.append(tableRenderer.render());
                }
            }
        }
        return result.toString();
    }

    protected String renderPropsViever(ITable table) {
        TableProperties props = table.getProperties();
//        StringBuilder result = new StringBuilder();
//        result.append("<fieldset style='width:300px;margin-bottom:5px;'><legend>Properties</legend>"
//                + "<table style='font-size: 12px;font-family: Arial' cellspacing='1' cellpadding='1'>");
//        if(props!=null) {
//            for (Property property : props.getProperties()) {
//                result.append("<tr><td><b>").append(property.getKey()).append(":</b></td><td>")
//                      .append(property.getValue()).append("</td></tr>");
//            }
//            result.append("</table></fieldset>");
//        
            /*result.append("<fieldset style='width:515px;margin-bottom:5px;'><legend>Properties</legend>"
                    + "<table style='font-size: 12px;font-family: Arial' cellspacing='1' cellpadding='1'><tr><td><table><tr><td><b>Name:</b></td><td>Table1</td></tr><tr><td><b>Template:</b></td>"
                    + "<td>Standard Rule</td></tr><tr><td><b>Effective Date:</b></td><td>12/12/2009</td></tr>"
                    + "<tr><td><b>Expiration Date:</b></td><td>12/12/2009</td></tr></table></td><td><table>"
                    + "<tr><td><b>Folder:</b></td><td>Folder1</td></tr><tr><td><b>Status:</td><td>Active</b></td></tr>"
                    + "<tr><td><b>Created On:</b></td><td>11/11/2009</td></tr><tr><td><b>Created By:</b></td>"
                    + "<td>Admin - Petr Udalov</td></tr></table></td></tr></table></fieldset>");*/
            
//        }        
//        return result.toString();
        
        PropertyRenderer propEditRend = new PropertyRenderer(props,"view");
        return propEditRend.renderProperties();
    }

    protected String renderPropsEditor(ITable table) {
        TableProperties props = table.getProperties();        
//       StringBuilder result = new StringBuilder();      
//        result.append("<fieldset style='width:300px;margin-bottom:5px;'><legend>Properties</legend>"
//                + "<table style='font-size: 12px;font-family: Arial' cellspacing='1' cellpadding='1'>");
//        for (Property property : props.getProperties()) {
//            result.append("<tr><td><b>").append(property.getKey()).append(":</b></td><td>")
//                  .append("<input id='_" + property.getKey() + "' type='text' value='" + property.getValue() + "' />")
//                  .append("</td></tr>");
//        }
//        result.append("</table></fieldset>");
        
//        result.append(renderJS("js/calendar_us.js"));
//        result.append(renderCSS("css/calendar.css"));
//        result.append("<fieldset style='width:580px;margin-bottom:5px;'><legend>Properties</legend>"
//                + "<table style='font-size: 12px;font-family: Arial' cellspacing='1' cellpadding='1'><tr><td><table><tr><td><b>Name:</b></td><td><input type='text' value='Table1' /></td></tr><tr><td><b>Template:</b></td>"
//                + "<td><select><option>Standard Rule</option><option>Standard Rule 2</option><option>Standard Rule 3</option></select></td></tr><tr><td><b>Effective Date:</b></td><td><input type='text' value='12/12/2009' id='datepicker1' />")
//                .append(renderJSBody("new tcal ({'controlname': 'datepicker1'});"))
//                .append("</td></tr>"
//                + "<tr><td><b>Expiration Date:</b></td><td><input type='text' value='11/11/2009' id='datepicker2' />")
//                .append(renderJSBody("new tcal ({'controlname': 'datepicker2'});"))
//                .append("</td></tr></table></td><td><table>"
//                + "<tr><td><b>Folder:</b></td><td>Folder1</td></tr><tr><td><b>Status:</td><td><select><option>Active</option><option>Inactive</option></select></b></td></tr>"
//                + "<tr><td><b>Created On:</b></td><td>11/11/2009</td></tr><tr><td><b>Created By:</b></td>"
//                + "<td>Admin - Petr Udalov</td></tr></table></td></tr></table></fieldset>");
//        result.append(renderJSBody("$(function(){$('[name=datepicker]').datepicker({});});"));
//        return result.toString();
        PropertyRenderer propEditRend = new PropertyRenderer(props,"edit");
        return propEditRend.renderProperties();
    }   
    
    /**
     * Temporary class to render properties on edit view
     * @author DLiauchuk
     *
     */
    public class PropertyRenderer {
                
        private List<TableProperty> listProperties = new ArrayList<TableProperty>();
        
        private int numberOfCalendars = 0;
        
        private StringBuilder result;
        
        private String mode;
        
        private TableProperties props;
        
        public PropertyRenderer(TableProperties props,String view) { 
            this.props = props;
            this.mode = view;
            this.listProperties = initPropertiesList();
            this.result = new StringBuilder();
        }
        
        private List<TableProperty> initPropertiesList() {
            boolean nameSet = false;
            boolean folderSet = false;
            List<TableProperty> listProp = new ArrayList<TableProperty>();
            if(props!=null){
                for(Property prop: props.getProperties()) {
                    if("name".equals(prop.getKey().getValue())) {
                        listProp.add(new TableProperty("Name", prop.getValue().getValue(),String.class));
                        nameSet = true;
                    }
                    if("category".equals(prop.getKey().getValue())) {
                        listProp.add(new TableProperty("Folder", prop.getValue().getValue(),String.class));
                        folderSet = true;
                    }
                }
            }
            if(!nameSet) {
                listProp.add(new TableProperty("Name", null, String.class));
            }
            if(!folderSet) {
                listProp.add(new TableProperty("Folder", null, String.class));
            }
            listProp.add(new TableProperty("Template", Arrays.asList("Standard_Rule1", "Standard_Rule2", "Standard_Rule3"),List.class));
            listProp.add(new TableProperty("Effective Date", null, Date.class));
            listProp.add(new TableProperty("Expiration Date", null, Date.class));            
            listProp.add(new TableProperty("Status", Arrays.asList("Active", "Inactive"),List.class));
            listProp.add(new TableProperty("Created On", null, Date.class));
            listProp.add(new TableProperty("Created By", null, String.class));
            return listProp;
        }  
        
        /**
         * 
         * @return Construct table for properties
         */
        public String renderProperties() {              
            result.append("<fieldset style='width:500px;margin-bottom:5px;'><legend>Properties</legend>"
                    + "<table style='font-size: 12px;font-family: Arial' cellspacing='1' cellpadding='1'>");
            result.append(renderJS("js/calendar_us.js"));
            result.append(renderCSS("css/calendar.css"));                        
            
            divideForTwoColumns();
            
            result.append("</table></fieldset>");
            result.append(renderJSBody("$(function(){$('[name=datepicker]').datepicker({});});"));
            return result.toString();            
        }
        
        /**
         * Divide number of properties for two columns
         */
        private void divideForTwoColumns() {
            int numToDivide = 0;
            if(listProperties.size()%2==0) {
                numToDivide = listProperties.size()/2;
            } else {
                numToDivide = (listProperties.size()+1)/2;                
            }
            buildTableProp(numToDivide);            
        }
        
        private void buildTableProp(int numToDivide) {            
            //filling left column of table            
            result.append("<td><table>");
            for(int i=0;i<numToDivide;i++) {
                if("edit".equals(mode)) {
                    fillEditRow(i);
                } else {
                    fillViewRow(i);
                }
            }
            result.append("</table></td>");
            
            //filling right column of table
            result.append("<td><table>");
            for(int i=numToDivide;i<listProperties.size();i++) {
                if("edit".equals(mode)) {
                    fillEditRow(i);
                } else {
                    fillViewRow(i);
                }
            }
            result.append("</table></div></td>");
            
        }
        
                
        /**
         * Fills the row in edit table, depending on the type of property value
         * @param index
         */
        private void fillEditRow(int index) {
            result.append("<tr>");
            insertLabel(listProperties.get(index).getDisplayName());
            if(String.class.equals(listProperties.get(index).getType())) {
                insertEdit((String)listProperties.get(index).getValue());                    
            } else {
                if(Date.class.equals(listProperties.get(index).getType())) {
                    insertCalendar((Date)listProperties.get(index).getValue());
                } else {
                    if(List.class.equals(listProperties.get(index).getType())) {
                        insertSelect((List<String>)listProperties.get(index).getValue());
                    }
                }
            }
            result.append("</tr>");
        }
        
        /**
         * Fills the row in view table
         * @param index
         */
        private void fillViewRow(int index) {
            result.append("<tr>");
            insertLabel(listProperties.get(index).getDisplayName());
            insertText(listProperties.get(index)); 
        }
        
        private void insertCalendar(Date value) {
            numberOfCalendars++;            
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy"); 
            String resultStr;
            if(value==null) {
                resultStr = "";
            } else {
                resultStr = sdf.format(value); 
            }
             
            result.append("<td><input type='text' value='"+resultStr+"' id='datepicker"+numberOfCalendars+"' />").append(renderJSBody("new tcal ({'controlname': 'datepicker"+numberOfCalendars+"'});")).append("</td>");            
        }

        private void insertSelect(List<String> listOfOptions) {                        
            result.append("<td><select>");
            result.append("<option></option>");
            for(String option : listOfOptions) {
                result.append("<option>"+option+"</option>");
            }
            result.append("</select></td>");
        }

        private void insertEdit(String value) {
            if(value==null) {
                value="";
            }
            result.append("<td><input type='text' value='" + value + "' /></td>");            
        }
        
        private void insertLabel(String displayName) {
            result.append("<td><b>"+displayName+":</b></td>");
        }
        
        /**
         * 
         * @param value
         */
        private void insertText(TableProperty prop) {
            if(String.class.equals(prop.getType())) {
                String resString;
                if(prop.getValue()==null){
                    resString = "";
                } else {
                    resString = (String)prop.getValue();
                }
                result.append("<td>"+resString+"</td>");                    
            } else {
                if(Date.class.equals(prop.getType())) {
                    numberOfCalendars++;            
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
                    String resString;
                    if(prop.getValue()==null) {
                        resString = "";
                    } else {
                        resString = sdf.format((Date)prop.getValue());
                    }
                    result.append("<td>"+resString+"</td>");
                } else {
                    if(List.class.equals(prop.getType())) {
                        //result.append("<td><b>"+((List<String>)prop.getValue()).get(0)+"</b></td>");
                    }
                }
            }
        }
    }
}
