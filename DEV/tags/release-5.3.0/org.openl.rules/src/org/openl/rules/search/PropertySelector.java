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
    public PropertySelector(SearchConditionElement se) {
        propertyNameSelector = se.isAny(se.getElementValueName()) ? null : AStringBoolOperator.makeOperator("matches", se
                .getElementValueName());
        propertyValueSelector = se.isAny(se.getElementValue()) ? null : AStringBoolOperator.makeOperator(se.getOpType2(), se
                .getElementValue());
    }

    public boolean selectProperty(TableProperties.Property prop, TableProperties tp) {
        return (propertyNameSelector == null || propertyNameSelector.isMatching(prop.getKey().getValue()))
                && (propertyValueSelector == null || propertyValueSelector.isMatching(tp
                        .getPropertyValueAsString(prop.getKey().getValue())));
    }

    @Override
    public boolean isTableSelected(TableSyntaxNode tsn) {
        TableProperties tp = tsn.getTableProperties();
        if (tp == null) {
            return propertyNameSelector == null && propertyValueSelector == null;
        }

        TableProperties.Property[] pp = tp.getProperties();
        for (int i = 0; i < pp.length; i++) {
            if (selectProperty(pp[i], tp)) {
                return true;
            }
        }

        return false;
    }

}
