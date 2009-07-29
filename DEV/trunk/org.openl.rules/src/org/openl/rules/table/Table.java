package org.openl.rules.table;

import org.openl.rules.lang.xls.binding.TableProperties;

public class Table implements ITable {
        
    private IGridTable gridTable;
    private TableProperties properties;
    
    public IGridTable getGridTable() {        
        return gridTable;
    }    

    public TableProperties getProperties() {        
        return properties;
    }

    public IGridTable getGridTable(String view) {
        return gridTable;
    }

    
    public void setProperties(TableProperties tableProperties) {
        this.properties = tableProperties;
    }

    public void setGridTable(IGridTable gridTable) {
        this.gridTable = gridTable;
    }

}
