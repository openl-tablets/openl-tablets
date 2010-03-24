/**
 * Created Apr 19, 2007
 */
package org.openl.rules.search;

import org.openl.types.IOpenClass;
import org.openl.util.AStringBoolOperator;

/**
 * @author snshor
 *
 */

public class ColumnTypeRowSelector extends ATableCellValueSelector {
    AStringBoolOperator columnTypeSelector;

    /**
     * @param se
     */
    public ColumnTypeRowSelector(SearchConditionElement se) {
        columnTypeSelector = se.isAny(se.getElementValueName()) ? null : AStringBoolOperator.makeOperator(se.getOpType1(), se
                .getElementValueName());
        cellValueSelector = se.isAny(se.getElementValue()) ? null : AStringBoolOperator.makeOperator(se.getOpType2(), se
                .getElementValue());
    }

    @Override
    public boolean isRowInTableSelected(ISearchTableRow row, ITableSearchInfo tsi) {
        int nc = tsi.getNumberOfColumns();

        for (int c = 0; c < nc; c++) {
            if (columnTypeSelector != null) {
                IOpenClass ctype = tsi.getColumnType(c);
                if (ctype != null) {
                    String ctypeName = ctype.getName();
                    if (!columnTypeSelector.isMatching(ctypeName)) {
                        continue;
                    }
                } else {
                    continue;
                }
                
            }

            if (cellValueSelector == null) {
                return true;
            }

            Object cellValue = tsi.getTableValue(c, row.getRow());
            if (selectCellValue(cellValue)) {
                return true;
            }

        }

        return false;
    }

}
