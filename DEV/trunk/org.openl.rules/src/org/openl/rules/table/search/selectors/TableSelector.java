package org.openl.rules.table.search.selectors;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.ASelector;

/**
 * An abstraction for table selectors.
 * 
 * @author <i>snshor</i><br>
 * commented by <i>DLiauchuk</i> 
 *
 */
public abstract class TableSelector extends ASelector<TableSyntaxNode> {

    /**
     * @param table <code>{@link TableSyntaxNode}</code> to check if this table satisfy to the selector. 
     * @return <code>True</code> if table satisfy to the certain selector.
     */
    @Override
    public abstract boolean select(TableSyntaxNode table);

}
