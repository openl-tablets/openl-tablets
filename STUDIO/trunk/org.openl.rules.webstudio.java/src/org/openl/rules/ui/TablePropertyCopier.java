package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.binding.TableProperties.Property;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.rules.tableeditor.renderkit.TableProperty;

public class TablePropertyCopier extends TableCopier {
    
    private List<TableProperty> propToCopy = new ArrayList<TableProperty>();
    
    public List<TableProperty> getPropToCopy() {
        return propToCopy;
    }

    public void setPropToCopy(List<TableProperty> propToCopy) {
        this.propToCopy = propToCopy;
    }
    
    public TablePropertyCopier(String elementUri1) {        
        start();
        this.elementUri = elementUri1;        
        initTableNames();  
        initProperies();
    }    
    
    private void initProperies() {
        TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions
                .getDefaultDefinitions();
        TableSyntaxNode node = getCopyingTable();
        
        for (TablePropertyDefinition propDefinition : propDefinitions) {
            String name = propDefinition.getName();
            propToCopy.add(new TableProperty(
                    propDefinition.getDisplayName(), node.getPropertyValue(name) != null ? node.getPropertyValue(name).getValue() : null, propDefinition
                            .getType() == null ? null : propDefinition
                            .getType().getInstanceClass(), propDefinition
                            .getGroup(), name,
                    propDefinition.getFormat()));
        }
    }

    @Override
    public String getName() {
        return "changeProperties";
    }
    
    @Override
    protected void reset() {
        super.reset();
        propToCopy.clear();
    }
    
    @Override
    protected Map<String, Object> buildProperties(Property[] tableProperties) {
        Map<String, Object> newProperties = new LinkedHashMap<String, Object>();
        
        //TO DO:
        // validateIfNecessaryPropertiesWereChanged(tableProperties);
        
        for (int i = 0; i < propToCopy.size(); i++) {
            String key = (propToCopy.get(i)).getName();
            Object value = (propToCopy.get(i)).getValue();
            if (value == null || (value instanceof String && StringUtils.isEmpty((String)value))) {
                continue;
            } else {
                newProperties.put(key.trim(), value);
            }
        }
        return newProperties;        
    }
}
