/**
 * Created Apr 19, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.ASelector;

/**
 * @author snshor
 *
 */
public abstract class ATableSyntaxNodeSelector extends ASelector<TableSyntaxNode> {

    public boolean select(TableSyntaxNode table) {
        return isTableSelected(table);
    }

    /**
     * @param node
     * @return
     */
    public abstract boolean isTableSelected(TableSyntaxNode node);
}
