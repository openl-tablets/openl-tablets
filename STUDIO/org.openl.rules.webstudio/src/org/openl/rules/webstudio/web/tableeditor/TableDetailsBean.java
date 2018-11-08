package org.openl.rules.webstudio.web.tableeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

@ManagedBean
@ViewScoped
public class TableDetailsBean {
    private boolean editable;
    private List<PropertyRow> propertyRows;
    private Map<String, List<TableProperty>> groups;
    private Set<String> propsToRemove = new HashSet<String>();

    private String newTableId;
    private String propertyToAdd;
    private String id;

    public TableDetailsBean() {
        WebStudio studio = WebStudioUtils.getWebStudio();

        id = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        IOpenLTable table;

        String uri;
        if (studio.getModel().getTableById(id) == null) {
            uri = studio.getTableUri();
            table = studio.getModel().getTable(uri);
        } else {
            table = getTable();
            uri = table.getUri();
        }

        //table = getTable();
        //uri = table.getId();

        if (table != null && table.isCanContainProperties()) {
            editable = WebStudioUtils.getProjectModel().isCanEditTable(uri) && !table.getName().startsWith(
                    DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME);
            initPropertyGroups(table, table.getProperties());
        }
    }

    public void initPropertyGroups(IOpenLTable table, ITableProperties props) {
        groups = new LinkedHashMap<String, List<TableProperty>>();
        TablePropertyDefinition[] propDefinitions = TablePropertyDefinitionUtils
                .getDefaultDefinitionsForTable(table.getType());

        for (TablePropertyDefinition propDefinition : propDefinitions) {
            Object value = props.getPropertyValue(propDefinition.getName());
            if (value != null) {
                InheritanceLevel inheritanceLevel = props.getPropertyLevelDefinedOn(propDefinition.getName());

                TableProperty prop = new TableProperty(propDefinition);
                prop.setValue(value);
                prop.setInheritanceLevel(inheritanceLevel);
                if (InheritanceLevel.MODULE.equals(inheritanceLevel)
                        || InheritanceLevel.CATEGORY.equals(inheritanceLevel)) {
                    prop.setInheritedTableId(getProprtiesTableId(inheritanceLevel, props));
                }

                storeProperty(prop);
            }
        }
    }

    private void storeProperty(TableProperty prop) {
        String group = prop.getGroup();
        List<TableProperty> groupList = groups.get(group);
        if (groupList == null) {
            groupList = new ArrayList<TableProperty>();
            groups.put(group, groupList);
        }
        if (!groupList.contains(prop)) {
            groupList.add(prop);
        }
    }

    private void removeProperty(TableProperty prop) {
        String group = prop.getGroup();
        List<TableProperty> groupList = groups.get(group);
        if (groupList != null) {
            groupList.remove(prop);

            if (groupList.isEmpty()) {
                groups.remove(group);
            }
        }
    }

    public List<PropertyRow> getPropertyRows() {
        if (groups == null) {
            return null;
        }
        propertyRows = new ArrayList<PropertyRow>();

        for (String group : groups.keySet()) {
            propertyRows.add(new PropertyRow(PropertyRowType.GROUP, group));
            for (TableProperty prop : groups.get(group)) {
                propertyRows.add(new PropertyRow(PropertyRowType.PROPERTY, prop));
            }
        }

        return propertyRows;
    }

    public boolean isEditable() {
        return editable;
    }

    private String getProprtiesTableId(InheritanceLevel inheritanceLevel, ITableProperties props) {
        String id = null;
        ILogicalTable propertiesTable = props.getInheritedPropertiesTable(inheritanceLevel);
        if (propertiesTable != null) {
            String tableUri = propertiesTable.getSource().getUri();
            id = TableUtils.makeTableId(tableUri);
        }
        return id;
    }

    public final IOpenLTable getTable() {
        return WebStudioUtils.getWebStudio().getModel().getTableById(id);
    }

    public String getNewTableId() {
        return newTableId;
    }

    public void setNewTableId(String newTableId) {
        this.newTableId = newTableId;
    }

