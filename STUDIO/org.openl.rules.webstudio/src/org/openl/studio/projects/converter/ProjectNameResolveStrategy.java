package org.openl.studio.projects.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 * Resolves projects by business name. First consults the workspace name index; if it returns nothing, scans each design
 * repository directly. For repositories that support mapped folders, matches on the project business name.
 *
 * @author Vladyslav Pikus
 */
@Component
@Order(2)
public class ProjectNameResolveStrategy implements ProjectResolveStrategy {

    @Override
    public List<RulesProject> resolve(UserWorkspace workspace, String identity) {
        var matches = new ArrayList<>(workspace.getProjectsByName(identity));
        if (matches.isEmpty()) {
            workspace.getDesignTimeRepository()
                    .getRepositories()
                    .forEach(repository -> findInRepository(workspace, repository, identity)
                            .forEach(project -> addUnique(matches, project)));
        }
        return matches;
    }

    private List<RulesProject> findInRepository(UserWorkspace workspace, Repository repository, String name) {
        var repoId = repository.getId();
        var direct = tryGetProject(workspace, repoId, name);
        if (direct.isPresent()) {
            return List.of(direct.get());
        }
        if (!repository.supports().mappedFolders()) {
            return List.of();
        }
        return workspace.getDesignTimeRepository().getProjects(repoId).stream()
                .filter(aproj -> name.equalsIgnoreCase(aproj.getBusinessName()))
                .flatMap(aproj -> tryGetProject(workspace, repoId, aproj.getName()).stream())
                .toList();
    }

    private Optional<RulesProject> tryGetProject(UserWorkspace workspace, String repoId, String name) {
        try {
            return Optional.of(workspace.getProject(repoId, name));
        } catch (ProjectException e) {
            return Optional.empty();
        }
    }

    private static void addUnique(List<RulesProject> list, RulesProject project) {
        if (!list.contains(project)) {
            list.add(project);
        }
    }
}
