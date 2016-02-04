package org.openl.rules.method;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.trace.SpreadsheetTraceObject;
import org.openl.rules.calc.trace.SpreadsheetTracerLeaf;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.algorithm.ColumnMatchTraceObject;
import org.openl.rules.cmatch.algorithm.IMatchAlgorithmExecutor;
import org.openl.rules.cmatch.algorithm.WColumnMatchTraceObject;
import org.openl.rules.cmatch.algorithm.WeightAlgorithmExecutor;
import org.openl.rules.dtx.IDecisionTable;
import org.openl.rules.dtx.trace.DecisionTableTraceObject;
import org.openl.rules.method.table.MethodTableTraceObject;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.runtime.debug.TBasicAlgorithmTraceObject;
import org.openl.types.Invokable;

public class TracedObjectFactory {

    public static ATableTracerNode getTracedObject(Invokable method, Object[] params) {
        if (method instanceof Algorithm) {
            return new TBasicAlgorithmTraceObject((Algorithm) method, params);
        } else if (method instanceof ColumnMatch) {
            ColumnMatch columnMatch = (ColumnMatch) method;
            IMatchAlgorithmExecutor algorithmExecutor = columnMatch.getAlgorithmExecutor();
            if (algorithmExecutor instanceof WeightAlgorithmExecutor) {
                return new WColumnMatchTraceObject(columnMatch, params);
            } else {
                return new ColumnMatchTraceObject(columnMatch, params);
            }
        } else if (method instanceof IDecisionTable) {
            return new DecisionTableTraceObject((IDecisionTable) method, params);
        } else if (method instanceof TableMethod) {
            return new MethodTableTraceObject((TableMethod) method, params);
        } else if (method instanceof Spreadsheet) {
            return new SpreadsheetTraceObject((Spreadsheet) method, params);
        } else if (method instanceof SpreadsheetCell) {
            return new SpreadsheetTracerLeaf((SpreadsheetCell) method);
        }
        throw new OpenlNotCheckedException(String.format("Can`t create traced object for %s.", method));
    }
}
