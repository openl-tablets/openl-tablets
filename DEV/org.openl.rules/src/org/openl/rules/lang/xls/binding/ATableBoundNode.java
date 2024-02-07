/**
 * Created Apr 13, 2007
 */
package org.openl.rules.lang.xls.binding;

import org.openl.binding.impl.ABoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * @author snshor
 */
public abstract class ATableBoundNode extends ABoundNode {

    public ATableBoundNode(TableSyntaxNode syntaxNode) {
        super(syntaxNode);
    }

    public final TableSyntaxNode getTableSyntaxNode() {
        return (TableSyntaxNode) getSyntaxNode();
    }
}
