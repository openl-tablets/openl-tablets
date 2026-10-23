package org.openl.rules.table;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.xls.XlsUrlParser;

public interface IOpenLTable {

    IGridTable getGridTable();

    IGridTable getGridTable(String view);

    ITableProperties getProperties();

    String getType();

    /**
     * @return Table name for user. (Firstly will be searched in table properties and then from table header)
     */
    String getDisplayName();

    String getName();

    /**
     * @return true if table is executable at OpenL rules runtime. Also it indicates that tests can be created for this
     * table.
     */
    boolean isExecutable();

    /**
     * @return true if table supports operations over versions
     */
    boolean isVersionable();

    String getUri();

    XlsUrlParser getUriParser();

    String getId();

    /**
     * The version of the table: it changes whenever the text of a cell of the table changes. A client tells the
     * version it read when it writes the table, so that a write over a newer table is refused.
     */
    String getVersion();

    /**
     * Says that the table was written, so it takes a new version even while the module is not compiled again.
     */
    void markWritten();

    boolean isCanContainProperties();

    MetaInfoReader getMetaInfoReader();

    TableSyntaxNode getSyntaxNode();

}
