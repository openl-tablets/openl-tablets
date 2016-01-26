package org.openl.rules.method.table;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ITable;

import java.util.ArrayList;
import java.util.List;

/**
 * Trace object for method table.
 *
 * @author PUdalau
 */
public class MethodTableTraceObject extends ATableTracerNode {

    public MethodTableTraceObject(TableMethod method, Object[] params) {
        super("method", "Method table", method, params);
    }

    public List<IGridRegion> getGridRegions() {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        ITable<?> tableBodyGrid = getTraceObject().getSyntaxNode().getTableBody().getSource();
        ICell cell;
        for (int row = 0; row < tableBodyGrid.getHeight(); row += cell.getHeight()) {
            cell = tableBodyGrid.getCell(0, row);
            regions.add(cell.getAbsoluteRegion());
        }
        return regions;
    }
}
