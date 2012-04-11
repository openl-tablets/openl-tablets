package org.openl.rules.table;

import java.util.List;

import org.openl.message.OpenLMessage;
import org.openl.rules.table.properties.ITableProperties;

public interface ITable {

    IGridTable getGridTable();

    IGridTable getGridTable(String view);

    ITableProperties getProperties();

    String getType();

    List<OpenLMessage> getMessages();
    
    /**
     * @return Table name form header string.
     */
    String getNameFromHeader();

    /**
     * @return Table name for user. (Firstly will be searched in table
     *         properties and then from table header)
     */
    String getName();

}
