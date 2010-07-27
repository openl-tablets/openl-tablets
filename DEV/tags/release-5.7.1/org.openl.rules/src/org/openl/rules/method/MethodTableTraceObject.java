package org.openl.rules.method;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;

/**
 * Trace object for method table.
 * 
 * @author PUdalau
 */
public class MethodTableTraceObject extends ATableTracerNode {

    private static final String METHOD_TABLE_TYPE = "method";

    public MethodTableTraceObject(TableMethod method, Object[] params) {
        super(method, params);
    }

    public TableMethod getMethod() {
        return (TableMethod) getTraceObject();
    }

    @Override
    public String getUri() {
        return getMethod().getSourceUrl();
    }

    public List<IGridRegion> getGridRegions() {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        IGridTable tableBodyGrid = getMethod().getSyntaxNode().getTableBody().getGridTable();
        ICell cell;
        for (int row = 0; row < tableBodyGrid.getGridHeight(); row += cell.getHeight()) {
            cell = tableBodyGrid.getCell(0, row);
            regions.add(cell.getAbsoluteRegion());
        }
        return regions;
    }

    public String getType() {
        return METHOD_TABLE_TYPE;
    }

    public String getDisplayName(int mode) {
        return "Method table " + asString(getMethod(), mode);
    }

}
