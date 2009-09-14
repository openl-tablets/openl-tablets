/**
 * Created Apr 29, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.AStringBoolOperator;

/**
 * @author snshor
 *
 */

public class TableHeaderSelector extends ATableSyntaxNodeSelector {
    AStringBoolOperator headerSelector;

    /**
     * @param se
     */
    public TableHeaderSelector(SearchConditionElement se) {
        headerSelector = se.isAny(se.getElementValue()) ? null : AStringBoolOperator.makeOperator(se.getOpType2(), se
                .getElementValue());
    }

    public AStringBoolOperator getHeaderSelector() {
        return headerSelector;
    }

    @Override
    public boolean isTableSelected(TableSyntaxNode node) {
        return headerSelector == null || headerSelector.isMatching(node.getHeaderLineValue().getValue());
    }

    public void setHeaderSelector(AStringBoolOperator headerSelector) {
        this.headerSelector = headerSelector;
    }

}
