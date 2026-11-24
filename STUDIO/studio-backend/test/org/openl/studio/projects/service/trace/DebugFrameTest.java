package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.studio.projects.model.trace.FrameKind;

/** Verifies what a frame keeps of the steps it executed. */
class DebugFrameTest {

    /** More steps than a frame kept before every executed step was recorded. */
    private static final int STEPS = 6_000;

    @Test
    @DisplayName("Keeps every executed step, however many the frame ran")
    void keepsEveryExecutedStep() {
        var descriptor = new SourceClassifier.FrameDescriptor(FrameKind.TBASIC, "uAlgo", "Algo", null);
        var frame = new DebugFrame(descriptor, new Object(), null, new Object[0], null, 0);
        var executors = new ArrayList<Object>();
        for (int i = 0; i < STEPS; i++) {
            var executor = new Object();
            executors.add(executor);
            frame.recordExecutedStep(executor, "S" + i, null, i, 0);
        }

        assertEquals(STEPS, frame.getExecutedSteps().size());
        assertNotNull(frame.executedStepFor(executors.getLast()), "a re-read of the last step finds it");
        assertEquals(STEPS, frame.toCallNode(UnaryOperator.identity(), false).steps().size(),
                "the executed tree lists every step");
    }
}
