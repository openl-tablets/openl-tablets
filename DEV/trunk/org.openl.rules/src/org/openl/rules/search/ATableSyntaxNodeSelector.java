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
public abstract class ATableSyntaxNodeSelector extends ASelector {

    public boolean select(Object obj) {
        return selectTable((TableSyntaxNode) obj);
    }

    /**
     * @param node
     * @return
     */
    public abstract boolean selectTable(TableSyntaxNode node);
}
