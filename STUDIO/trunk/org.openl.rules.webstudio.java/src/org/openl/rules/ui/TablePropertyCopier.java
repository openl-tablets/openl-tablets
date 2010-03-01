package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.component.html.HtmlOutputText;

import org.ajax4jsf.component.UIRepeat;
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
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.renderkit.HTMLRenderer;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.EnumUtils;
import org.openl.util.conf.Version;

public class TablePropertyCopier extends TableCopier {

    public static final String INIT_VERSION = "0.0.1";

    private static final Log LOG = LogFactory.getLog(TablePropertyCopier.class);

    private List<TableProperty> propsToCopy = new ArrayList<TableProperty>();

    private List<TableProperty> defaultProps = new ArrayList<TableProperty>();

    private UIRepeat propsTable;
    
    private HtmlOutputText enumOutput;
    private HtmlOutputText enumArrayOutput;

    public HtmlOutputText getEnumOutput() {
        return enumOutput;
    }

    public void setEnumOutput(HtmlOutputText enumOutput) {
        this.enumOutput = enumOutput;
    }

    public HtmlOutputText getEnumArrayOutput() {
        return enumArrayOutput;
    }

    public void setEnumArrayOutput(HtmlOutputText enumArrayOutput) {
        this.enumArrayOutput = enumArrayOutput;
    }
    
    public String getEnumValue() {
        
        String componentId = enumOutput.getId();
        TableProperty property = (TableProperty)enumOutput.getAttributes().get("property");
        
        return getEnumSelectComponentCode(componentId, property);
    }
    
    public String getEnumArrayValue() {

        String componentId = enumArrayOutput.getId();
        TableProperty property = (TableProperty)enumArrayOutput.getAttributes().get("property");
        
        return getEnumMultiSelectComponentCode(componentId, property);
    }

    public List<TableProperty> getPropsToCopy() {
        return propsToCopy;
    }

    public void setPropsToCopy(List<TableProperty> propsToCopy) {
        this.propsToCopy = propsToCopy;
    }
    
    public void setDefaultProps(List<TableProperty> defaultProps) {
        this.defaultProps = defaultProps;
    }

    public List<TableProperty> getDefaultProps() {
        return defaultProps;
    }

    public TablePropertyCopier(String elementUri) {
        this(elementUri, false);
    }

    public TablePropertyCopier(String elementUri, boolean edit) {
        start();
        setElementUri(elementUri);
        initTableNames();
        initProperties(false);
        setEdit(edit);
    }

