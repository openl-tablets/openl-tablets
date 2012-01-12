package org.openl.rules.table.properties;

import java.util.Map;
import java.util.Map.Entry;

import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodDelegator;

public class PropertiesHelper {
    
    public static final String PROPERTIES_HEADER = "properties";
    
    private PropertiesHelper(){};
    
    public static ILogicalTable getPropertiesTableSection(ILogicalTable table) {

        if (table.getHeight() < 2) {
            return null;
        }

        ILogicalTable propTable = table.getRows(1, 1);
        String header = propTable.getSource().getCell(0, 0).getStringValue();
        
        if (!PROPERTIES_HEADER.equals(header)) {
            return null;
        }

        return propTable.getColumns(1);
    }

    public static ITableProperties getTableProperties(IOpenMethod method) {
        if (method instanceof ExecutableRulesMethod) {
            return ((ExecutableRulesMethod) method).getMethodProperties();
        } else if (method.getInfo() != null) {
            TableProperties properties = new TableProperties();
            Map<String, Object> definedInTable = method.getInfo().getProperties();
            if (definedInTable != null) {
                for (Entry<String, Object> property : definedInTable.entrySet()) {
                    properties.setFieldValue(property.getKey(), property.getValue());
                }
            }
            return properties;
            
        }
        if (method instanceof MethodDelegator) {
            return getTableProperties(((MethodDelegator) method).getMethod());
        }
    
        return new TableProperties();
    }

    public static Map<String, Object> getMethodProperties(IOpenMethod method) {
        if (method instanceof ExecutableRulesMethod) {
            return ((ExecutableRulesMethod) method).getProperties();
        } else if (method.getInfo() != null) {
            return method.getInfo().getProperties();
        } else if (method instanceof MethodDelegator) {
            return getMethodProperties(((MethodDelegator) method).getMethod());
        } else {
            return null;
        }
    }
}
