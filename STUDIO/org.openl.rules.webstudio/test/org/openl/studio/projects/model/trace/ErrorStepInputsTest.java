package org.openl.studio.projects.model.trace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.studio.projects.service.trace.DebugListener;
import org.openl.studio.projects.service.trace.TraceDebugger;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;

/**
 * When a business click inspects the step a run failed on, its step-inputs carry the error, so the failing
 * step's own panel explains why it failed — the way the advanced view shows the error on both the frame and
 * the step. A step that finished before the error carries none.
 *
 * <p>Fixture {@code errorProject.xlsx}: {@code Validate.Result} raises {@code error("Some text")} while
 * {@code Validate.Flag} finishes normally.
 */
class ErrorStepInputsTest {

    private static final String SRC = "test/rules/trace-debug/errorProject.xlsx";

    @Test
    @DisplayName("The step the run failed on carries the error; a step that finished does not")
    void erroringStepCarriesTheError() {
        var compiled = new RulesEngineFactory<>(SRC).getCompiledOpenClass();
        var module = compiled.getOpenClass();
        var method = module.getMethods().stream()
                .filter(m -> "DeterminePolicy".equals(m.getName())).findFirst().orElseThrow();
        var classLoader = compiled.getClassLoader();

        var debugger = new TraceDebugger(DebugListener.NOOP);
        // Break-on-exception on (the default): the run parks on the throwing frame, as a business click's
        // inspect session does, so the frame carries its error and the cell it failed on.
        debugger.start("err-step", classLoader, false, true, true, () -> {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setTracer(debugger.tracer());
            method.invoke(module.newInstance(env), new Object[]{1}, env);
        });
        assertEquals(DebugStatus.SUSPENDED, debugger.awaitInitialHalt(20_000));
        var stack = debugger.stack();
        var frame = stack.get(stack.size() - 1);
        assertEquals("Validate", frame.getName(), "parked on the frame that threw");

        var objectMapper = new ObjectMapper();
        var mapper = new TraceDebugMapper(objectMapper,
                new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper), new TraceParameterRegistry());
        // Result (R1C0) is the cell that raised error("Some text").
        var erroring = mapper.freezeStepInputs(frame, "R1C0", classLoader, false);
        // Flag (R0C0) finished before the error.
        var finished = mapper.freezeStepInputs(frame, "R0C0", classLoader, false);
        debugger.terminate(20_000);

        assertEquals(List.of("Some text"),
                erroring.errors().stream().map(MessageDescription::summary).toList(),
                "the failing step carries the error");
        assertNull(finished.errors(), "a step that finished before the error carries none");
    }
}
