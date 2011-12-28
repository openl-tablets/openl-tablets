package org.openl.rules.webstudio.web.tableeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
//import org.openl.rules.table.constraints.Constraint;
//import org.openl.rules.table.constraints.LessThanConstraint;
//import org.openl.rules.table.constraints.MoreThanConstraint;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.renderkit.PropertyGroup;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;

@ManagedBean
@ViewScoped
public class TablePropertiesBean {

    //private static final Log LOG = LogFactory.getLog(TablePropertiesBean.class);

    private IOpenLTable table;
    private ITableProperties props;
    private List<TableProperty> listProperties;
    private List<PropertyGroup> groups;

    private String newTableUri;
    private String propertyToAdd;

    public TablePropertiesBean() {
        WebStudio studio = WebStudioUtils.getWebStudio();

        String uri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);
        table = studio.getModel().getTable(uri);

        if (table == null) {
            uri = studio.getTableUri();
            table = studio.getModel().getTable(uri);
        }

        if (isShowProperties()) {
            this.props = table.getProperties();
            this.listProperties = initPropertiesList();
        }
    }

    public List<TableProperty> getProperties() {
        return listProperties;
    }

    private List<TableProperty> initPropertiesList() {
        List<TableProperty> listProp = new ArrayList<TableProperty>();
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
                    prop.setInheritedTableUri(getProprtiesTableUri(inheritanceLevel));
                }
                listProp.add(prop);
            }
        }
        return listProp;
    }

    public boolean isEditable() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();

        boolean isDispatcherValidationNode = table.getTechnicalName().startsWith(
                DispatcherTablesBuilder.DEFAULT_DISPATCHER_TABLE_NAME);

        return projectModel.isEditable() && !isDispatcherValidationNode;
    }

    private String getProprtiesTableUri(InheritanceLevel inheritanceLevel) {
        String uri = null;
        ILogicalTable propertiesTable = props.getInheritedPropertiesTable(inheritanceLevel);
        if (propertiesTable != null) {
            String tableUri = propertiesTable.getSource().getUri();
            uri = StringTool.encodeURL(tableUri);
        }
        return uri;
    }

    public List<PropertyGroup> getPropertyGroups() {
        groups = new ArrayList<PropertyGroup>();
        Map<String, List<TableProperty>> groupProps = new LinkedHashMap<String, List<TableProperty>>();
        if (isShowProperties()) {
            for (TableProperty prop : listProperties) {
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
        }
        for (String groupName : groupProps.keySet()) {
            PropertyGroup group = new PropertyGroup();
            group.setGroup(groupName);
            group.setProperties(groupProps.get(groupName));
            groups.add(group);
        }
        return groups;
    }

    public boolean isShowProperties() {
        return table.isCanContainProperties();
    }

    public void save() throws Exception {
        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();
        TableEditorModel tableEditorModel = model.getTableEditorModel(table);
        boolean toSave = false;
        for (TableProperty property : listProperties) {
            String name = property.getName();
            Object newValue = property.getValue();
            if (newValue != null
                    && !newValue.equals(props.getPropertyValue(name))) {
                tableEditorModel.setProperty(name, newValue);
                toSave = true;
            }
        }
        if (toSave) {
            this.newTableUri = tableEditorModel.save();
            studio.rebuildModel();
        }
    }

    public String getNewTableUri() {
        return newTableUri;
    }

    public void setNewTableUri(String newTableUri) {
        this.newTableUri = newTableUri;
    }

    public List<SelectItem> getPropertiesToAdd() {
        List<SelectItem> propertiesToAdd = new ArrayList<SelectItem>();
        TablePropertyDefinition[] propDefinitions = TablePropertyDefinitionUtils
                .getDefaultDefinitionsForTable(table.getType(), InheritanceLevel.TABLE, true);
        Collection<String> currentProps = new TreeSet<String>();
        for (TableProperty prop : listProperties) {
            currentProps.add(prop.getName());
        }

        for (TablePropertyDefinition propDefinition : propDefinitions) {
            String propName = propDefinition.getName();
            if (!currentProps.contains(propName)) {
                propertiesToAdd.add(new SelectItem(propName, propDefinition.getDisplayName()));
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

    public void addProperty() {
    	TablePropertyDefinition propDefinition = TablePropertyDefinitionUtils.getPropertyByName(propertyToAdd);
        listProperties.add(new TableProperty(propDefinition));
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
