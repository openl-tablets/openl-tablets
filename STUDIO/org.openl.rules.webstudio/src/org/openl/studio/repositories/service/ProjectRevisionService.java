package org.openl.studio.repositories.service;

import java.io.IOException;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.repositories.model.ProjectRevision;

public interface ProjectRevisionService {

    /**
     * Retrieves project revision history.
     *
     * @param repository the repository
     * @param projectName the name of the project
     * @param branch optional branch name
     * @param searchTerm optional search term to filter revisions
     * @param techRevs whether to include technical revisions
     * @param page pagination parameters
     * @return paginated project revision history
     * @throws IOException if an I/O error occurs
     * @throws ProjectException if the project cannot be found or accessed
     */
    PageResponse<ProjectRevision> getProjectRevision(Repository repository,
                                                     String projectName,
                                                     String branch,
                                                     String searchTerm,
                                                     boolean techRevs,
                                                     Pageable page) throws IOException, ProjectException;
}
