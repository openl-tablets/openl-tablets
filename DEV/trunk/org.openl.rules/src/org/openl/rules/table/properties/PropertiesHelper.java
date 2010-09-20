package org.openl.rules.table.properties;

import org.openl.rules.table.IGridTable;

public class PropertiesHelper {
    
    public static final String PROPERTIES_HEADER = "properties";
    
    private PropertiesHelper(){};
    
    public static IGridTable getPropertiesTableSection(IGridTable table) {

        if (table.getGridHeight() < 2) {
            return null;
        }

        IGridTable propTable = table.rows(1, 1);
        String header = propTable.getGridTable().getCell(0, 0).getStringValue();
        
        if (!PROPERTIES_HEADER.equals(header)) {
            return null;
        }

        return propTable.columns(1);
    }

}
