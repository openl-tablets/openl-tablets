package org.openl.rules.webstudio.web.trace.node;

import java.util.HashMap;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.algorithm.WeightAlgorithmExecutor;
import org.openl.rules.dt.ActionInvoker;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDecisionTable;
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

    public static SimpleTracerObject deepCopy(SimpleTracerObject source) {
        SimpleTracerObject resultNode = makeCopy(source);
        copyChildren(resultNode, source.getChildren());
        return resultNode;
    }

    private static void copyChildren(ITracerObject parentNode, Iterable<ITracerObject> children) {
        for (ITracerObject childrenNode : children) {
            SimpleTracerObject newChildrenNode = makeCopy((SimpleTracerObject) childrenNode);

            parentNode.addChild(newChildrenNode);
            copyChildren(newChildrenNode, childrenNode.getChildren());
        }
    }

    private static SimpleTracerObject makeCopy(SimpleTracerObject source) {
        SimpleTracerObject result = null;
        Class<?> sourceClass = source.getClass();
        if (sourceClass == SpreadsheetTracerLeaf.class) {
            SpreadsheetTracerLeaf sourceNode = (SpreadsheetTracerLeaf) source;
            result = new SpreadsheetTracerLeaf(sourceNode.getSpreadsheetCell());
        } else if (sourceClass == OverloadedMethodChoiceTraceObject.class) {
            OverloadedMethodChoiceTraceObject sourceNode = (OverloadedMethodChoiceTraceObject) source;
            result = new OverloadedMethodChoiceTraceObject(sourceNode.getTraceObject(),
                    sourceNode.getParameters(), sourceNode.getContext(), sourceNode.getMethodCandidates());
        } else if (sourceClass == WScoreTraceObject.class) {
            WScoreTraceObject sourceNode = (WScoreTraceObject) source;
            result = new WScoreTraceObject((ColumnMatch) sourceNode.getTraceObject(),
                    sourceNode.getParameters(), sourceNode.getContext());
        } else if (sourceClass == DecisionTableTraceObject.class) {
            DecisionTableTraceObject sourceNode = (DecisionTableTraceObject) source;
            result = new DecisionTableTraceObject((IDecisionTable) sourceNode.getTraceObject(),
                    sourceNode.getParameters(), sourceNode.getContext());
        } else if (sourceClass == MethodTableTraceObject.class) {
            MethodTableTraceObject sourceNode = (MethodTableTraceObject) source;
            result = new MethodTableTraceObject((TableMethod) sourceNode.getTraceObject(),
                    sourceNode.getParameters(), sourceNode.getContext());
        } else if (sourceClass == DTRuleTracerLeaf.class) {
            DTRuleTracerLeaf sourceNode = (DTRuleTracerLeaf) source;
            result = new DTRuleTracerLeaf(sourceNode.getRuleIndexes());
        } else if (sourceClass == TBasicOperationTraceObject.class) {
            TBasicOperationTraceObject sourceNode = (TBasicOperationTraceObject) source;
            result = new TBasicOperationTraceObject(sourceNode.getNameForDebug(), sourceNode.getGridRegion(),
                    sourceNode.getOperationName(), sourceNode.getOperationRow(), sourceNode.getUri(),
                    sourceNode.getFieldValues());
        } else if (sourceClass == DTRuleTraceObject.class) {
            DTRuleTraceObject sourceNode = (DTRuleTraceObject) source;
            result = new DTRuleTraceObject(sourceNode.getCondition(), sourceNode.getRules(), sourceNode.isSuccessful(),
                    sourceNode.isIndexed());
        } else if (sourceClass == MatchTraceObject.class){
            MatchTraceObject sourceNode = (MatchTraceObject) source;
            result = new MatchTraceObject((ColumnMatch) sourceNode.getTraceObject(), sourceNode.getCheckValue(),
                    sourceNode.getOperation(), sourceNode.getGridRegion());
        } else if (sourceClass == ResultTraceObject.class) {
            ResultTraceObject sourceNode = (ResultTraceObject) source;
            result = new ResultTraceObject((ColumnMatch) sourceNode.getTraceObject(), sourceNode.getGridRegion());
        } else if (sourceClass == ATableTracerNode.class) {
            ATableTracerNode sourceNode = (ATableTracerNode) source;
            result = new ATableTracerNode(sourceNode.getType(), sourceNode.getPrefix(), sourceNode.getTraceObject(),
                    sourceNode.getParameters(), sourceNode.getContext());
        }

        if (result == null) {
            //should never happen
            throw new IllegalStateException("Could not clone tracer object instance of " + source.getClass().getSimpleName());
        }

        result.setResult(source.getResult());
        result.setError(source.getError());

        return result;
    }
}
