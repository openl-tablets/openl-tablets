/**
 * Created May 14, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;

/**
 * @author snshor
 *
 */
public class SearchTableRow implements ISearchTableRow {

    private int row;
    private ITableSearchInfo tsi;

    /**
     * @param row
     * @param tsi
     */
    public SearchTableRow(int row, ITableSearchInfo tsi) {
        this.row = row;
        this.tsi = tsi;
    }

    public int getRow() {
        return row;
    }

    public IGridTable getRowTable() {
        return tsi.getRowTable(row);
    }

    public ITableSearchInfo getTableSearchInfo() {
        return tsi;
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tsi.getTableSyntaxNode();
    }

}
