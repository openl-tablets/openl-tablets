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
    public ColumnNameRowSelector(SearchConditionElement se) {
        columnNameSelector = se.isAny(se.getElementValueName()) ? null : AStringBoolOperator.makeOperator(se.getOpType1(), se
                .getElementValueName());
        cellValueSelector = se.isAny(se.getElementValue()) ? null : AStringBoolOperator.makeOperator(se.getOpType2(), se
                .getElementValue());
    }

    @Override
    public boolean isRowInTableSelected(ISearchTableRow searchTableRow, ITableSearchInfo searchTableInfo) {
        int colNum = searchTableInfo.getNumberOfColumns();

        for (int c = 0; c < colNum; c++) {
            if (columnNameSelector != null) {
                String cname = searchTableInfo.getColumnName(c);
                if (!columnNameSelector.isMatching(cname)) {
                    continue;
                }
            }

            if (cellValueSelector == null) {
                return true;
            }

            Object cellValue = searchTableInfo.getTableValue(c, searchTableRow.getRow());
            if (selectCellValue(cellValue)) {
                return true;
            }

        }

        return false;
    }

}
