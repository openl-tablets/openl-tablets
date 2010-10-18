package org.openl.rules.tableeditor.renderkit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.constraints.Constraint;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.constraints.LessThanConstraint;
import org.openl.rules.table.constraints.MoreThanConstraint;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.tableeditor.model.ui.ActionLink;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.ICellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.util.EnumUtils;
import org.openl.rules.tableeditor.util.WebUtil;
import org.openl.util.StringTool;

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
        Map<String, Object> requestMap = FacesUtils.getRequestMap();
        Set resources = (Set) requestMap.get(Constants.TABLE_EDITOR_RESOURCES);
        if (resources == null) {
            resources = new HashSet();
            requestMap.put(Constants.TABLE_EDITOR_RESOURCES, resources);
        }
        return resources;
    }

    public String render(TableEditor editor) {
        return render(editor, false, null, null, null);
    }

    public String render(TableEditor editor, boolean inner, String cellToEdit,
            List<ActionLink> actionLinks, String errorCell) {
        StringBuilder result = new StringBuilder();
        result.append("<div>").append(renderCSS("css/common.css")).append(renderCSS("css/menu.css")).append(
                renderCSS("css/toolbar.css")).append(renderCSS("css/datepicker.css")).append(
                renderCSS("css/multiselect.css")).append(renderCSS("css/tooltip.css"));
        if (!Constants.THIRD_PARTY_LIBS_PROTOTYPE.equalsIgnoreCase(editor.getExcludeScripts())) {
            result.append(renderJS("js/prototype/prototype-1.6.1.js"));
        }
        result.append(renderJS("js/ScriptLoader.js")).append(renderJS("js/AjaxHelper.js")).append(
                renderJS("js/IconManager.js")).append(renderJS("js/TableEditor.js")).append(
                renderJS("js/initTableEditor.js")).append(renderJS("js/BaseEditor.js")).append(
                renderJS("js/BaseTextEditor.js"));
        if (!inner) {
            result.append("<div id='").append(editor.getId()).append("' class='te_'>");
        }
        String mode = editor.getMode();
        if (mode == null || mode.equals(Constants.MODE_VIEW)) {
            result.append(renderViewer(editor, actionLinks, errorCell));
        } else if (mode.equals(Constants.MODE_EDIT)) {
            result.append(renderEditor(editor, cellToEdit, errorCell));
        }
        if (!inner) {
            result.append("</div>");
        }
        result.append("</div>");
        return result.toString();
    }

    protected String renderActionMenu(String menuId, boolean editable, List<ActionLink> actionLinks) {
        StringBuilder result = new StringBuilder();

        String editLink = "<tr><td><a href=\"javascript:triggerEdit('"
                + menuId.replaceFirst(Constants.ID_POSTFIX_MENU, "") + "','" + WebUtil.internalPath("ajax/edit")
                + "')\">Edit</a></td></tr>";
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

    protected String renderEditor(TableEditor editor, String cellToEdit, String errorCell) {
        StringBuilder result = new StringBuilder();
        cellToEdit = cellToEdit == null ? "" : cellToEdit;
        
        String tableId = editor.getId() + Constants.ID_POSTFIX_TABLE;
        String editorJsVar = Constants.TABLE_EDITOR_PREFIX + editor.getId();
        
        String beforeSave = getEditorJSAction(editor.getOnBeforeSave());
        String afterSave = getEditorJSAction(editor.getOnAfterSave());
        String saveFailure = getEditorJSAction(editor.getOnSaveFailure());
        
        String actions = "{beforeSave:" + beforeSave + ",afterSave:" + afterSave + ",saveFailure:" + saveFailure + "}";

        result.append(renderJSBody("var " + editorJsVar + ";"))
                .append(renderEditorToolbar(editor.getId(), editorJsVar))
                .append(renderJS("js/tooltip.js"))
                .append(renderJS("js/validation.js"))
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
        result.append(
                renderPropsEditor(editor.getId(), editor.getTable(), Constants.MODE_EDIT, editor.isCollapseProps()));
        result.append("<div id=\"").append(tableId).append("\"></div>");
        result.append(renderJSBody(editorJsVar + " = initTableEditor(\"" + editor.getId() + "\", \""
                + WebUtil.internalPath("ajax/") + "\",\"" + cellToEdit + "\"," + actions + ");"));

        return result.toString();
    }

    protected String getEditorJSAction(String action) {
        return StringUtils.isBlank(action) ? "''" : "function() {" + action + "}";
    }

    protected String renderEditorToolbar(String editorId, String editorJsVar) {
        StringBuilder result = new StringBuilder();

        final String toolbarItemSeparator = "<img src=" + WebUtil.internalPath("img/toolbarSeparator.gif")
                + " class=\"item_separator\"></img>";

        result
                .append("<div class=\"te_toolbar\">")
                .append(renderEditorToolbarItem(editorId + "_save_all", editorJsVar, "img/Save.gif", "save()", "Save"))
                .append(renderEditorToolbarItem(editorId + "_undo", editorJsVar, "img/Undo.gif", "undoredo()", "Undo"))
                .append(
                        renderEditorToolbarItem(editorId + "_redo", editorJsVar, "img/Redo.gif", "undoredo(true)",
                                "Redo"))
                .append(toolbarItemSeparator)
                .append(
                        renderEditorToolbarItem(editorId + "_insert_row_before", editorJsVar, "img/insert_row.gif",
                                "doTableOperation(TableEditor.Operations.INSERT_ROW_BEFORE)", "Insert row before"))
                .append(
                        renderEditorToolbarItem(editorId + "_remove_row", editorJsVar, "img/delete_row.gif",
                                "doTableOperation(TableEditor.Operations.REMOVE_ROW)", "Remove row"))
                .append(toolbarItemSeparator)
                .append(
                        renderEditorToolbarItem(editorId + "_insert_column_before", editorJsVar,
                                "img/insert_column.gif",
                                "doTableOperation(TableEditor.Operations.INSERT_COLUMN_BEFORE)", "Insert column before"))
                .append(
                        renderEditorToolbarItem(editorId + "_remove_column", editorJsVar, "img/delete_column.gif",
                                "doTableOperation(TableEditor.Operations.REMOVE_COLUMN)", "Remove column")).append(
                        toolbarItemSeparator).append(
                        renderEditorToolbarItem(editorId + "_align_left", editorJsVar, "img/alLeft.gif",
                                "setAlignment('left')", "Align left")).append(
                        renderEditorToolbarItem(editorId + "_align_center", editorJsVar, "img/alCenter.gif",
                                "setAlignment('center')", "Align center")).append(
                        renderEditorToolbarItem(editorId + "_align_right", editorJsVar, "img/alRight.gif",
                                "setAlignment('right')", "Align right")).append(toolbarItemSeparator).append(
                        renderEditorToolbarItem(editorId + "_decrease_indent", editorJsVar, "img/indent_left.gif",
                                "indent('-1')", "Decrease indent")).append(
                        renderEditorToolbarItem(editorId + "_increase_indent", editorJsVar, "img/indent_right.gif",
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

    protected String renderViewer(TableEditor editor, List<ActionLink> actionLinks, String errorCell) {
        StringBuilder result = new StringBuilder();
        if (editor.getTable() != null) {
            result.append(renderPropsEditor(editor.getId(), editor.getTable(), Constants.MODE_VIEW, editor
                    .isCollapseProps()));
        }
        if (editor.getTable() != null) {
            IGridFilter[] filters = (editor.getFilter() == null) ? null : new IGridFilter[] { editor.getFilter() };
            TableModel tableModel = TableModel.initializeTableModel(editor.getTable().getGridTable(editor.getView()),
                    filters);
            if (tableModel != null) {
                String menuId = editor.getId() + Constants.ID_POSTFIX_MENU;
                TableRenderer tableRenderer = new TableRenderer(tableModel);
                tableRenderer.setCellIdPrefix(editor.getId() + Constants.ID_POSTFIX_CELL);
                if (editor.isEditable() || (actionLinks != null && !actionLinks.isEmpty())) {
                    result.append(renderJS("js/popup/popupmenu.js"))
                        .append(renderJS("js/tableEditorMenu.js"))
                        .append(tableRenderer.renderWithMenu(editor, menuId, errorCell))
                        .append(renderActionMenu(menuId, editor.isEditable(), actionLinks));
                } else {
                    result.append(tableRenderer.render(editor.isShowFormulas()));
                }
            }
        }
        return result.toString();
    }

    protected String renderPropsEditor(String editorId, IOpenLTable table, String mode, boolean collapseProps) {
        final String tableType = table.getType();
        if (tableType != null && !tableType.equals(ITableNodeTypes.XLS_OTHER)
                && !tableType.equals(ITableNodeTypes.XLS_ENVIRONMENT)
                && !tableType.equals(ITableNodeTypes.XLS_PROPERTIES)) {
            ITableProperties props = table.getProperties();
            return new PropertyRenderer(editorId + Constants.ID_POSTFIX_PROPS, props, mode, collapseProps,
                    tableType).renderProperties();
        }
        return "";
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

        public String render(boolean showFormulas) {
            return render(null, false, showFormulas, null);
        }

        public String render(String extraTDText, boolean embedCellURI, boolean showFormulas, String errorCell) {
            String tdPrefix = "<td";
            if (extraTDText != null) {
                tdPrefix += " ";
                tdPrefix += extraTDText;
            }
            final String prefix = cellIdPrefix != null ? cellIdPrefix : Constants.ID_POSTFIX_CELL;

            IGridTable table = tableModel.getGridTable();

            StringBuilder s = new StringBuilder();
            s.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");

            for (int i = 0; i < tableModel.getCells().length; i++) {
                s.append("<tr>\n");
                for (int j = 0; j < tableModel.getCells()[i].length; j++) {

                    ICellModel cell = tableModel.getCells()[i][j];
                    if ((cell == null) || !cell.isReal()) {
                        continue;
                    }

                    String cellUri = null;
                    if (table != null) {
                        cellUri = table.getUri(j, i);
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

                    StringBuilder id = new StringBuilder();
                    id.append(prefix).append(String.valueOf(i + 1)).append(":").append(j + 1);

                    s.append(" id=\"").append(id).append("\">");
                    if (embedCellURI) {
                        s.append("<input name=\"uri\" type=\"hidden\" value=\"").append(cellUri).append("\"></input>");
                    }
                    if (cell.hasFormula()) {
                        s.append("<input name=\"formula\" type=\"hidden\" value=\"").append(cell.getFormula()).append(
                                "\"></input>");
                    }
                    String cellContent = cell.getContent(showFormulas);
                    if (cellContent != null) {
                        cellContent.replaceAll("", "");
                    }
                    s.append(cellContent).append("</td>\n");
                }
                s.append("</tr>\n");
            }
            s.append("</table>");
            return s.toString();
        }

        public String renderWithMenu(TableEditor editor, String menuId, String errorCell) {
            menuId = menuId == null ? "" : menuId;
            String eventHandlers = "onmousedown=\"openMenu('" + menuId + "',this,event)\"";
            if (editor.isEditable()) {
                eventHandlers += " ondblclick=\"triggerEdit('"
                    + menuId.replaceFirst(Constants.ID_POSTFIX_MENU, "") + "','"
                    + WebUtil.internalPath("ajax/edit") + "', this)\"";
            }
            return render(eventHandlers, true, editor.isShowFormulas(), errorCell);
        }

        public void setCellIdPrefix(String prefix) {
            cellIdPrefix = prefix;
        }
    }

    /**
     * Render properties
     * 
     * @author DLiauchuk
     * 
     */
    public class PropertyRenderer {

        private List<TableProperty> listProperties = new ArrayList<TableProperty>();

        private StringBuilder result;

        private String mode;

        private ITableProperties props;

        private String propsId;

        private boolean collapsed;
        
        private String tableType;

        public PropertyRenderer(String propsId, ITableProperties props, String view, boolean collapsed,
                String tableType) {
            this.propsId = propsId == null ? "" : propsId;
            this.props = props;
            this.mode = view;
            this.tableType = tableType;
            this.listProperties = initPropertiesList();
            this.result = new StringBuilder();
            this.collapsed = collapsed;
            
        }

        private List<TableProperty> initPropertiesList() {
            List<TableProperty> listProp = new ArrayList<TableProperty>();
            TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions.getDefaultDefinitions();
            for (TablePropertyDefinition propDefinition : propDefinitions) {
                String name = propDefinition.getName();
                String displayName = propDefinition.getDisplayName();
                Object value = props != null ? props.getPropertyValue(propDefinition.getName()) : null;
                Class<?> type = propDefinition.getType() == null ? String.class : propDefinition.getType()
                        .getInstanceClass();
                InheritanceLevel inheritanceLevel = props.getPropertyLevelDefinedOn(name);
                String group = propDefinition.getGroup();
                String format = propDefinition.getFormat();
                Constraints constraints = propDefinition.getConstraints();
                String description = propDefinition.getDescription();
                boolean system = propDefinition.isSystem();
                if (PropertiesChecker.isPropertySuitableForTableType(name, tableType)) {
                    TableProperty prop = new TableProperty.TablePropertyBuilder(name, type).value(value)
                    .displayName(displayName).group(group).format(format).constraints(constraints)
                    .description(description).system(system).inheritanceLevel(inheritanceLevel).build();
                    listProp.add(prop);
                }
                
            }
            return listProp;
        }

        private TableProperty getProperty(String name) {
            for (TableProperty property : listProperties) {
                if (property.getName().equals(name)) {
                    return property;
                }
            }
            return null;
        }

        private Map<String, List<TableProperty>> groupProps(List<TableProperty> props) {
            Map<String, List<TableProperty>> groupProps = new LinkedHashMap<String, List<TableProperty>>();
            for (TableProperty prop : props) {
                String group = prop.getGroup();
                List<TableProperty> groupList = groupProps.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<TableProperty>();
                    groupProps.put(group, groupList);
                }
                if (!groupList.contains(prop)) {
                    groupList.add(prop);
                }
            }
            return groupProps;
        }

        /**
         * 
         * @return Construct table for properties
         */
        public String renderProperties() {
            result.append(renderCSS("css/properties.css"));
            result.append("<table cellspacing='0' cellpadding='0' class='te_props'>");
            result.append("<tr><td class='te_props_header'>Properties");
            renderHideButton(propsId);
            result.append("</td></tr><tr><td><div id=" + propsId
                    + " class='te_props_propstable' style='display: none;'>");
            result.append("<table cellspacing='1' cellpadding='1'>");
            buildPropsTable();
            result.append("</table></div></td></tr></table>");
            if (!collapsed) {
                result.append(renderJSBody("$('" + propsId + "').show()"));
            }
            return result.toString();
        }

        private void buildPropsTable() {
            Map<String, List<TableProperty>> groupProps = groupProps(listProperties);
            int groupSize = groupProps.size();
            int numToDivide = getNumToDivideColumns(groupSize);

            int i = 0;
            Set<String> groupKeys = groupProps.keySet();
            for (String group : groupKeys) {
                if (i == 0) {
                    result.append("<tr>");
                }
                if (i == numToDivide) {
                    result.append("</tr>");
                    result.append("<tr>");
                }
                result.append("<td valign='top' class='te_props_group'>");
                fillPropsGroup(group, mode);
                result.append("</td>");

                if (i == groupSize - 1) {
                    result.append("</tr>");
                }
                i++;
            }
        }

        private int getNumToDivideColumns(int groupSize) {
            int numToDivide = 0;
            if (groupSize % 2 == 0) {
                numToDivide = groupSize / 2;
            } else {
                numToDivide = (groupSize + 1) / 2;
            }
            return numToDivide;
        }

        private void fillPropsGroup(String groupKey, String mode) {
            Map<String, List<TableProperty>> groupProps = groupProps(listProperties);
            List<TableProperty> groupList = groupProps.get(groupKey);
            String groupId = propsId + Constants.ID_POSTFIX_PROPS_GROUP + groupKey;
            result.append("<div class='te_props_groupheader'>");
            result.append(groupKey);
            renderHideButton(groupId);
            result.append("</div>");
            result.append("<div id='" + groupId + "' class='te_props_grouptable'>");
            result.append("<table>");
            for (TableProperty prop : groupList) {
                insertProp(prop);
            }
            result.append("</table></div>");
        }

        private void insertProp(TableProperty prop) {
            String inheritPropStyleClass = "";
            if (prop.isModuleLevelProperty() || prop.isCategoryLevelProperty()) {
                inheritPropStyleClass = " te_props_prop_inherited";
            }
            result.append("<tr class='te_props_prop" + inheritPropStyleClass + "'>");
            insertPropLabel(prop.getDisplayName());
            insertPropValue(prop, mode);
            insertPropertiesTableLink(prop, mode);
            result.append("</tr>");
        }

        private void insertPropValue(TableProperty prop, String mode) {
            if ("edit".equals(mode)) {
                final String propId = getPropId(prop);
                if (prop.isSystem() || !prop.canBeOverridenInTable() || "version".equalsIgnoreCase(prop.getName())) {
                    insertText(prop, propId, true);
                } else {
                    insertInput(prop, propId, true);
                }
            } else {
                insertText(prop);
            }
        }

        private void insertPropertiesTableLink(TableProperty prop, String mode) {
            result.append("<td>");
            String propsTableUri = getProprtiesTablePageUrl(prop, mode);
            if (propsTableUri != null) {
                String imgUp = WebUtil.internalPath("img/up.gif");
                result.append("<a href='" + propsTableUri + "' title=''><img src='" + imgUp
                        + "' title='Go to Properties table' alt='Go to Properties table' /></a>");
            }
            result.append("</td>");
        }

        private String getProprtiesTablePageUrl(TableProperty prop, String mode) {
            String url = null;
            ILogicalTable propertiesTable = null;
            if (prop.isModuleLevelProperty()) {
                propertiesTable = props.getModulePropertiesTable();
            } else if (prop.isCategoryLevelProperty()) {
                propertiesTable = props.getCategoryPropertiesTable();
            }
            if (propertiesTable != null) {
                String tableUri = propertiesTable.getSource().getUri();
                url = "?uri=" + StringTool.encodeURL(tableUri);
                if ("edit".equals(mode)) {
                    url += "&mode=edit";
                }
            }
            return url;
        }

        private void renderHideButton(String idToHide) {
            String imgCollapseSrc = WebUtil.internalPath("img/arrow_right.gif");
            String imgExpandSrc = WebUtil.internalPath("img/arrow_down.gif");
            result.append(" <img src='" + imgCollapseSrc + "' onclick=\"$('" + idToHide
                    + "').toggle();this.src=(this.title == 'Hide' ? '" + imgExpandSrc + "' : '" + imgCollapseSrc
                    + "');" + "this.title=(this.title == 'Hide' ? 'Show' : 'Hide');\""
                    + " title='Hide' class='te_props_hidebutton' />");
        }

        private String getPropId(TableProperty prop) {
            return propsId + Constants.ID_POSTFIX_PROP + prop.getName();
        }

        private void insertInput(TableProperty prop, String id, boolean showTooltip) {
            
            boolean inserted = true;
            
            String propValue = prop.getStringValue();

            if (prop.isString() || prop.isDouble()) {
                insertTextbox(prop, id); 
            } else if (prop.isStringArray()) {
                insertTextbox(prop, id);
            } else if (prop.isDate()) {
                insertCalendar(prop, id);
            } else if (prop.isBoolean()) {
                insertCheckbox(propValue, id);
            } else if (prop.isEnum()) {
                insertSingleSelectForEnum(prop, id);
            } else if (prop.isEnumArray()) {
                insertMultiSelectForEnum(prop, id);
            } else {
                inserted = false;
            }
            if (showTooltip && inserted) {
                insertTooltip(id, prop.getDescription());
            }
        }

        private void insertTooltip(String propId, String description) {
            result.append(renderJSBody("new Tooltip('_" + propId + "','" + description + "',{skin:'green'})"));
        }

        private void insertCalendar(TableProperty prop, String id) {
            String value = prop.getStringValue();
            Constraints constraints = prop.getConstraints();
            result.append("<td id='" + id + "' class='te_props_proptextinput'></td>").append(
                    renderJSBody("new DateEditor('','" + id + "','','" + StringEscapeUtils.escapeJavaScript(value)
                            + "','')"));

            for (Constraint constraint : constraints.getAll()) {
                if (constraint instanceof LessThanConstraint || constraint instanceof MoreThanConstraint) {
                    String validator = constraint instanceof LessThanConstraint ? "lessThan" : "moreThan";
                    String compareToField = (String) constraint.getParams()[0];
                    String compareToFieldId = "_" + id.replaceFirst(prop.getName() + "(?=$)", compareToField);
                    TableProperty compareToProperty = getProperty(compareToField);
                    String compareToPropertyDisplayName = compareToProperty == null ? "" : compareToProperty
                            .getDisplayName();
                    result.append(renderJSBody("new Validation('_" + id + "', '" + validator
                            + "', 'blur', {compareToFieldId:'" + compareToFieldId + "',messageParams:'"
                            + compareToPropertyDisplayName + "'})"));
                }
            }
        }

        private void insertSingleSelect(String componentId, String[] values, String[] displayValues, String value) {

            String jsCode = getSingleSelectComponentCode(componentId, values, displayValues, value);
            result.append("<td id='" + componentId + "' class='te_props_proptextinput'></td>").append(
                    renderJSBody(jsCode));
        }

        private void insertSingleSelectForEnum(TableProperty prop, String id) {
            Class<?> instanceClass = prop.getType();
            String value = prop.getStringValue();

            String[] values = EnumUtils.getNames(instanceClass);
            String[] displayValues = EnumUtils.getValues(instanceClass);

            insertSingleSelect(id, values, displayValues, value);
        }

        private void insertMultiSelectForEnum(TableProperty prop, String id) {
            Class<?> instanceClass = prop.getType().getComponentType();

            String valueString = prop.getStringValue();

            String[] values = EnumUtils.getNames(instanceClass);
            String[] displayValues = EnumUtils.getValues(instanceClass);

            insertMultiSelect(id, values, displayValues, valueString);
        }

        private void insertMultiSelect(String componentId, String[] values, String[] displayValues, String value) {

            String jsCode = getMultiSelectComponentCode(componentId, values, displayValues, value);

            result.append("<td id='" + componentId + "' class='te_props_proptextinput'></td>").append(
                    renderJSBody(jsCode));
        }

        private void insertTextbox(TableProperty prop, String id) {
            String value = prop.getStringValue();
            insertTextbox(value, id);
        }

        private void insertTextbox(String value, String id) {

            result.append("<td id='" + id + "' class='te_props_proptextinput'></td>").append(
                    renderJSBody("new TextEditor('','" + id + "','','" + StringEscapeUtils.escapeJavaScript(value)
                            + "','')"));
        }

        private void insertCheckbox(String value, String id) {
            Boolean bValue = new Boolean(value);
            if (value == null) {
                bValue = false;
            }
            result.append("<td id='" + id + "'></td>").append(
                    renderJSBody("new BooleanEditor('','" + id + "','','" + bValue + "','')"));
        }

        private void insertPropLabel(String displayName) {
            result.append("<td class='te_props_proplabel'>" + displayName + ":</td>");
        }

        private void insertText(TableProperty prop) {
            insertText(prop, null, false);
        }

        private void insertText(TableProperty prop, String id, boolean showTooltip) {
            String propValue = prop.getDisplayValue();
            result.append("<td class='te_props_propvalue'><span"
                    + (StringUtils.isNotBlank(id) ? (" id='_" + id + "'") : "") + ">" + propValue + "</span></td>");
            if (showTooltip) {
                insertTooltip(id, prop.getDescription());
            }
        }
    }
}
