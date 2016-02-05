package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.algorithm.WeightAlgorithmExecutor;
import org.openl.rules.dt.ActionInvoker;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.Invokable;

public class TracedObjectFactory {

    public static ATableTracerNode getTracedObject(Object source, Invokable method, Object target, Object[] params) {
        if (source instanceof OpenMethodDispatcher) {
            return OverloadedMethodChoiceTraceObject.create((OpenMethodDispatcher) source, params);
        } else if (source instanceof WeightAlgorithmExecutor) {
            return new WScoreTraceObject((ColumnMatch) target, params);
        } else if (source instanceof ColumnMatch) {
            ColumnMatch columnMatch = (ColumnMatch) source;
            if (columnMatch.getAlgorithmExecutor() instanceof WeightAlgorithmExecutor) {
                return new WColumnMatchTraceObject(columnMatch, params);
            } else {
                return new ColumnMatchTraceObject(columnMatch, params);
            }
        } else if (source instanceof Algorithm) {
            return new TBasicAlgorithmTraceObject((Algorithm) source, params);
        } else if (source instanceof AlgorithmSubroutineMethod) {
            return new TBasicMethodTraceObject((AlgorithmSubroutineMethod) source);
        } else if (source instanceof DecisionTable) {
            return new DecisionTableTraceObject((DecisionTable) source, params);
        } else if (source instanceof Spreadsheet) {
            return new SpreadsheetTraceObject((Spreadsheet) source, params);
        } else if (source instanceof TableMethod) {
            return new MethodTableTraceObject((TableMethod) source, params);
        } else if (method instanceof SpreadsheetCell) {
            return new SpreadsheetTracerLeaf((SpreadsheetCell) method);
        } else if (method instanceof ActionInvoker) {
            return new DTRuleTracerLeaf(((ActionInvoker) method).getRule());
        }
        return null;
    }
}
