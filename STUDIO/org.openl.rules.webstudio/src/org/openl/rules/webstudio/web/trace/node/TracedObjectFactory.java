package org.openl.rules.webstudio.web.trace.node;

import java.util.HashMap;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.algorithm.WeightAlgorithmExecutor;
import org.openl.rules.dt.ActionInvoker;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

public class TracedObjectFactory {

    public static SimpleTracerObject getTracedObject(Object source,
            Invokable method,
            Object target,
            Object[] params,
            IRuntimeEnv env) {
        if (source instanceof OpenMethodDispatcher) {
            return OverloadedMethodChoiceTraceObject.create((OpenMethodDispatcher) source, params);
        } else if (source instanceof WeightAlgorithmExecutor) {
            return new WScoreTraceObject((ColumnMatch) target, params);
        } else if (source instanceof ColumnMatch) {
            ColumnMatch columnMatch = (ColumnMatch) source;
            if (columnMatch.getAlgorithmExecutor() instanceof WeightAlgorithmExecutor) {
                return new ATableTracerNode("wcmatch", "WCM", columnMatch, params);
            } else {
                return new ATableTracerNode("cmatch", "CM", columnMatch, params);
            }
        } else if (source instanceof Algorithm) {
            return new ATableTracerNode("tbasic", "Algorithm", (Algorithm) source, params);
        } else if (source instanceof AlgorithmSubroutineMethod) {
            return new ATableTracerNode("tbasicMethod", "Algorithm Method", (AlgorithmSubroutineMethod) source, null);
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
        } else if (method instanceof RuntimeOperation) {
            RuntimeOperation operation = (RuntimeOperation) method;
            String nameForDebug = operation.getNameForDebug();
            if (nameForDebug == null) {
                return null;
            }
            TBasicOperationTraceObject operationTracer = new TBasicOperationTraceObject(operation.getSourceCode(),
                nameForDebug);
            operationTracer.setFieldValues(
                (HashMap<String, Object>) ((TBasicContextHolderEnv) env).getTbasicTarget().getFieldValues());
            return operationTracer;
        }
        return null;
    }
}
