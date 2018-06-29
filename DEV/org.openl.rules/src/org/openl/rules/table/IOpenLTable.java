package org.openl.rules.table;

import java.util.Collection;

import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.xls.XlsUrlParser;

public interface IOpenLTable {

    IGridTable getGridTable();

    IGridTable getGridTable(String view);

    ITableProperties getProperties();

    String getType();

    Collection<OpenLMessage> getMessages();

    /**
     * @return Table name for user. (Firstly will be searched in table
     *         properties and then from table header)
     */
    String getDisplayName();
    String getName();

    /**
     * 
     * @return true if table is executable at OpenL rules runtime. Also it indicates that tests can be created for this 
     * table.   
     */
    boolean isExecutable();
    
    /**
     * 
     * @return true if table supports operations over versions
     */
    boolean isVersionable();

    String getUri();

    XlsUrlParser getUriParser();

    String getId();

    boolean isCanContainProperties();

    MetaInfoReader getMetaInfoReader();

}
