package org.openl.studio.projects.model.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.studio.projects.service.trace.CallNode;
import org.openl.studio.projects.service.trace.CurrentLocation;
import org.openl.studio.projects.service.trace.DebugCommand;
import org.openl.studio.projects.service.trace.DebugFrame;
import org.openl.studio.projects.service.trace.DebugListener;
import org.openl.studio.projects.service.trace.SpreadsheetCellNames;
import org.openl.studio.projects.service.trace.TraceDebugger;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * Validates that a spreadsheet step's inputs are read back in the formula's own terms: sibling steps,
 * the table's parameters, fields of a parameter opened into the table's scope, and module constants.
 */
class TraceStepInputsTest {

    private static final String SRC = "test/rules/EPBDS-16292/bankRating.xlsx";

    private static TraceDebugger debugger;
    private static DebugFrame frame;
    private static ClassLoader classLoader;
    private static IOpenClass module;
    private static IOpenMethod test;

    private static TraceDebugMapper mapper() {
        var objectMapper = new ObjectMapper();
        var schemaGenerator = new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper);
        return new TraceDebugMapper(objectMapper, schemaGenerator, new TraceParameterRegistry());
    }

    @BeforeAll
    static void runToBankRatingCalculationExit() {
        CompiledOpenClass compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        module = compiled.getOpenClass();
        test = module.getMethod("BankRatingTest", IOpenClass.EMPTY);
        assertNotNull(test, "BankRatingTest must compile");
        classLoader = compiled.getClassLoader();

        debugger = new TraceDebugger(DebugListener.NOOP);
        // Break at the calculation's entry, then run the frame to its own exit: every step is executed
        // there, so any step's inputs can be read back.
        debugger.setBreakpoints(Set.of("BankRatingCalculation"));
        debugger.start("step-inputs-test", classLoader, false, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            test.invoke(module.newInstance(env), new Object[0], env);
        });
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(20_000));
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_OUT, 20_000));

        List<DebugFrame> stack = debugger.stack();
        frame = stack.get(stack.size() - 1);
        assertEquals("BankRatingCalculation", frame.getName());
        assertTrue(frame.isCompleted(), "the frame paused at its own exit");
    }

    @AfterAll
    static void terminate() {
        debugger.terminate(20_000);
    }

    private static List<String> names(List<ParameterValue> inputs) {
        return inputs.stream().map(ParameterValue::name).toList();
    }

    @Test
    void formulaOverSiblingStepAndConstantReadsBoth() {
        // Limit = $LimitIndex * MaxLimit: a sibling step and a Constants-table value.
        List<ParameterValue> inputs = mapper().freezeStepInputs(frame, "R9C1", classLoader, false);

        assertEquals(List.of("$LimitIndex", "MaxLimit"), names(inputs));
        assertNotNull(inputs.get(0).value(), "the executed sibling step carries its recorded value");
        assertEquals(5000, inputs.get(1).value().asInt());
    }

    @Test
    void fieldOfAParameterOpenedIntoTheTableScopeIsResolvedFromTheParameterValue() {
        // CheckCurrentFinancialData = SetNonZeroValues(currentFinancialData), a field of the bank parameter.
        List<ParameterValue> inputs = mapper().freezeStepInputs(frame, "R0C1", classLoader, false);

        assertEquals(List.of("currentFinancialData"), names(inputs));
        assertEquals("FinancialData", inputs.get(0).description());
    }

    @Test
    void tableParameterUsedByNameIsListedWithItsValue() {
        // BankQualitativeIndexCalculation = BankQualitativeIndexCalculation(bank).
        List<ParameterValue> inputs = mapper().freezeStepInputs(frame, "R4C1", classLoader, false);

        assertEquals(List.of("bank"), names(inputs));
    }

    @Test
    void fieldPickedFromAnotherStepResultIsTheDottedPreciseInput() {
        // BankRating = round($BalanceQualityIndexCalculation.$Value$BalanceQualityIndex * ..., 2): the
        // formula reads ONE field of a sibling step's result — the input is that field with its value,
        // not the whole result object.
        List<ParameterValue> inputs = mapper().freezeStepInputs(frame, "R6C1", classLoader, false);

        List<String> names = names(inputs);
        assertEquals(List.of(
                "$BalanceQualityIndexCalculation.$Value$BalanceQualityIndex",
                "$BalanceDynamicIndexCalculation",
                "$BankQualitativeIndexCalculation",
                "$IsAdequateNormativeIndexCalculation"), names);
        assertEquals("Double", inputs.get(0).description());
        assertNotNull(inputs.get(0).value(), "the field is read off the recorded result");
    }

    @Test
    void plainValueCellsAppearInTheExecutedTreeAsConstantSteps() {
        // The executed tree lists static cells too, flagged constant, sorted into their grid position —
        // so the trace tree shows the whole table like the grid does.
        TraceDebugger local = new TraceDebugger(DebugListener.NOOP);
        local.start("tree-static-test", classLoader, false, true, false, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(local.tracer());
            test.invoke(module.newInstance(env), new Object[0], env);
        });
        try {
            assertEquals(DebugStatus.COMPLETED, local.awaitInitialHalt(20_000));
            CallNode tree = local.completedTree();
            assertNotNull(tree);
            assertEquals("BankRatingCalculation", tree.name());

            var statics = tree.steps().stream().filter(CallNode.Step::constant).toList();
            assertTrue(statics.stream().anyMatch(s -> "$Description$BankRatingGroup".equals(s.label())),
                    "description cells are constant steps: "
                            + statics.stream().map(CallNode.Step::label).toList());
            // Grid ordering: the description cell of a row sits right before that row's value cell.
            List<String> refs = tree.steps().stream().map(CallNode.Step::ref).toList();
            assertTrue(refs.indexOf("R7C0") == refs.indexOf("R7C1") - 1,
                    "steps are ordered by grid position: " + refs);
        } finally {
            local.terminate(20_000);
        }
    }

    @Test
    void plainValueCellsAreListedWithTheirStaticContent() {
        // The Description column holds plain text cells, not formulas. They never execute, but the grid
        // must still show them — the legacy trace listed every cell of the table, not only the steps.
        var variables = mapper().freezeVariables(frame, classLoader, false);

        var description = variables.steps().stream()
                .filter(step -> step.value() != null && step.value().value() != null
                        && step.value().value().asText().contains("Calculate Bank Rating Group"))
                .findFirst();
        assertTrue(description.isPresent(), "the static description text is a listed cell: "
                + variables.steps().stream().map(StepValueView::label).toList());
    }

    @Test
    void decisionTableExpandsIntoConditionsAndReturnedRule() {
        // A DT node in the executed tree breaks down like the legacy detailed trace: one row per evaluated
        // condition (matched or not), then the returned rule — instead of a bare fired-rule step.
        TraceDebugger local = new TraceDebugger(DebugListener.NOOP);
        local.start("tree-dt-test", classLoader, false, true, false, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(local.tracer());
            test.invoke(module.newInstance(env), new Object[0], env);
        });
        try {
            assertEquals(DebugStatus.COMPLETED, local.awaitInitialHalt(20_000));
            CallNode dt = findNode(local.completedTree(), n -> n.kind() == FrameKind.DECISION_TABLE);
            assertNotNull(dt, "the run calls the NonZeroValues decision table");
            assertEquals("NonZeroValues", dt.name());

            List<CallNode.Step> conditions = dt.steps().stream()
                    .filter(s -> s.decision() == DecisionRow.MATCHED || s.decision() == DecisionRow.UNMATCHED)
                    .toList();
            assertFalse(conditions.isEmpty(), "conditions are listed: "
                    + dt.steps().stream().map(CallNode.Step::label).toList());
            assertTrue(conditions.stream().allMatch(s -> s.label() != null && s.label().startsWith("Condition: ")),
                    "condition rows read like the legacy trace");
            assertTrue(conditions.stream().anyMatch(s -> s.decision() == DecisionRow.MATCHED)
                            && conditions.stream().anyMatch(s -> s.decision() == DecisionRow.UNMATCHED),
                    "both a matched and an unmatched condition are shown");

            List<CallNode.Step> returned = dt.steps().stream()
                    .filter(s -> s.decision() == DecisionRow.RETURNED).toList();
            assertEquals(1, returned.size(), "exactly one returned-rule row");
            assertTrue(returned.get(0).label() != null && returned.get(0).label().startsWith("Returned rule: ["),
                    "the returned rule keeps the legacy label: " + returned.get(0).label());
        } finally {
            local.terminate(20_000);
        }
    }

    @Test
    void detailedTitlesCarryTheSignatureResultAndCellValues() {
        // The business view builds the classic detailed titles: a table node reads as its kind, signature and
        // result; a spreadsheet cell as its value. (The advanced debugger passes false and keeps plain names.)
        TraceDebugger local = new TraceDebugger(DebugListener.NOOP);
        local.start("tree-detailed-test", classLoader, false, true, true, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(local.tracer());
            test.invoke(module.newInstance(env), new Object[0], env);
        });
        try {
            assertEquals(DebugStatus.COMPLETED, local.awaitInitialHalt(20_000));
            CallNode tree = local.completedTree();
            assertNotNull(tree);
            // The root spreadsheet node reads as its kind prefix and signature, not a bare name.
            assertTrue(tree.name().startsWith("SpreadSheet ") && tree.name().contains("BankRatingCalculation("),
                    "the root carries its kind prefix and signature: " + tree.name());

            // A decision table reads with the "DT" prefix, its signature, and its result after "=".
            CallNode dt = findNode(tree, n -> n.kind() == FrameKind.DECISION_TABLE);
            assertNotNull(dt);
            assertTrue(dt.name().startsWith("DT ") && dt.name().contains("NonZeroValues(") && dt.name().contains(" = "),
                    "the DT node reads like the legacy detailed trace: " + dt.name());

            // A spreadsheet cell carries its computed value after "=".
            assertTrue(tree.steps().stream().anyMatch(s -> s.label() != null && s.label().contains(" = ")),
                    "cells carry their value: " + tree.steps().stream().map(CallNode.Step::label).toList());
        } finally {
            local.terminate(20_000);
        }
    }

    /** Depth-first search of the executed tree for the first node matching the predicate. */
    private static @org.jspecify.annotations.Nullable CallNode findNode(
            @org.jspecify.annotations.Nullable CallNode node, java.util.function.Predicate<CallNode> match) {
        if (node == null) {
            return null;
        }
        if (match.test(node)) {
            return node;
        }
        for (CallNode.Step step : node.steps()) {
            for (CallNode child : step.children()) {
                CallNode found = findNode(child, match);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void unknownStepAndNonSpreadsheetInputsAreEmpty() {
        assertTrue(mapper().freezeStepInputs(frame, "R99C9", classLoader, false).isEmpty());
    }

    @Test
    void cellRangeReferenceExpandsIntoTheStepsItSpans() {
        // BankQualitativeIndex = min($LossesInThisYearScore:$TotalBalanceScore): a range over six steps.
        TraceDebugger local = new TraceDebugger(DebugListener.NOOP);
        local.setBreakpoints(Set.of("BankQualitativeIndexCalculation"));
        local.start("range-inputs-test", classLoader, false, () -> {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(local.tracer());
            test.invoke(module.newInstance(env), new Object[0], env);
        });
        try {
            assertEquals(DebugStatus.SUSPENDED, local.awaitInitialHalt(20_000));
            assertEquals(DebugStatus.SUSPENDED, local.command(DebugCommand.STEP_OUT, 20_000));
            List<DebugFrame> stack = local.stack();
            DebugFrame owner = stack.get(stack.size() - 1);
            assertEquals("BankQualitativeIndexCalculation", owner.getName());

            List<ParameterValue> inputs = mapper().freezeStepInputs(owner,
                    stepRefByLabel(owner, "$Value$BankQualitativeIndex"), classLoader, false);

            List<String> names = names(inputs);
            assertEquals(6, names.size(), "every step the range spans is an input: " + names);
            assertEquals("$Value$LossesInThisYearScore", names.get(0));
            assertEquals("$Value$TotalBalanceScore", names.get(names.size() - 1));
            assertTrue(inputs.stream().allMatch(input -> input.value() != null),
                    "each spanned step carries its recorded value");
        } finally {
            local.terminate(20_000);
        }
    }

    /** The RnCm reference of the step with the given OpenL cell name. */
    private static String stepRefByLabel(DebugFrame owner, String label) {
        Spreadsheet spreadsheet = (Spreadsheet) owner.getSource();
        for (SpreadsheetCell[] row : spreadsheet.getCells()) {
            for (SpreadsheetCell cell : row) {
                if (cell != null && label.equals(SpreadsheetCellNames.of(spreadsheet, cell))) {
                    return CurrentLocation.cellRef(cell.getRowIndex(), cell.getColumnIndex());
                }
            }
        }
        throw new IllegalStateException("no step labeled " + label);
    }
}
