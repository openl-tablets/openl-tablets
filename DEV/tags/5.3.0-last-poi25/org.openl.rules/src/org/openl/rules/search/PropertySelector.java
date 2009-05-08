/**
 * Created Apr 19, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.AStringBoolOperator;

/**
 * @author snshor
 *
 */

public class PropertySelector extends ATableSyntaxNodeSelector {
    AStringBoolOperator propertyNameSelector;

    AStringBoolOperator propertyValueSelector;

    /**
     * @param se
     */
    public PropertySelector(SearchElement se) {
        propertyNameSelector = se.isAny(se.getValue1()) ? null : AStringBoolOperator.makeOperator("matches", se
                .getValue1());
        propertyValueSelector = se.isAny(se.getValue2()) ? null : AStringBoolOperator.makeOperator(se.getOpType2(), se
                .getValue2());
    }

    public boolean selectProperty(TableProperties.Property prop) {
        return (propertyNameSelector == null || propertyNameSelector.op(prop.getKey().getValue()))
                && (propertyValueSelector == null || propertyValueSelector.op(prop.getValue().getValue()));
    }

    @Override
    public boolean selectTable(TableSyntaxNode tsn) {
        TableProperties tp = tsn.getTableProperties();
        if (tp == null) {
            return propertyNameSelector == null && propertyValueSelector == null;
        }

        TableProperties.Property[] pp = tp.getProperties();
        for (int i = 0; i < pp.length; i++) {
            if (selectProperty(pp[i])) {
                return true;
            }
        }

        return false;
    }

}
