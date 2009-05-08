/**
 * Created Apr 19, 2007
 */
package org.openl.rules.search;

import org.openl.util.AStringBoolOperator;

/**
 * @author snshor
 *
 */

public class ColumnNameRowSelector extends ATableCellValueSelector {
    AStringBoolOperator columnNameSelector;

    /**
     * @param se
     */
    public ColumnNameRowSelector(SearchElement se) {
        columnNameSelector = se.isAny(se.getValue1()) ? null : AStringBoolOperator.makeOperator(se.getOpType1(), se
                .getValue1());
        cellValueSelector = se.isAny(se.getValue2()) ? null : AStringBoolOperator.makeOperator(se.getOpType2(), se
                .getValue2());
    }

    @Override
    public boolean selectRowInTable(ISearchTableRow row, ITableSearchInfo tsi) {
        int nc = tsi.numberOfColumns();

        for (int c = 0; c < nc; c++) {
            if (columnNameSelector != null) {
                String cname = tsi.columnName(c);
                if (!columnNameSelector.op(cname)) {
                    continue;
                }
            }

            if (cellValueSelector == null) {
                return true;
            }

            Object cellValue = tsi.tableValue(c, row.getRow());
            if (selectCellValue(cellValue)) {
                return true;
            }

        }

        return false;
    }

}
