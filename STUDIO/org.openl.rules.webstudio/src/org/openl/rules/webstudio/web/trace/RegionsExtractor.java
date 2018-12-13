package org.openl.rules.webstudio.web.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ITable;
import org.openl.rules.webstudio.web.trace.node.*;

public class RegionsExtractor {
    static List<IGridRegion> getGridRegions(ITracerObject obj) {
        if (obj instanceof DTRuleTraceObject) {
            return getiGridRegions((DTRuleTraceObject) obj);
        } else if (obj instanceof DTRuleTracerLeaf) {
            return getiGridRegions((DTRuleTracerLeaf) obj);
        } else if (obj instanceof ResultTraceObject) {
            return getiGridRegions((ResultTraceObject) obj);
        } else if (obj instanceof MatchTraceObject) {
            return getiGridRegions((MatchTraceObject) obj);
        } else if (obj instanceof MethodTableTraceObject) {
            return getiGridRegions((MethodTableTraceObject) obj);
        } else if (obj instanceof OverloadedMethodChoiceTraceObject) {
            return getiGridRegions((OverloadedMethodChoiceTraceObject) obj);
        } else if (obj instanceof SpreadsheetTracerLeaf) {
            return getiGridRegions((SpreadsheetTracerLeaf) obj);
        } else if (obj instanceof TBasicOperationTraceObject) {
            return getiGridRegions((TBasicOperationTraceObject) obj);
        } else if (obj instanceof RefToTracerNodeObject) {
            return getGridRegions(((RefToTracerNodeObject) obj).getOriginalTracerNode());
        }
        return null;
    }

    private static List<IGridRegion> getiGridRegions(ResultTraceObject rto) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(rto.getGridRegion());
        return regions;
    }

    private static List<IGridRegion> getiGridRegions(TBasicOperationTraceObject tbo) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(tbo.getGridRegion());
        return regions;
    }

    private static List<IGridRegion> getiGridRegions(SpreadsheetTracerLeaf stl) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(stl.getSpreadsheetCell().getSourceCell().getAbsoluteRegion());
        return regions;
    }

    private static List<IGridRegion> getiGridRegions(OverloadedMethodChoiceTraceObject omc) {
        Iterator<ITracerObject> iterator = omc.getChildren().iterator();
        if (iterator.hasNext()) {
            List<IGridRegion> gridRegions = new ArrayList<IGridRegion>();
            ILogicalTable[] tables = ((DTRuleTracerLeaf) iterator.next()).getRuleTables();
            for (ILogicalTable table : tables){
                gridRegions.addAll(GridTableUtils.getGridRegions(table));
            }
            return gridRegions;
        } else {
            return Collections.emptyList();
        }
    }

    private static List<IGridRegion> getiGridRegions(MethodTableTraceObject mmto) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        ITable<?> tableBodyGrid = mmto.getTraceObject().getSyntaxNode().getTableBody().getSource();
        ICell cell;
        for (int row = 0; row < tableBodyGrid.getHeight(); row += cell.getHeight()) {
            cell = tableBodyGrid.getCell(0, row);
            regions.add(cell.getAbsoluteRegion());
        }
        return regions;
    }

    private static List<IGridRegion> getiGridRegions(MatchTraceObject mto) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(mto.getGridRegion());
        return regions;
    }

    private static List<IGridRegion> getiGridRegions(DTRuleTracerLeaf dtr) {
        List<IGridRegion> gridRegions = new ArrayList<IGridRegion>();
        ILogicalTable[] tables = dtr.getRuleTables();
        for (ILogicalTable table : tables){
            gridRegions.addAll(GridTableUtils.getGridRegions(table));
        }
        return gridRegions;
    }

    private static List<IGridRegion> getiGridRegions(DTRuleTraceObject dti) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();

        for (int rule : dti.getRules()) {
            ILogicalTable table = dti.getCondition().getValueCell(rule);
            regions.addAll(GridTableUtils.getGridRegions(table));
        }
        return regions;
    }
}
