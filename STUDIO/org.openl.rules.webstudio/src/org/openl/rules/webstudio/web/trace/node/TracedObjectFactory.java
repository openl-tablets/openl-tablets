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
            return OverloadedMethodChoiceTraceObject.create((OpenMethodDispatcher) source, params, env.getContext());
        } else if (source instanceof WeightAlgorithmExecutor) {
            return new WScoreTraceObject((ColumnMatch) target, params, env.getContext());
        } else if (source instanceof ColumnMatch) {
            ColumnMatch columnMatch = (ColumnMatch) source;
            if (columnMatch.getAlgorithmExecutor() instanceof WeightAlgorithmExecutor) {
                return new ATableTracerNode("wcmatch", "WCM", columnMatch, params, env == null ? null : env.getContext());
            } else {
                return new ATableTracerNode("cmatch", "CM", columnMatch, params, env == null ? null : env.getContext());
            }
        } else if (source instanceof Algorithm) {
            return new ATableTracerNode("tbasic", "Algorithm", (Algorithm) source, params, env == null ? null : env.getContext());
        } else if (source instanceof AlgorithmSubroutineMethod) {
            return new ATableTracerNode("tbasicMethod", "Algorithm Method", (AlgorithmSubroutineMethod) source, null);
        } else if (source instanceof DecisionTable) {
            return new DecisionTableTraceObject((DecisionTable) source, params, env == null ? null : env.getContext());
        } else if (source instanceof Spreadsheet) {
            return new ATableTracerNode("spreadsheet", "SpreadSheet", (Spreadsheet) source, params, env == null ? null : env.getContext());
        } else if (source instanceof TableMethod) {
            return new MethodTableTraceObject((TableMethod) source, params, env == null ? null : env.getContext());
        } else if (method instanceof SpreadsheetCell) {
            return new SpreadsheetTracerLeaf((SpreadsheetCell) method);
        } else if (method instanceof ActionInvoker) {
            return new DTRuleTracerLeaf(((ActionInvoker) method).getRules());
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

    public static ITracerObject getTracedObject(Object source, String id, Object[] args) {
        ITracerObject trObj;
        if ("index".equals(id) || "condition".equals(id)) {
            trObj = DTRuleTraceObject.create(args);
        } else if ("match".equals(id)) {
            trObj = MatchTraceObject.create(args);
        } else if ("result".equals(id)) {
            trObj = ResultTraceObject.create(args);
        } else if (source instanceof SpreadsheetCell) {
            SpreadsheetTracerLeaf tr = new SpreadsheetTracerLeaf((SpreadsheetCell) source);
            tr.setResult(args[0]);
            trObj = tr;
        } else if (source instanceof OpenMethodDispatcher) {
            trObj = new DTRuleTracerLeaf(new int[]{((OpenMethodDispatcher) source).getCandidates().indexOf(args[0])});
        } else {
            trObj = null;
        }
        return trObj;
    }

}
