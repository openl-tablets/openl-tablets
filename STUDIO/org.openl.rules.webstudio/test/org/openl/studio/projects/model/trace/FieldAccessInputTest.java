package org.openl.studio.projects.model.trace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.openl.types.IOpenClass;

/**
 * A step formula that reads a field off a parameter — {@code CountCensus(policy.census)} — must present the
 * field access {@code policy.census} as the input, with the field's own value and type, not the whole
 * {@code policy} parameter it was only reached through.
 *
 * <p>Fixture {@code fieldAccessProject.xlsx}: datatype {@code Policy{census}}, and {@code Caller(Policy policy)}
 * whose {@code Count} step calls {@code CountCensus(policy.census)}.
 */
class FieldAccessInputTest {

    private static final String SRC = "test/rules/trace-debug/fieldAccessProject.xlsx";

    @Test
    @DisplayName("A field read off a parameter lists the dotted field, not the whole parameter")
    void parameterFieldAccessIsTheDottedInput() {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var module = compiled.getOpenClass();
        var policyType = module.findType("Policy");
        var caller = module.getMethod("Caller", new IOpenClass[]{ policyType });
        var classLoader = compiled.getClassLoader();

        var debugger = new TraceDebugger(DebugListener.NOOP);
        debugger.setBreakpoints(Set.of("Caller"));
        debugger.start("field-access", classLoader, false, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            var policy = policyType.newInstance(env);
            policyType.getField("census").set(policy, 5, env);
            caller.invoke(module.newInstance(env), new Object[]{ policy }, env);
        });
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(20_000));
        // Run the frame to its own exit so its Count step has executed and its inputs can be read back.
        assertEquals(DebugStatus.SUSPENDED, debugger.command(DebugCommand.STEP_OUT, 20_000));
        var stack = debugger.stack();
        var frame = stack.get(stack.size() - 1);
        assertEquals("Caller", frame.getName());

        var objectMapper = new ObjectMapper();
        var mapper = new TraceDebugMapper(objectMapper,
                new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper), new TraceParameterRegistry());
        // Count = CountCensus(policy.census): the input is the census field with its own value, not policy.
        List<ParameterValue> inputs = mapper.freezeStepInputs(frame, "R0C0", classLoader, false).inputs();
        debugger.terminate(20_000);

        assertEquals(List.of("policy.census"), inputs.stream().map(ParameterValue::name).toList(),
                "the field access is the precise input, not the root parameter");
        assertEquals("Integer", inputs.get(0).description());
        assertNotNull(inputs.get(0).value(), "the field's recorded value is shown");
        assertEquals(5, inputs.get(0).value().asInt());
    }
}
