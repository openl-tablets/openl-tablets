package org.openl.studio.projects.model.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.config.ObjectSchemaGeneratorConfiguration;
import org.openl.studio.projects.service.trace.DebugFrame;
import org.openl.studio.projects.service.trace.DebugListener;
import org.openl.studio.projects.service.trace.TraceDebugger;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;

/**
 * When a business click inspects the step a run failed on, its step-inputs carry the error, so the failing
 * step's own panel explains why it failed — the way the advanced view shows the error on both the frame and
 * the step. A step that finished before the error carries none. Callers above the throwing frame are stamped
 * with the same throwable, so clicking an ancestor table or its interrupted calling step still shows why the
 * run failed.
 *
 * <p>Fixture {@code errorProject.xlsx}: {@code Validate.Result} raises {@code error("Some text")} while
 * {@code Validate.Flag} finishes normally. The call chain is DeterminePolicy → DetermineManual → Validate.
 */
class ErrorStepInputsTest {

    private static final String SRC = "test/rules/trace-debug/errorProject.xlsx";

    @Test
    @DisplayName("The step the run failed on carries the error; a step that finished does not")
    void erroringStepCarriesTheError() {
        var parked = parkOnError();
        var frame = parked.stack().get(parked.stack().size() - 1);
        assertEquals("Validate", frame.getName(), "parked on the frame that threw");

        var mapper = mapper();
        // Result (R1C0) is the cell that raised error("Some text").
        var erroring = mapper.freezeStepInputs(frame, "R1C0", parked.classLoader(), false);
        // Flag (R0C0) finished before the error.
        var finished = mapper.freezeStepInputs(frame, "R0C0", parked.classLoader(), false);
        parked.debugger().terminate(20_000);

        assertEquals(List.of("Some text"),
                erroring.errors().stream().map(MessageDescription::summary).toList(),
                "the failing step carries the error");
        assertNull(finished.errors(), "a step that finished before the error carries none");
    }

    @Test
    @DisplayName("A live caller of the throwing frame exposes the same error on the frame and its calling step")
    void ancestorFrameAndCallingStepCarryTheError() {
        var parked = parkOnError();
        var stack = parked.stack();
        assertEquals(3, stack.size(), "DeterminePolicy → DetermineManual → Validate");
        DebugFrame root = stack.get(0);
        DebugFrame caller = stack.get(1);
        assertEquals("DeterminePolicy", root.getName());
        assertEquals("DetermineManual", caller.getName());
        assertNotNull(root.getError(), "the root is stamped so inspecting it still reads the failure");
        assertNotNull(caller.getError(), "the mid-stack caller is stamped too");

        var mapper = mapper();
        var classLoader = parked.classLoader();
        assertEquals(List.of("Some text"),
                mapper.freezeVariables(root, classLoader, false).errors().stream()
                        .map(MessageDescription::summary).toList(),
                "the root frame's variables carry the child's error");
        assertEquals(List.of("Some text"),
                mapper.freezeVariables(caller, classLoader, false).errors().stream()
                        .map(MessageDescription::summary).toList(),
                "the caller's variables carry the child's error");

        // The business view also clicks the interrupted calling step (`$Validation = ERROR`); that step is
        // the caller's current location while the child is still on the stack.
        assertNotNull(caller.getLocation(), "the caller is paused on the step that invoked the failing table");
        String callingRef = caller.getLocation().ref();
        assertNotNull(callingRef);
        var callingStep = mapper.freezeStepInputs(caller, callingRef, classLoader, false);
        assertEquals(List.of("Some text"),
                callingStep.errors().stream().map(MessageDescription::summary).toList(),
                "the interrupted calling step carries the child's error");

        // The stamp is for inspect payloads only — the stack's error mark stays on the completed thrower so
        // Advanced Trace still shows waiting callers distinctly from the failed frame.
        var stackView = TraceDebugMapper.toStackView(DebugStatus.SUSPENDED, stack, null);
        assertFalse(stackView.frames().get(0).error(), "stamped root is not a completed failure");
        assertFalse(stackView.frames().get(1).error(), "stamped mid-stack caller is not a completed failure");
        assertTrue(stackView.frames().get(2).error(), "the throwing frame failed and completed");
        assertTrue(stackView.frames().get(2).completed());
        parked.debugger().terminate(20_000);
    }

    /** Park DeterminePolicy on Validate's thrown error the way a business inspect session does. */
    private static Parked parkOnError() {
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
        return new Parked(debugger, debugger.stack(), classLoader);
    }

    private static TraceDebugMapper mapper() {
        var objectMapper = new ObjectMapper();
        return new TraceDebugMapper(objectMapper,
                new ObjectSchemaGeneratorConfiguration().schemaGenerator(objectMapper), new TraceParameterRegistry());
    }

    private record Parked(TraceDebugger debugger, List<DebugFrame> stack, ClassLoader classLoader) {
    }
}
