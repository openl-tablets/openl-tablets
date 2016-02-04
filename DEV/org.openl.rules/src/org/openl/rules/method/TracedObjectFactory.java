package org.openl.rules.method;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.calc.SpreadsheetInvoker;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.trace.SpreadsheetTraceObject;
import org.openl.rules.calc.trace.SpreadsheetTracerLeaf;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.ColumnMatchInvoker;
import org.openl.rules.cmatch.algorithm.ColumnMatchTraceObject;
import org.openl.rules.cmatch.algorithm.IMatchAlgorithmExecutor;
import org.openl.rules.cmatch.algorithm.WColumnMatchTraceObject;
import org.openl.rules.cmatch.algorithm.WeightAlgorithmExecutor;
import org.openl.rules.dt.DecisionTableInvoker;
import org.openl.rules.dtx.trace.DecisionTableTraceObject;
import org.openl.rules.method.table.MethodTableInvoker;
import org.openl.rules.method.table.MethodTableTraceObject;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.tbasic.AlgorithmInvoker;
import org.openl.rules.tbasic.runtime.debug.TBasicAlgorithmTraceObject;
import org.openl.types.Invokable;

public class TracedObjectFactory {

    public static ATableTracerNode getTracedObject(Invokable method, Object[] params) {
        if (method instanceof AlgorithmInvoker) {
            return new TBasicAlgorithmTraceObject(((AlgorithmInvoker) method).getInvokableMethod(), params);
        } else if (method instanceof ColumnMatchInvoker) {
            ColumnMatch columnMatch = ((ColumnMatchInvoker) method).getInvokableMethod();
            IMatchAlgorithmExecutor algorithmExecutor = columnMatch.getAlgorithmExecutor();
            if (algorithmExecutor instanceof WeightAlgorithmExecutor) {
                return new WColumnMatchTraceObject(columnMatch, params);
            } else {
                return new ColumnMatchTraceObject(columnMatch, params);
            }
        } else if (method instanceof DecisionTableInvoker) {
            return new DecisionTableTraceObject(((DecisionTableInvoker) method).getInvokableMethod(), params);
        } else if (method instanceof MethodTableInvoker) {
            return new MethodTableTraceObject(((MethodTableInvoker) method).getInvokableMethod(), params);
        } else if (method instanceof SpreadsheetInvoker) {
            return new SpreadsheetTraceObject(((SpreadsheetInvoker) method).getInvokableMethod(), params);
        } else if (method instanceof SpreadsheetCell) {
            return new SpreadsheetTracerLeaf((SpreadsheetCell) method);
        }
        throw new OpenlNotCheckedException(String.format("Can`t create traced object for %s.", method));
    }
}
