/**
 * Created Apr 19, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.ASelector;

/**
 * An abstraction for all selectors.
 * 
 * @author <i>snshor</i><br>
 * commented by <i>DLiauchuk</i> 
 *
 */
public abstract class ATableSyntaxNodeSelector extends ASelector<TableSyntaxNode> {

    public boolean select(TableSyntaxNode table) {
        return doesTableMatch(table);
    }

    /**
     * @param node <code>{@link TableSyntaxNode}</code> to check if this table satisfy to the selector. 
     * @return <code>True</code> if table satisfy to the certain selector.
     */
    public abstract boolean doesTableMatch(TableSyntaxNode node);
}
