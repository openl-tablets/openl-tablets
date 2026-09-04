package org.openl.studio.repositories.service;

import java.io.IOException;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
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

    /**
     * Retrieves the revision history of the given project, in the branch the project is on.
     *
     * <p>A project that lives only in the workspace has never been published, so it answers with an empty page.
     *
     * <p>Read access is not checked here: the project arrives already resolved for a caller allowed to read it.
     *
     * @param project the project to read the history of
     * @param searchTerm optional search term to filter revisions
     * @param techRevs whether to include technical revisions
     * @param page pagination parameters
     * @return paginated project revision history
     * @throws IOException if an I/O error occurs
     */
    PageResponse<ProjectRevision> getProjectRevision(RulesProject project,
                                                     String searchTerm,
                                                     boolean techRevs,
                                                     Pageable page) throws IOException;

    /**
     * Retrieves the revision history of a single file of the given project, in the branch the project is on.
     *
     * <p>Only the revisions that changed the file are reported, so a project revision that left the file alone
     * is absent unless technical revisions are asked for.
     *
     * <p>A project that lives only in the workspace has never been published, so it answers with an empty page.
     *
     * <p>Read access is not checked here: the project arrives already resolved for a caller allowed to read it.
     *
     * @param project the project holding the file
     * @param path the path of the file relative to the project root
     * @param searchTerm optional search term to filter revisions
     * @param techRevs whether to include revisions that did not change the file
     * @param page pagination parameters
     * @return paginated revision history of the file
     * @throws IOException if an I/O error occurs
     */
    PageResponse<ProjectRevision> getFileRevision(RulesProject project,
                                                  String path,
                                                  String searchTerm,
                                                  boolean techRevs,
                                                  Pageable page) throws IOException;
}
