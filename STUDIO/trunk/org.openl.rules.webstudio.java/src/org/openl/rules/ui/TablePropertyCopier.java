package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.rules.tableeditor.renderkit.TableProperty;

public class TablePropertyCopier extends TableCopier {
    
    private List<TableProperty> propsToCopy = new ArrayList<TableProperty>();
    
    private List<TableProperty> defaultProps = new ArrayList<TableProperty>();
    
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
        start();
        setElementUri(elementUri);        
        initTableNames();  
        initProperties();
    }    
    
    private void initProperties() {
        TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions
                .getDefaultDefinitions();
        TableSyntaxNode node = getCopyingTable();
        
        for (TablePropertyDefinition propDefinition : propDefinitions) {
            if (!propDefinition.isSystem()) {
                ITableProperties tableProperties = node.getTableProperties();
                String name = propDefinition.getName();
                Object propertyValue = tableProperties.getPropertyValue(name) != null ? 
                        tableProperties.getPropertyValue(name) : null;
                Class<?> propertyType = propDefinition.getType() == null ? 
                        null : propDefinition.getType().getInstanceClass();
                String displayName = propDefinition.getDisplayName();
                String group = propDefinition.getGroup(); 
                String format = propDefinition.getFormat();
                Constraints constraints = propDefinition.getConstraints();
                if (tableProperties.getPropertiesSetByDefault().contains(name)) {
                    defaultProps.add(
                            new TableProperty(displayName, propertyValue, propertyType, 
                                    group, name, format, constraints));
                } else {
                    propsToCopy.add(
                            new TableProperty(displayName, propertyValue, propertyType, 
                                    group, name, format, constraints));
                }
            }
        }
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
}
