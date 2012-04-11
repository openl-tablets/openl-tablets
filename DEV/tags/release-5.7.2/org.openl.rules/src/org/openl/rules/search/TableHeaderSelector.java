/**
 * Created Apr 29, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.AStringBoolOperator;

/**
 * Handles the header selector that was set in search condition. Checks if the table matches to this selector. 
 * @author snshor
 *
 */

public class TableHeaderSelector extends ATableSyntaxNodeSelector {
    
    private AStringBoolOperator headerSelector;

    /**
     * Constructs the object, according to the info that was set to the search condition.
     * 
     * @param se Search condition. 
     */
    public TableHeaderSelector(SearchConditionElement se) {
        headerSelector = se.isAny(se.getElementValue()) ? null : AStringBoolOperator.makeOperator(se.getOpType2(), se
                .getElementValue());
    }

    public AStringBoolOperator getHeaderSelector() {
        return headerSelector;
    }

    @Override
    public boolean doesTableMatch(TableSyntaxNode node) {
        return headerSelector == null || headerSelector.isMatching(node.getHeaderLineValue().getValue());
    }

    public void setHeaderSelector(AStringBoolOperator headerSelector) {
        this.headerSelector = headerSelector;
    }

}
