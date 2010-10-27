/**
 * Created May 4, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.ASelector;

/**
 * @author snshor
 *
 */
public abstract class ATableRowSelector extends ASelector<ISearchTableRow> {
    
    /**
     * Finds the table search info, according to its type.
     * @param tableSyntaxNode
     * @return
     */
    public static ITableSearchInfo getTableSearchInfo(TableSyntaxNode tsn) {
        if (XlsNodeTypes.XLS_DT.toString().equals(tsn.getType())) {
            return new DecisionTableSearchInfo(tsn);
        } else if (XlsNodeTypes.XLS_DATA.toString().equals(tsn.getType())) {
            return new DataTableSearchInfo(tsn);
        } else if (XlsNodeTypes.XLS_TEST_METHOD.toString().equals(tsn.getType())) {
            return new DataTableSearchInfo(tsn);
        } else if (XlsNodeTypes.XLS_RUN_METHOD.toString().equals(tsn.getType())) {
            return new DataTableSearchInfo(tsn);
        } else if (XlsNodeTypes.XLS_SPREADSHEET.toString().equals(tsn.getType())) {
            return new TableSearchInfo(tsn);
        } else if (XlsNodeTypes.XLS_TBASIC.toString().equals(tsn.getType())) {
            return new TableSearchInfo(tsn);
        } else if (XlsNodeTypes.XLS_COLUMN_MATCH.toString().equals(tsn.getType())) {
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
