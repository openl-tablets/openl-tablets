package org.openl.studio.projects.service;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.webstudio.dependencies.ProjectPom;

/**
 * @author ybiruk
 */
public interface ProjectDependencyResolver {


    /**
     * Returns everything the project declares a dependency on, whether the workspace has it or not, as a list of
     * projects shows it: only the {@code pom.xml} of an opened project gives coordinates.
     */
    default List<ProjectDependency> getDependencies(RulesProject project) {
        return getDependencies(project, false);
    }

    /**
     * Returns everything the project declares a dependency on, whether the workspace has it or not.
     *
     * @param everyPom whether the {@code pom.xml} of a closed project gives coordinates too, as one project shown
     *                 does. It is read from the repository, and the archives it names are downloaded at first: too
     *                 much for every project of a list.
     */
    List<ProjectDependency> getDependencies(RulesProject project, boolean everyPom);

    /**
     * Returns all projects that project depends on.
     */
    default List<RulesProject> getProjectDependencies(RulesProject project) {
        return getDependencies(project).stream()
                .map(ProjectDependency::project)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Returns the Maven coordinates of the libraries the project declares: the dependencies without a name.
     */
    List<String> getLibraries(RulesProject project);

    /**
     * Returns the {@code pom.xml} of the project, whose dependencies are libraries of the project too, or {@code null}
     * when it has none, it cannot be read or no Maven repository is set.
     */
    @Nullable ProjectPom getPom(RulesProject project);

    /**
     * Returns all projects that depend on project
     */
    List<RulesProject> getDependsOnProject(RulesProject project) throws ProjectException;
}
