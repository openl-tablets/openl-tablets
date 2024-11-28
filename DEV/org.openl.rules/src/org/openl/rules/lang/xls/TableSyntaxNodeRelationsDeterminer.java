package org.openl.rules.lang.xls;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

interface TableSyntaxNodeRelationsDeterminer {
    boolean determine(TableSyntaxNode node, TableSyntaxNode dependsOnNode);
}
