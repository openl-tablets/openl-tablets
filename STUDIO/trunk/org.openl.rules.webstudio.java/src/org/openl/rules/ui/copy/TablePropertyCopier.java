package org.openl.rules.ui.copy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.constraints.Constraint;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.constraints.LessThanConstraint;
import org.openl.rules.table.constraints.MoreThanConstraint;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tablewizard.PropertiesBean;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.conf.Version;
import org.richfaces.component.UIRepeat;

public class TablePropertyCopier extends TableCopier {
    
    private static final Log LOG = LogFactory.getLog(TablePropertyCopier.class);
    
    /**
     * Bean - container of all properties for new copy of the original table.
     */
    private PropertiesBean propertiesManager; 

    public static final String INIT_VERSION = "0.0.1";

    private UIRepeat propsTable;
    
    public PropertiesBean getPropertiesManager() {
        return propertiesManager;
    }
    
    public TablePropertyCopier(String elementUri) {
        this(elementUri, false);
    }

    public TablePropertyCopier(String elementUri, boolean edit) {
        start();
        setElementUri(elementUri);
        propertiesManager = new PropertiesBean(getAllPossibleProperties(getCopyingTable().getType()));
        initTableNames();
        initProperties();
        setEdit(edit);
    }
    
    private List<String> getAllPossibleProperties(String tableType) {
        List<String> possibleProperties = new ArrayList<String>();
        TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions.getDefaultDefinitions();
        for (TablePropertyDefinition propDefinition : propDefinitions) {
            if (!propDefinition.isSystem()) {
                String propertyName = propDefinition.getName();
                
                // check if the property can be defined in current type of table 
                // and if property can be defined on TABLE level.
                if (PropertiesChecker.isPropertySuitableForTableType(propertyName, tableType) 
                        && PropertiesChecker.isPropertySuitableForLevel(InheritanceLevel.TABLE, propertyName)) {
                    possibleProperties.add(propDefinition.getName());
                }
            }
        }
        return possibleProperties;
    }

    /**
     * When doing the copy of the table, properties that where physically defined in original table
     * get to the properties copying page.
     */
    private void initProperties() {
        List<TableProperty> definedProperties = new ArrayList<TableProperty>();        
        TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions.getDefaultDefinitions();
        TableSyntaxNode node = getCopyingTable();
        
        for (TablePropertyDefinition propDefinition : propDefinitions) {
            
            if (!propDefinition.isSystem()) {
                
                ITableProperties tableProperties = node.getTableProperties();
                
                String name = propDefinition.getName();
                Object propertyValue = tableProperties.getPropertyValue(name) != null ? 
                        tableProperties.getPropertyValue(name) : null;
                
                if (tableProperties.getPropertiesDefinedInTable().containsKey(name)) {
                    Class<?> propertyType = null;

                    if (propDefinition.getType() != null) {
                        propertyType = propDefinition.getType().getInstanceClass();
                    }

                    String displayName = propDefinition.getDisplayName();
                    String format = propDefinition.getFormat();
                    
                    TableProperty tableProperty = new TableProperty.TablePropertyBuilder(name, propertyType).value(
                                propertyValue).displayName(displayName).format(format).build();
    
                    definedProperties.add(tableProperty);
                    
                }
            }
        }
        propertiesManager.setProperties(definedProperties);
    }

    public String getValidationJS() {
        StringBuilder validation = new StringBuilder();
        TableProperty prop = getCurrentProp();
        Constraints constraints = prop.getConstraints();
        if (constraints != null) {
            String inputId = getInputIdJS(prop.getName());
            for (Constraint constraint : constraints.getAll()) {
                if (constraint instanceof LessThanConstraint || constraint instanceof MoreThanConstraint) {
                    String validator = constraint instanceof LessThanConstraint ? "lessThan" : "moreThan";
                    String compareToField = (String) constraint.getParams()[0];
                    String compareToFieldId = getInputIdJS(compareToField);
                    TableProperty compareToProperty = getProperty(prop.getName());
                    String compareToPropertyDisplayName = compareToProperty == null ? ""
                            : compareToProperty.getDisplayName();
                    validation.append("new Validation(" + inputId + ", '"
                            + validator + "', '', {compareToFieldId:" + compareToFieldId
                            + ",messageParams:'" + compareToPropertyDisplayName + "'})");
                }
            }
        }
        return validation.toString();
    }

    private String getInputIdJS(String propName) {
        return "$('" + propsTable.getParent().getId() + "').down('input[type=hidden][name=id][value="
            + propName + "]').up().down('input').id";
    }

    private TableProperty getCurrentProp() {
        return (TableProperty) propsTable.getRowData();
    }

    private TableProperty getProperty(String name) {
        for (TableProperty property : propertiesManager.getProperties()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "changeProperties";
    }
    
    @Override
    protected Map<String, Object> buildProperties() {
        Map<String, Object> newProperties = new LinkedHashMap<String, Object>();
        newProperties.putAll(buildSystemProperties());
        //TO DO:
        // validateIfNecessaryPropertiesWereChanged(tableProperties);
        
        for (TableProperty property : propertiesManager.getProperties()) {
            String name = property.getName();
            Object value = property.getValue();
            if (value == null || (value instanceof String && StringUtils.isEmpty((String)value))) {
                continue;
            } else {
                newProperties.put(name.trim(), value);
            }
        }        
        return newProperties;        
    }
    

    public void setPropsTable(UIRepeat propsTable) {
        this.propsTable = propsTable;
    }

    public UIRepeat getPropsTable() {
        return propsTable;
    }

    /**
     * It is only necessary for version editor.
     * 
     * @return "Version" TableProperty.
     */
    public TableProperty getVersion() {
        return getProperty("version");
    }

    /**
     * @return Min version that can be set into new copy of original table.
     */
    public Version getMinNextVersion() {
        TableProperty originalVersion = getVersion();
        if (originalVersion != null && StringUtils.isNotEmpty((String) originalVersion.getValue())) {
            return Version.parseVersion((String) originalVersion.getValue(), 0, "..");
        } else {
            return Version.parseVersion(INIT_VERSION, 0, "..");
        }
    }
    
    protected void updatePropertiesForOriginalTable(Map<String, String> properties) {
        if (properties.size() > 0) {
            WebStudio studio = WebStudioUtils.getWebStudio();
            ProjectModel model = studio.getModel();
            TableEditorModel tableEditorModel = model.getTableEditorModel(getElementUri());

            Set<String> propNames = properties.keySet();
            try {
                for (String propName : propNames) {
                    String propValue = properties.get(propName);
                    tableEditorModel.setProperty(propName, propValue);
                }
                getModifiedWorkbooks().add(tableEditorModel.getSheetSource().getWorkbookSource());
            } catch (Exception e) {
                LOG.error("Can not update table properties for original table", e);
            }
        }
    }
    
}
