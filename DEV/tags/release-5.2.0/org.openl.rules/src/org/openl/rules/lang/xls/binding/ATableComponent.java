package org.openl.rules.lang.xls.binding;

import org.openl.binding.IBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public abstract class ATableComponent extends ATableBoundNode {

    public ATableComponent(TableSyntaxNode syntaxNode, IBoundNode[] children) {
        super(syntaxNode, children);
    }
}
