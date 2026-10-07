package org.openl.studio.projects.model.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.studio.projects.model.ParameterValue;
import org.openl.studio.projects.service.trace.DebugCommand;
import org.openl.studio.projects.service.trace.DebugListener;
import org.openl.studio.projects.service.trace.TraceDebugger;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;

/**
 * A step formula that reads a field off an array of spreadsheet results — {@code sum($Plans.$Lives)} where
 * {@code $Plans} is a {@code SpreadsheetResultPlanCost[]} — must present the field access {@code $Plans.$Lives}
 * as an input, with the field read off every element (OpenL's matrix syntax), not drop it because the receiver
 * is an array whose type differs from the field's element declaring class.
 *
 * <p>Unlike a scalar result reached only through its field, the whole array stays listed too: it is commonly
 * also passed whole to the same call.
 *
 * <p>Fixture {@code arrayResultFieldProject.xlsx}: {@code PlanCost(Integer x){Lives=x}}, and
 * {@code Caller(Integer[] xs)} whose {@code Plans = PlanCost(xs)} maps to a {@code SpreadsheetResultPlanCost[]};
 * {@code CaseTwo = sum($Plans.$Lives)} reads the field only, {@code CaseOne = length($Plans) + sum($Plans.$Lives)}
 * reads the array both whole and via the field.
 */
class ArrayResultFieldInputTest {

    private static final String SRC = "test/rules/trace-debug/arrayResultFieldProject.xlsx";

    private record Frozen(List<ParameterValue> inputs, TraceParameterRegistry registry) {
        List<String> names() {
            return inputs.stream().map(ParameterValue::name).toList();
        }
    }

    @Test
    @DisplayName("A field read off an array of results lists the field access, its value read per element")
    void arrayResultFieldAccessIsListedAsAMatrix() {
        var frozen = freeze("R1C0"); // CaseTwo = sum($Plans.$Lives)

        assertTrue(frozen.names().contains("$Plans.$Lives"),
                "the array field access is listed, not dropped: " + frozen.names());
        assertTrue(frozen.names().contains("$Plans"), "the whole array stays listed too: " + frozen.names());

        var access = frozen.inputs().stream()
                .filter(p -> p.name().equals("$Plans.$Lives")).findFirst().orElseThrow();
        assertEquals("Integer[]", access.description());
        // The value is the field read off each element — the matrix [2, 3, 5], resolved via the lazy registry.
        var value = frozen.registry().get(access.parameterId()).getValue();
        assertEquals(List.of(2, 3, 5), Arrays.asList((Integer[]) value));
    }

    @Test
    @DisplayName("An array passed both whole and via a field lists both, not just the whole array")
    void arrayUsedWholeAndViaFieldListsBoth() {
        var names = freeze("R2C0").names(); // CaseOne = length($Plans) + sum($Plans.$Lives)

        assertTrue(names.contains("$Plans"), names.toString());
        assertTrue(names.contains("$Plans.$Lives"), names.toString());
    }

    private static Frozen freeze(String ref) {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var module = compiled.getOpenClass();
        var caller = module.getMethods().stream()
                .filter(m -> m.getName().equals("Caller")).findFirst().orElseThrow();
        var classLoader = compiled.getClassLoader();

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.setBreakpoints(Set.of("Caller"));
        debugger.start("array-result", classLoader, false, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            caller.invoke(module.newInstance(env), new Object[]{ new Integer[]{ 2, 3, 5 } }, env);
        });
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(20_000));
        // Run the frame to its own exit so its steps have executed and their inputs can be read back.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_OUT, 20_000));
        var stack = debugger.stack();
        var frame = stack.get(stack.size() - 1);
        assertEquals("Caller", frame.getName());

        var objectMapper = new ObjectMapper();
        var registry = new TraceParameterRegistry();
        var mapper = new TraceDebugMapper(objectMapper,
                new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper), registry);
        var inputs = mapper.freezeStepInputs(frame, ref, classLoader, false).inputs();
        debugger.terminate(20_000);
        return new Frozen(inputs, registry);
    }
}