    /**
     * When doing the copy of the table, just properties that where physically defined in original table
     * get to the properties copying page.
     */
    private void initProperties(boolean excludeDimensionalProperties) {
        
        TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions.getDefaultDefinitions();
        TableSyntaxNode node = getCopyingTable();
        
        for (TablePropertyDefinition propDefinition : propDefinitions) {
            
            if (!propDefinition.isSystem() 
                    && !(excludeDimensionalProperties && propDefinition.isDimensional())) {
                
                ITableProperties tableProperties = node.getTableProperties();
                
                String name = propDefinition.getName();
                Object propertyValue = tableProperties.getPropertyValue(name) != null ? 
                        tableProperties.getPropertyValue(name) : null;
                
                if (!tableProperties.getPropertiesDefinedInTable().containsKey(name)) {
                    propertyValue = StringUtils.EMPTY;
                }
                
                Class<?> propertyType = null;
                
                if (propDefinition.getType() != null) {
                    propertyType = propDefinition.getType().getInstanceClass();
                }
                
                String displayName = propDefinition.getDisplayName();                
                String format = propDefinition.getFormat();
                
                TableProperty tableProperty = new TableProperty.TablePropertyBuilder(name, displayName)
                        .value(propertyValue).type(propertyType).format(format)
                        .build();
                
                propsToCopy.add(tableProperty);
            }
        }
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
        for (TableProperty property : propsToCopy) {
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
    protected void reset() {
        super.reset();
        propsToCopy.clear();
    }
    
    @Override
    protected Map<String, Object> buildProperties() {
        Map<String, Object> newProperties = new LinkedHashMap<String, Object>();
        newProperties.putAll(buildSystemProperties());
        //TO DO:
        // validateIfNecessaryPropertiesWereChanged(tableProperties);
        
        for (int i = 0; i < propsToCopy.size(); i++) {
            String key = (propsToCopy.get(i)).getName();
            Object value = (propsToCopy.get(i)).getValue();
            if (value == null || (value instanceof String && StringUtils.isEmpty((String)value))) {
                continue;
            } else {
                newProperties.put(key.trim(), value);
            }
        }        
        newProperties.putAll(processDefaultValues());
        return newProperties;        
    }
    
    /**
     * Base table may have properties set by default. These properties exists only in {@link TableSyntaxNode} 
     * representation of the table and not in source of the table. So when copying we must add to new table just 
     * properties and values that were revalued during the copy.
     * 
     * @return Map of properties that in base table were as default and were revalued during copy for new table.
     */
    private Map<String, Object> processDefaultValues() {
        Map<String, Object> revaluedDefaultProperties = new HashMap<String, Object>();
        TableSyntaxNode node = getCopyingTable();
        ITableProperties baseTableProperties = node.getTableProperties();        
        if (baseTableProperties != null) {
            for (TableProperty defaultCopyingProp : defaultProps) {
                String propName = defaultCopyingProp.getName();
                Object basePropValue = baseTableProperties.getPropertyValue(propName);
                Object newPropValue = defaultCopyingProp.getValue();
                if (newPropValue != null) {
                    boolean emptyString = false;
                    if (newPropValue instanceof String) {
                         emptyString = StringUtils.isEmpty((String) newPropValue);
                    }
                    if (!basePropValue.equals(newPropValue) && !emptyString) {
                        revaluedDefaultProperties.put(propName, newPropValue);
                    }
                }
            }
        }
        return revaluedDefaultProperties;
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

    public Version getMinNextVersion() {
        Object originalVersion = getVersion().getValue();
        if (StringUtils.isNotEmpty((String)originalVersion)) {
            return Version.parseVersion((String) originalVersion, 0, "..");
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
                tableEditorModel.save();
            } catch (Exception e) {
                LOG.error("Can not update table properties", e);
            }
        }
    }
    
    private String getEnumSelectComponentCode(String componentId, TableProperty tableProperty) {

        Class<?> instanceClass = tableProperty.getType();
        String value = tableProperty.getStringValue();

        String[] values = EnumUtils.getNames(instanceClass);
        String[] displayValues = EnumUtils.getValues(instanceClass);

        String id = String.format("%s:enumSelect", componentId);

        String componentCode = new HTMLRenderer().getSingleSelectComponentCode(id, values, displayValues, value);

        return getEditorHTMLCode(id, componentCode);
    }

    private String getEnumMultiSelectComponentCode(String componentId, TableProperty tableProperty) {
        
        Class<?> instanceClass = tableProperty.getType().getComponentType();

        String valueString = tableProperty.getStringValue();

        String[] values = EnumUtils.getNames(instanceClass);
        String[] displayValues = EnumUtils.getValues(instanceClass);
        
        String id = String.format("%s:enumArraySelect", componentId);
        
        String componentCode = new HTMLRenderer().getMultiSelectComponentCode(id, values, displayValues, valueString);

        return getEditorHTMLCode(id, componentCode);
    }

    private String getEditorHTMLCode(String id, String editorCode) {
        return String.format(
                  "<div id='%1$s'></div>"
                + "<script type='text/javascript'>"
                + "var editor = %2$s;"
                // editor value setter code
                + "editor.input.onblur=function(){var newValue = this.getValue();"
                + "$('%1$s').up().down('input[name!=id]').value=newValue;return false;};"
                + "</script>", id, editorCode);
    }

}
