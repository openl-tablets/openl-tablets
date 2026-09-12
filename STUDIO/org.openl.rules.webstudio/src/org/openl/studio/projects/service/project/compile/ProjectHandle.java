package org.openl.studio.projects.service.project.compile;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.openl.rules.ui.ProjectModel;

/**
 * Handle returned by {@code WorkspaceProjectService.openProject(...)}.
 *
 * <p>The handle exposes an immediately usable {@link ProjectModel} together with a
 * {@link CompilationJob} describing the asynchronous compilation lifecycle. The
 * underlying model is a single instance shared with the WebStudio session, so the
 * same {@link #project()} reference becomes the compiled model once
 * {@link CompilationJob#future()} completes successfully.
 *
 * @author Vladyslav Pikus
 */
public interface ProjectHandle {

    /**
     * Factory for the default handle implementation.
     */
    static ProjectHandle of(ProjectModel project, CompilationJob compilation) {
        return new ProjectHandleImpl(project, compilation);
    }

    /**
     * Project model available immediately, regardless of compilation state.
     */
    ProjectModel project();

    /**
     * Compilation job tracking the asynchronous compilation of {@link #project()}.
     */
    CompilationJob compilation();

    /**
     * Convenience: wait for the compilation to finish and return the (now
     * compiled) project model. Re-throws compilation failures wrapped in a
     * {@link CompletionException}.
     *
     * <p>A compilation the reader stopped counts as finished. What was compiled by then stays readable, and it
     * is the project status that says the rest was never built — a reader who ended the wait is not answered
     * with an error for every read afterwards.
     */
    default ProjectModel awaitCompiled() {
        try {
            compilation().future().join();
        } catch (CancellationException stopped) {
            // The reader asked for the wait to end; what was compiled by then is the answer.
        } catch (CompletionException failed) {
            if (!(failed.getCause() instanceof CancellationException)) {
                throw failed;
            }
        }
        return project();
    }
}
