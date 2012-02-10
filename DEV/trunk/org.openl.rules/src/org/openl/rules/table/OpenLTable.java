package org.openl.rules.table;

import java.util.List;

import org.openl.message.OpenLMessage;
import org.openl.rules.table.properties.ITableProperties;
/**
 * The high logical representation of table.
 * 
 * @author DLiauchuk
 * 
 */
public class OpenLTable implements IOpenLTable {
        
    private IGridTable gridTable;
    private ITableProperties properties;
    private String type;
    private List<OpenLMessage> messages;
    private boolean isExecutable;
    private boolean isVersionable;
    private String uri;
    private String name;
    private String technicalName;
    private boolean canContainProperties;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
  
    public String getTechnicalName() {
        return technicalName;
    }

    public void setTechnicalName(String technicalName) {
        this.technicalName = technicalName;
    }

    public void setExecutable(boolean isExecutable) {
        this.isExecutable = isExecutable;
    }

    public boolean isExecutable() {        
        return isExecutable;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isVersionable() {        
        return isVersionable;
    }

    public boolean isCanContainProperties() {
        return canContainProperties;
    }

    public void setCanContainProperties(boolean canContainProperties) {
        this.canContainProperties = canContainProperties;
    }

}
