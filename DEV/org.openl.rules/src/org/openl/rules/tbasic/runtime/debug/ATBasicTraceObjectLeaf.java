package org.openl.rules.tbasic.runtime.debug;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerLeaf;
import org.openl.rules.table.ATableTracerNode;
import org.openl.vm.trace.ITracerObject;

public abstract class ATBasicTraceObjectLeaf extends ATableTracerLeaf {

    public ATBasicTraceObjectLeaf() {
        super();
    }

    public ATBasicTraceObjectLeaf(Object tracedObject) {
        super(tracedObject);
    }

    public TableSyntaxNode getTableSyntaxNode() {
        TableSyntaxNode tsn = null;

        ITracerObject parentTraceObject = getParent();
        while (parentTraceObject != null) {
            if (parentTraceObject instanceof ATableTracerNode) {
                tsn = ((ATableTracerNode) parentTraceObject).getTableSyntaxNode();
                break;
            }
            parentTraceObject = parentTraceObject.getParent();
        }

        return tsn;
    }

}