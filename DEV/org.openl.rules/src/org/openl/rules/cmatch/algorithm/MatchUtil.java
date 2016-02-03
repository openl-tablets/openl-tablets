package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.table.IGridRegion;
import org.openl.vm.trace.Tracer;

/**
 * @author Yury Molchan
 */
class MatchUtil {

    static void trace(ColumnMatch columnMatch, int resultIndex, Object result) {
        if (Tracer.isTracerOn()) {
            TableRow tableRow = columnMatch.getRows().get(0);
            IGridRegion gridRegion = tableRow.get(MatchAlgorithmCompiler.VALUES)[resultIndex].getGridRegion();
            ResultTraceObject traceObject = new ResultTraceObject(columnMatch, gridRegion);
            traceObject.setResult(result);
            Tracer.put(traceObject);
        }
    }

    static void trace(ColumnMatch columnMatch, MatchNode node, int resultIndex, Object result) {
        if (Tracer.isTracerOn()) {
            int rowIndex = node.getRowIndex();
            TableRow row = columnMatch.getRows().get(rowIndex);
            IGridRegion gridRegion = row.get(MatchAlgorithmCompiler.VALUES)[resultIndex].getGridRegion();
            String operation = row.get(MatchAlgorithmCompiler.OPERATION)[0].getString();
            MatchTraceObject trObj = new MatchTraceObject(columnMatch, node.getCheckValues()[resultIndex],
                    operation, gridRegion);
            trObj.setResult(result);
            Tracer.put(trObj);
        }
    }
}