    public List<SelectItem> getPropertiesToAdd() {
        IOpenLTable table = getTable();
        List<SelectItem> propertiesToAdd = new ArrayList<SelectItem>();
        TablePropertyDefinition[] propDefinitions = TablePropertyDefinitionUtils
                .getDefaultDefinitionsForTable(table.getType(), InheritanceLevel.TABLE, true);
        Collection<String> currentProps = new TreeSet<String>();
        for (PropertyRow row : propertyRows) {
            if (row.getType().equals(PropertyRowType.PROPERTY)) {
                currentProps.add(((TableProperty) row.getData()).getName());
            }
        }

        Map<String, List<TablePropertyDefinition>> propGroups = TablePropertyDefinitionUtils
                .groupProperties(propDefinitions);
        for (Map.Entry<String, List<TablePropertyDefinition>> entry : propGroups.entrySet()) {
            List<SelectItem> items = new ArrayList<SelectItem>();

            for (TablePropertyDefinition propDefinition : entry.getValue()) {
                String propName = propDefinition.getName();
                if (!currentProps.contains(propName)
                        && !"version".equals(propName)
                        && propDefinition.getDeprecation() == null) {
                    items.add(new SelectItem(propName, propDefinition.getDisplayName()));
                }
            }

            if (!items.isEmpty()) {
                SelectItemGroup itemGroup = new SelectItemGroup(entry.getKey());
                itemGroup.setSelectItems(items.toArray(new SelectItem[items.size()]));
                propertiesToAdd.add(itemGroup);
            }
        }

        return propertiesToAdd;
    }

    public String getPropertyToAdd() {
        return propertyToAdd;
    }

    public void setPropertyToAdd(String propertyToAdd) {
        this.propertyToAdd = propertyToAdd;
    }

    public void addNew() {
        TablePropertyDefinition propDefinition = TablePropertyDefinitionUtils.getPropertyByName(propertyToAdd);
        storeProperty(new TableProperty(propDefinition));
        propsToRemove.remove(propertyToAdd);
    }

    public void remove(TableProperty prop) {
        removeProperty(prop);
        propsToRemove.add(prop.getName());
    }
    
    public boolean isChanged() throws Exception {
        if (propertyRows == null) {
            return false;
        }
        ITableProperties props = getTable().getProperties();
        for (PropertyRow row : propertyRows) {
            if (row.getType().equals(PropertyRowType.PROPERTY)) {
                TableProperty property = (TableProperty) row.getData();
                String name = property.getName();
                Object newValue = property.getValue();
                Object oldValue = props.getPropertyValue(name);
                boolean enumArray = property.isEnumArray();
                if ((enumArray && !Arrays.equals((Enum<?>[]) oldValue, (Enum<?>[]) newValue))
                        || (!enumArray && ObjectUtils.notEqual(oldValue, newValue))
                        || (!props.getAllProperties().containsKey(name))) {
                    return true;
                }
            }
        }
        for (String propToRemove : propsToRemove) {
            if (props.getAllProperties().containsKey(propToRemove))
                return true;
        }
        return false;
    }

    public void save() throws Exception {
        IOpenLTable table = getTable();
        table.getGridTable().edit();

        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();
        TableEditorModel tableEditorModel = model.getTableEditorModel(table);
        boolean toSave = false;

        ITableProperties props = table.getProperties();
        for (PropertyRow row : propertyRows) {
            if (row.getType().equals(PropertyRowType.PROPERTY)) {
                TableProperty property = (TableProperty) row.getData();
                String name = property.getName();
                Object newValue = property.getValue();
                Object oldValue = props.getPropertyValue(name);
                boolean enumArray = property.isEnumArray();
                boolean stringArray = property.isStringArray();

                if (newValue == null && oldValue != null) {
                    //if value is empty we have to delete it
                    propsToRemove.add(name);
                } else if ((enumArray && !Arrays.equals((Enum<?>[]) oldValue, (Enum<?>[]) newValue))
                        || (stringArray && !Arrays.equals((String[]) oldValue, (String[]) newValue))
                        || (!enumArray && !stringArray && ObjectUtils.notEqual(oldValue, newValue))) {
                    tableEditorModel.setProperty(name,
                            newValue.getClass().isArray() && ArrayUtils.getLength(newValue) == 0 ? null : newValue);
                    toSave = true;
                }
            }
        }

        for (String propToRemove : propsToRemove) {
            tableEditorModel.removeProperty(propToRemove);
            toSave = true;
        }

        if (toSave) {
            if (studio.isUpdateSystemProperties()) {
                EditHelper.updateSystemProperties(table, tableEditorModel,
                    WebStudioUtils.getWebStudio().getSystemConfigManager().getStringProperty("user.mode"));
            }
            this.newTableId = tableEditorModel.save();
            studio.compile();
        }

        table.getGridTable().stopEditing();
        FacesUtils.removeSessionParam(org.openl.rules.tableeditor.util.Constants.TABLE_EDITOR_MODEL_NAME);
    }

    /*for (Constraint constraint : constraints.getAll()) {
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
    }*/

}
