package org.openl.rules.workspace.dtr;

import java.util.List;

import org.openl.rules.project.abstraction.AProject;

/**
 * The projects a repository holds, once for each branch that holds them.
 *
 * <p>A listing that is not complete may miss projects: the index of the repository is still built, a branch failed to
 * be read, or the repository cannot be read. A project it lists exists all the same.
 *
 * @param projects the projects listed
 * @param complete whether every project of the repository is listed
 */
public record RepositoryProjects(List<AProject> projects, boolean complete) {

    /** A listing of a repository nothing is known about. */
    public static final RepositoryProjects UNKNOWN = new RepositoryProjects(List.of(), false);
}
