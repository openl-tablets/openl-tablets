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
    public TableHeaderSelector(SearchElement se) {
        headerSelector = se.isAny(se.getValue2()) ? null : AStringBoolOperator.makeOperator(se.getOpType2(), se
                .getValue2());
    }

    public AStringBoolOperator getHeaderSelector() {
        return headerSelector;
    }

    @Override
    public boolean selectTable(TableSyntaxNode node) {
        return headerSelector == null || headerSelector.op(node.getHeaderLineValue().getValue());
    }

    public void setHeaderSelector(AStringBoolOperator headerSelector) {
        this.headerSelector = headerSelector;
    }

}
