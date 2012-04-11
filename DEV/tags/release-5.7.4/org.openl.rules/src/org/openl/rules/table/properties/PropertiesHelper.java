package org.openl.rules.table.properties;

import org.openl.rules.table.ILogicalTable;

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

}
