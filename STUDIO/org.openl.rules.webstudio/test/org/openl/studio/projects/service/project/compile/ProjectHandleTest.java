package org.openl.studio.projects.service.project.compile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import org.openl.rules.ui.ProjectModel;

/**
 * Verifies what a reader waiting for a compilation is answered with when the compilation does not run to its end.
 */
class ProjectHandleTest {

    private final ProjectModel model = mock(ProjectModel.class);

    @Test
    void answersWithWhatWasCompiledWhenTheReaderStoppedTheCompilation() {
        var cycle = new CompletableFuture<Void>();
        cycle.cancel(false);

        // The wait is over, and what the compilation built before it was stopped stays readable.
        assertSame(model, handle(cycle).awaitCompiled());
        // The same through the wrapper a compilation job puts around the cycle.
        assertSame(model, handle(cycle.whenComplete((ignored, error) -> { })).awaitCompiled());
    }

    @Test
    void tellsTheCallerWhenTheCompilationItselfFailed() {
        var cycle = new CompletableFuture<Void>();
        cycle.completeExceptionally(new IllegalStateException("module is broken"));

        var failure = assertThrows(CompletionException.class, () -> handle(cycle).awaitCompiled());

        assertEquals("module is broken", failure.getCause().getMessage());
    }

    private ProjectHandle handle(CompletableFuture<Void> future) {
        var job = mock(CompilationJob.class);
        when(job.future()).thenReturn(future);
        return ProjectHandle.of(model, job);
    }
}
