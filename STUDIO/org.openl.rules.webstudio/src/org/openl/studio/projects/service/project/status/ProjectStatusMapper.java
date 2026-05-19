package org.openl.studio.projects.service.project.status;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;

/**
 * Maps a {@link RulesProject} to a {@link ProjectStatusViewModel}.
 *
 * @author Vladyslav Pikus
 */
public interface ProjectStatusMapper {

    /**
     * Build a status view for the given project, including compilation state and
     * messages when the project is opened.
     *
     * @param project workspace project to inspect
     * @return populated status view
     */
    ProjectStatusViewModel map(RulesProject project);
}
