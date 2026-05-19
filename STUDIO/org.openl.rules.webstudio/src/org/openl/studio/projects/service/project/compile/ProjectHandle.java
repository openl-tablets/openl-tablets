package org.openl.studio.projects.service.project.compile;

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
     * {@link java.util.concurrent.CompletionException}.
     */
    default ProjectModel awaitCompiled() {
        compilation().future().join();
        return project();
    }
}
