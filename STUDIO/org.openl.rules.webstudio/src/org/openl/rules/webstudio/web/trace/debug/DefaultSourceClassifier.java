package org.openl.rules.webstudio.web.trace.debug;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.dt.ActionInvoker;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.vm.IRuntimeEnv;

/**
 * Production classifier that maps concrete OpenL execution types onto debugger frames and sub-steps.
 *
 * <p>Table frames are the rule methods that the user authors: decision tables, spreadsheets, method
 * tables, column matches, and TBasic algorithms. Cells, fired rules, conditions, and TBasic operations
 * are sub-steps inside the current frame. Dispatching and internal executors are transparent.
 */
public final class DefaultSourceClassifier implements SourceClassifier {

    @Override
    public @Nullable FrameDescriptor describeFrame(Object source) {
        FrameKind kind = frameKind(source);
        if (kind == null || !(source instanceof ExecutableRulesMethod method)) {
            return null;
        }
        return new FrameDescriptor(kind, method.getSourceUrl(), method.getName());
    }

    private static @Nullable FrameKind frameKind(Object source) {
        return switch (source) {
            case DecisionTable ignored -> FrameKind.DECISION_TABLE;
            case Spreadsheet ignored -> FrameKind.SPREADSHEET;
            case TableMethod ignored -> FrameKind.METHOD;
            case ColumnMatch ignored -> FrameKind.COLUMN_MATCH;
            case Algorithm ignored -> FrameKind.TBASIC;
            case AlgorithmSubroutineMethod ignored -> FrameKind.TBASIC_METHOD;
            case null, default -> null;
        };
    }

    @Override
    public @Nullable CurrentLocation describeSubStep(Object executor, IRuntimeEnv env, @Nullable Object frameSource) {
        return switch (executor) {
            case SpreadsheetCell cell -> cellLocation(cell, frameSource);
            case ActionInvoker invoker -> dtRuleLocation(invoker, frameSource);
            case RuntimeOperation operation -> operationLocation(operation);
            case null, default -> null;
        };
    }

    /** Labels a fired decision-table rule by its name (as the legacy trace did), e.g. {@code R10}. */
    private static CurrentLocation dtRuleLocation(ActionInvoker invoker, @Nullable Object frameSource) {
        int[] rules = invoker.getRules();
        if (frameSource instanceof IDecisionTable decisionTable) {
            String label = Arrays.stream(rules)
                    .mapToObj(decisionTable::getRuleName)
                    .collect(Collectors.joining(", "));
            return CurrentLocation.dtRule(label);
        }
        return CurrentLocation.dtRule("rule " + Arrays.toString(rules));
    }

    private static CurrentLocation cellLocation(SpreadsheetCell cell, @Nullable Object frameSource) {
        String label = frameSource instanceof Spreadsheet spreadsheet ? SpreadsheetCellNames.of(spreadsheet, cell) : null;
        return CurrentLocation.cell(cell.getRowIndex(), cell.getColumnIndex(), label);
    }

    private static @Nullable CurrentLocation operationLocation(RuntimeOperation operation) {
        String name = operation.getNameForDebug();
        return name == null ? null : CurrentLocation.operation(name);
    }

    @Override
    public @Nullable ConditionCheck describeCondition(String id, Object[] args) {
        if (!("index".equals(id) || "condition".equals(id)) || args.length < 3) {
            return null;
        }
        int[] rules;
        if (args[1] instanceof DecisionTableRuleNode node) {
            rules = node.getRules();
        } else if (args[1] instanceof Integer rule) {
            rules = new int[]{rule};
        } else {
            return null;
        }
        if (rules == null || rules.length == 0) {
            return null;
        }
        boolean successful = args[2] instanceof Boolean flag && flag;
        return new ConditionCheck(args[0], rules, successful);
    }
}
