package org.openl.rules.webstudio.web.trace;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.calc.trace.SpreadsheetTracerLeaf;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.cmatch.algorithm.MatchAlgorithmCompiler;
import org.openl.rules.cmatch.algorithm.MatchTraceObject;
import org.openl.rules.dtx.IDecisionTable;
import org.openl.rules.dtx.trace.DTConditionTraceObject;
import org.openl.rules.dtx.trace.DTIndexedTraceObject;
import org.openl.rules.dtx.trace.DTRuleTracerLeaf;
import org.openl.rules.method.table.MethodTableTraceObject;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ITable;
import org.openl.rules.tbasic.runtime.debug.TBasicOperationTraceObject;
import org.openl.rules.types.impl.OverloadedMethodChoiceTraceObject;
import org.openl.types.IOpenMethod;
import org.openl.vm.trace.ITracerObject;

public class RegionsExtractor {
    static List<IGridRegion> getGridRegions(ITracerObject obj) {
        if (obj instanceof DTIndexedTraceObject) {
            return getiGridRegions((DTIndexedTraceObject) obj);
        } else if (obj instanceof DTConditionTraceObject) {
            return getiGridRegions((DTConditionTraceObject) obj);
        } else if (obj instanceof DTRuleTracerLeaf) {
            return getiGridRegions((DTRuleTracerLeaf) obj);
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
        }
        return null;
    }

    private static List<IGridRegion> getiGridRegions(TBasicOperationTraceObject tbo) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(tbo.getSourceCode().getGridRegion());
        return regions;
    }

    private static List<IGridRegion> getiGridRegions(SpreadsheetTracerLeaf stl) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(stl.getSpreadsheetCell().getSourceCell().getAbsoluteRegion());
        return regions;
    }

    private static List<IGridRegion> getiGridRegions(OverloadedMethodChoiceTraceObject omc) {
        IOpenMethod method = (IOpenMethod) omc.getResult();
        int methodIndex = omc.getMethodCandidates().indexOf(method);

        ILogicalTable table = ((IDecisionTable) omc.getTraceObject()).getRuleTable(methodIndex);
        return GridTableUtils.getGridRegions(table);
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
        TableRow row = mto.getRow();
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(row.get(MatchAlgorithmCompiler.VALUES)[mto.getResultIndex()].getGridRegion());
        return regions;
    }

    private static List<IGridRegion> getiGridRegions(DTRuleTracerLeaf dtr) {
        ILogicalTable table = dtr.getRuleTable();
        return GridTableUtils.getGridRegions(table);
    }

    private static List<IGridRegion> getiGridRegions(DTIndexedTraceObject dti) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();

        for (int rule : dti.getLinkedRule().getRules()) {
            ILogicalTable table = dti.getCondition().getValueCell(rule);
            regions.addAll(GridTableUtils.getGridRegions(table));
        }
        return regions;
    }

    private static List<IGridRegion> getiGridRegions(DTConditionTraceObject dtc) {
        ILogicalTable table = dtc.getCondition().getValueCell(dtc.getRuleIndex());
        return GridTableUtils.getGridRegions(table);
    }

}