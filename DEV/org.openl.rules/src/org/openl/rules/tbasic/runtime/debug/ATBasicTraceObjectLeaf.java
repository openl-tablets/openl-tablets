package org.openl.rules.tbasic.runtime.debug;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerLeaf;

abstract class ATBasicTraceObjectLeaf extends ATableTracerLeaf {
    protected ATBasicTraceObjectLeaf(String type) {
        super(type);
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return null;
    }

}