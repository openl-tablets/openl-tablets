/**
 * Created May 4, 2007
 */
package org.openl.rules.search;

import org.openl.rules.table.IGridTable;

/**
 * @author snshor
 *
 * Contains information on table row (logical) to be used in search methods
 */
public interface ISearchTableRow {
    int getRow();

    IGridTable getRowTable();

    ITableSearchInfo getTableSearchInfo();

}
