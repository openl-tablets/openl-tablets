package org.openl.studio.projects.service.project.status;

import jakarta.annotation.Nullable;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;

/**
 * Maps a {@link RulesProject} to a {@link ProjectStatusViewModel}.
 *
 * @author Vladyslav Pikus
 */
public interface ProjectStatusMapper {

    /**
     * Build a status view for the given project. The compilation job is looked up via the
     * session-scoped registry, so this overload must be called from a thread that has the
     * web session bound (typically a REST request thread).
     *
     * @param project workspace project to inspect
     * @return populated status view
     */
    ProjectStatusViewModel map(RulesProject project);

    /**
     * Build a status view for the given project using the supplied {@link ProjectModel}
     * directly, without consulting the session-scoped compilation registry. Use this
     * overload when the model is already known (e.g. from a Spring event handler running
     * outside a request context).
     *
     * @param project workspace project to inspect
     * @param model   project model to read compilation info from; {@code null} means no
     *                compilation has been initiated (status will be {@code IDLE})
     * @return populated status view
     */
    ProjectStatusViewModel map(RulesProject project, @Nullable ProjectModel model);
}
