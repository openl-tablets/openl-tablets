package org.openl.rules.table;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openl.message.OpenLMessage;
import org.openl.rules.table.properties.ITableProperties;
/**
 * The high logical representation of table.
 * 
 * @author DLiauchuk
 * 
 */
public class Table implements ITable {
        
    private IGridTable gridTable;
    private ITableProperties properties;
    private String type;
    private List<OpenLMessage> messages;

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

    public List<OpenLMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<OpenLMessage> messages) {
        this.messages = messages;
    }

    public String getNameFromHeader() {
        return StringUtils.EMPTY;
    }

    public String getName() {
        if (properties != null) {
            return properties.getName();
        } else {
            return getNameFromHeader();
        }
    }
}
