/**
 * Created Apr 13, 2007
 */
package org.openl.rules.lang.xls.binding;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ABoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * @author snshor
 *
 */
public abstract class ATableBoundNode extends ABoundNode {

    public ATableBoundNode(TableSyntaxNode syntaxNode, IBoundNode[] children) {
        super(syntaxNode, children);
    }

    public final TableSyntaxNode getTableSyntaxNode() {
        return (TableSyntaxNode) getSyntaxNode();
    }
}
