package org.openl.rules.table;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.vm.ITracerObject;

public interface ITableTracerObject extends ITracerObject {
    IGridRegion getGridRegion();

    TableSyntaxNode getTableSyntaxNode();

    ITableTracerObject[] getTableTracers();
}
