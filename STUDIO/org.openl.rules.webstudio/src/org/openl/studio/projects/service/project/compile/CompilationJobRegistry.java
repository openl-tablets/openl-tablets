package org.openl.studio.projects.service.project.compile;

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
}
