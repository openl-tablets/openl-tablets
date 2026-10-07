package org.openl.studio.projects.service;

import java.util.List;
import java.util.Objects;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;

/**
 * @author ybiruk
 */
public interface ProjectDependencyResolver {


    /**
     * Returns everything the project declares a dependency on, whether the workspace has it or not.
     */
    List<ProjectDependency> getDependencies(RulesProject project);

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
     * Returns all projects that depend on project
     */
    List<RulesProject> getDependsOnProject(RulesProject project) throws ProjectException;
}
