package org.openl.rules.table;

import java.util.List;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.vm.ITracerObject;

public interface ITableTracerObject extends ITracerObject {
    /**
     * @return Regions to highlight in trace.
     */
    List<IGridRegion> getGridRegions();

    TableSyntaxNode getTableSyntaxNode();

    ITableTracerObject[] getTableTracers();
}
