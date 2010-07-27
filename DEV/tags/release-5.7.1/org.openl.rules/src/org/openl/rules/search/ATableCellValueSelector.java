/**
 * Created May 14, 2007
 */
package org.openl.rules.search;

import java.lang.reflect.Array;

import org.openl.util.AStringBoolOperator;

/**
 * @author snshor
 *
 */
public abstract class ATableCellValueSelector extends ATableRowSelector {
    protected AStringBoolOperator cellValueSelector;

    protected boolean selectCellValue(Object cellValue) {

        if (cellValue == null) {
            return false;
        }

        if (cellValue.getClass().isArray()) {
            int len = Array.getLength(cellValue);
            for (int i = 0; i < len; i++) {
                Object cv = Array.get(cellValue, i);
                if (selectCellValue(cv)) {
                    return true;
                }
            }
            return false;
        }

        String strCellValue = String.valueOf(cellValue);

        return cellValueSelector.isMatching(strCellValue);

    }

}
