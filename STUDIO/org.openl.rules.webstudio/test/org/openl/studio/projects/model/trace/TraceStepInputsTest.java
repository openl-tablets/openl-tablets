package org.openl.studio.projects.model.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
