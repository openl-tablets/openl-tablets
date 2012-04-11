package org.openl.rules.table;

import org.openl.rules.table.properties.ITableProperties;
/**
 * 
 * @author DLiauchuk
 * 
 * The high logical representation of table
 */
public class Table implements ITable {
        
    private IGridTable gridTable;
    private ITableProperties properties;
    private String type;
    
    public IGridTable getGridTable() {        
        return gridTable;
    }    

    public ITableProperties getProperties() {        
        return properties;
    }

    public IGridTable getGridTable(String view) {
        return gridTable;
    }
    
    public void setProperties(ITableProperties tableProperties) {
        this.properties = tableProperties;
    }

    public void setGridTable(IGridTable gridTable) {
        this.gridTable = gridTable;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
