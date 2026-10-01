package org.openl.studio.projects.service;

import jakarta.annotation.Nonnull;

import org.openl.rules.repository.api.Pageable;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.projects.model.ProjectViewModel;

/**
 * Project service API
 *
 * @author Vladyslav Pikus
 */
public interface ProjectService {

    /**
     * Get projects by criteria query
     *
     * @param query criteria query
     * @return list of projects
     */
    @Nonnull
    PageResponse<ProjectViewModel> getProjects(@Nonnull ProjectCriteriaQuery query, @Nonnull Pageable page);

}
