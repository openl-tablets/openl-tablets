package org.openl.studio.projects.service.project.compile;

import java.util.Optional;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.ProjectIdModel;

/**
 * Session-scoped registry that owns the current {@link CompilationJob} for a
 * WebStudio session and ensures repeated open requests for the same project
 * reuse the existing job.
 *
 * @author Vladyslav Pikus
 */
public interface CompilationJobRegistry {

    /**
     * Return an existing compilation job for the given project, or create a new
     * one tracking the supplied {@link ProjectModel} if none matches.
     *
     * @param projectId target project identifier
     * @param model     project model whose compilation should be tracked
     * @return the current compilation job for the supplied project
     */
    @NotNull
    CompilationJob acquire(@NotNull ProjectIdModel projectId, @NotNull ProjectModel model);

    /**
     * Look up the current compilation job for the given project and branch without
     * registering a new one. Used by read-only callers (e.g. the project status mapper)
     * that must not trigger any compilation side effects.
     *
     * @param projectId target project identifier
     * @param branch    branch name the caller expects; {@code null} for repositories without
     *                  branch support
     * @return job tracking the active compilation for this project/branch, or empty when no
     *         matching compilation has been registered
     */
    @NotNull
    Optional<CompilationJob> find(@NotNull ProjectIdModel projectId, @Nullable String branch);

    /**
     * Drop the cached compilation job so that read-only callers no longer observe a stale
     * compile state. Any in-flight compilation future is cancelled. Invoked when the session
     * workspace is reset.
     */
    void clear();
}
