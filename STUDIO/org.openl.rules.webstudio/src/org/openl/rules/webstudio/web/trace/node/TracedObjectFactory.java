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
        if (source instanceof OpenMethodDispatcher dispatcher) {
            return OverloadedMethodChoiceTraceObject.create(dispatcher, params, env.getContext());
        } else if (source instanceof WeightAlgorithmExecutor) {
            return new WScoreTraceObject((ColumnMatch) target, params, env.getContext());
        } else if (source instanceof ColumnMatch columnMatch) {
            if (columnMatch.getAlgorithmExecutor() instanceof WeightAlgorithmExecutor) {
                return new ATableTracerNode("wcmatch",
                        "WCM",
                        columnMatch,
                        params,
                        env == null ? null : env.getContext());
            } else {
                return new ATableTracerNode("cmatch", "CM", columnMatch, params, env == null ? null : env.getContext());
            }
        } else if (source instanceof Algorithm algorithm) {
            return new ATableTracerNode("tbasic",
                    "Algorithm",
                    algorithm,
                    params,
                    env == null ? null : env.getContext());
        } else if (source instanceof AlgorithmSubroutineMethod subroutineMethod) {
            return new ATableTracerNode("tbasicMethod", "Algorithm Method", subroutineMethod, null);
        } else if (source instanceof DecisionTable table) {
            return new DecisionTableTraceObject(table, params, env == null ? null : env.getContext());
        } else if (source instanceof Spreadsheet spreadsheet) {
            return new ATableTracerNode("spreadsheet",
                    "SpreadSheet",
                    spreadsheet,
                    params,
                    env == null ? null : env.getContext());
        } else if (source instanceof TableMethod tableMethod) {
            return new MethodTableTraceObject(tableMethod, params, env == null ? null : env.getContext());
        } else if (method instanceof SpreadsheetCell cell) {
            return new SpreadsheetTracerLeaf(cell);
        } else if (method instanceof ActionInvoker invoker) {
            return new DTRuleTracerLeaf(invoker.getRules());
        } else if (method instanceof RuntimeOperation operation) {
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

    public static boolean supportLazyTrace(Object source) {
        return source instanceof OpenMethodDispatcher
                || source instanceof WeightAlgorithmExecutor
                || source instanceof ColumnMatch
                || source instanceof DecisionTable
                || source instanceof Spreadsheet
                || source instanceof TableMethod;
    }

    public static ITracerObject getTracedObject(Object source, String id, Object[] args) {
        ITracerObject trObj;
        if ("index".equals(id) || "condition".equals(id)) {
            trObj = DTRuleTraceObject.create(args);
        } else if ("match".equals(id)) {
            trObj = MatchTraceObject.create(args);
        } else if ("result".equals(id)) {
            trObj = ResultTraceObject.create(args);
        } else if (source instanceof SpreadsheetCell cell) {
            SpreadsheetTracerLeaf tr = new SpreadsheetTracerLeaf(cell);
            tr.setResult(args[0]);
            trObj = tr;
        } else if (source instanceof OpenMethodDispatcher dispatcher) {
            trObj = new DTRuleTracerLeaf(
                    new int[]{dispatcher.getCandidates().indexOf(args[0])});
        } else {
            trObj = null;
        }
        return trObj;
    }

}
