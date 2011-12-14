package org.openl.rules.webstudio.web.tableeditor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;

@ManagedBean
@RequestScoped
public class TablePropertiesBean {

    //private static final Log LOG = LogFactory.getLog(TablePropertiesBean.class);

    private IOpenLTable table;
    private ITableProperties props;
    private List<TableProperty> listProperties;

    private String newTableUri;

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

            if (PropertiesChecker.isPropertySuitableForTableType(name, table.getType())) {
            	TableProperty.TablePropertyBuilder builder = new TableProperty.TablePropertyBuilder(name, type).value(value)
                .displayName(displayName).group(group).format(format).constraints(constraints)
                .description(description).system(system).inheritanceLevel(inheritanceLevel);
            	if (InheritanceLevel.MODULE.equals(inheritanceLevel)
            			|| InheritanceLevel.CATEGORY.equals(inheritanceLevel)) {
            		builder.inheritedTableUri(getProprtiesTableUri(inheritanceLevel));
            	}
                TableProperty prop = builder.build();
                listProp.add(prop);
            }
        }
        return listProp;
    }

    public boolean isEditable() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();

        boolean isDispatcherValidationNode = ((TableSyntaxNodeAdapter) table).getTechnicalName().startsWith(
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

    public Map<String, List<TableProperty>> getGroupProps() {
        Map<String, List<TableProperty>> groupProps = new LinkedHashMap<String, List<TableProperty>>();
        if (isShowProperties()) {
	        for (TableProperty prop : listProperties) {
	            String group = prop.getGroup();
	            List<TableProperty> groupList = groupProps.get(group);
	            if (groupList == null) {
	                groupList = new ArrayList<TableProperty>();
	                groupProps.put(group, groupList);
	            }
	            if (!groupList.contains(prop) && prop.getValue() != null) {
	                groupList.add(prop);
	            }
	        }
        }
        return groupProps;
    }

    public boolean isShowProperties() {
        String tableType = table.getType();
        return tableType != null && !tableType.equals(XlsNodeTypes.XLS_OTHER.toString())
                && !tableType.equals(XlsNodeTypes.XLS_ENVIRONMENT.toString())
                && !tableType.equals(XlsNodeTypes.XLS_PROPERTIES.toString());
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

}
