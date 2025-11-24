package org.openl.studio.projects.service;

import java.util.List;
import jakarta.annotation.Nonnull;

import org.openl.rules.project.abstraction.AProject;
import org.openl.studio.projects.model.ProjectViewModel;

/**
 * Project service API
 *
 * @author Vladyslav Pikus
 */
public interface ProjectService<T extends AProject> {

    /**
     * Get projects by criteria query
     *
     * @param query criteria query
     * @return list of projects
     */
    @Nonnull
    List<ProjectViewModel> getProjects(@Nonnull ProjectCriteriaQuery query);

}
