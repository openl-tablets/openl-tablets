package org.openl.rules.workspace.abstracts;

import java.util.Collection;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;

/**
 * An interfaces to be implemented by containers of special types of {@link AProject}s;
 *
 * @author Aleh Bykhavets
 */
public interface ProjectsContainer {

    /**
     * Returns a project by its name.
     *
     * @param name project name
     * @return project by name
     * @throws ProjectException if container does not contain a project with given name
     */
    AProject getProject(String repositoryId, String name) throws ProjectException;

    /**
     * Returns collection of projects in the container. The order of the projects returned depends on the specific
     * container. Never returns <code>null</code>.
     *
     * @return all projects in the container.
     */
    Collection<? extends AProject> getProjects();

    /**
     * Returns <code>true</code> if there is a project with given name.
     *
     * @param name name of a project whose presence in the container is to be tested
     * @return if there is a project with given name
     */
    boolean hasProject(String repositoryId, String name);
}
