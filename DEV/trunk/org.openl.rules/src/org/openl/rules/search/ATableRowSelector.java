/**
 * Created May 4, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.ASelector;

/**
 * @author snshor
 *
 */
public abstract class ATableRowSelector extends ASelector<ISearchTableRow> implements ITableNodeTypes {
    
    /**
     * Finds the table search info, according to its type.
     * @param tableSyntaxNode
     * @return
     */
    public static ITableSearchInfo getTableSearchInfo(TableSyntaxNode tsn) {
        if (XLS_DT.equals(tsn.getType())) {
            return new DecisionTableSearchInfo(tsn);
        } else if (XLS_DATA.equals(tsn.getType())) {
            return new DataTableSearchInfo(tsn);
        } else if (XLS_TEST_METHOD.equals(tsn.getType())) {
            return new DataTableSearchInfo(tsn);
        } else if (XLS_RUN_METHOD.equals(tsn.getType())) {
            return new DataTableSearchInfo(tsn);
        } else if (XLS_SPREADSHEET.equals(tsn.getType())) {
            return new TableSearchInfo(tsn);
        } else if (XLS_TBASIC.equals(tsn.getType())) {
            return new TableSearchInfo(tsn);
        } else if (XLS_COLUMN_MATCH.equals(tsn.getType())) {
            return new TableSearchInfo(tsn);
        }
        return null;
    }

    public boolean select(ISearchTableRow obj) {
        return selectRow((ISearchTableRow) obj);
    }

    public boolean selectRow(ISearchTableRow row) {
        ITableSearchInfo tsi = row.getTableSearchInfo();
        return isRowInTableSelected(row, tsi);
    }

    public abstract boolean isRowInTableSelected(ISearchTableRow row, ITableSearchInfo tsi);

}
