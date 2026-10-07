package org.openl.rules.webstudio.dependencies;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.WebStudio;

@RequiredArgsConstructor
@Slf4j
public class WebStudioWorkspaceDependencyManagerFactory {

    private final WebStudio studio;

    public WebStudioWorkspaceRelatedDependencyManager buildDependencyManager(ProjectDescriptor project) {
        var workspaceProjectsToResolveDependencies = resolveWorkspace(project);
        var rootClassLoader = WebStudioWorkspaceRelatedDependencyManager.class.getClassLoader();
        return new WebStudioWorkspaceRelatedDependencyManager(workspaceProjectsToResolveDependencies,
                rootClassLoader,
                false,
                studio.getExternalProperties(), studio.isAutoCompile());
    }

    public Set<ProjectDescriptor> resolveWorkspace(ProjectDescriptor project) {
        var workspace = new LinkedHashSet<ProjectDescriptor>();
        var breadcrumbs = new HashSet<ProjectDescriptor>();
        workspace.add(project);
        breadcrumbs.add(project);
        resolveWorkspaceRec(project, workspace, breadcrumbs);
        return workspace;
    }

    private void resolveWorkspaceRec(ProjectDescriptor p, Set<ProjectDescriptor> workspace, Set<ProjectDescriptor> breadcrumbs) {
        if (p.getDependencies() != null && !p.getDependencies().isEmpty()) {
            var projectDependencyNames = p.getDependencies()
                    .stream()
                    .map(ProjectDependencyDescriptor::getName)
                    .collect(Collectors.toSet());
            for (ProjectDescriptor pd : studio.getAllProjects()) {
                if (projectDependencyNames.contains(pd.getName())) {
                    if (!breadcrumbs.contains(pd)) {
                        workspace.add(pd);
                        breadcrumbs.add(pd);
                        resolveWorkspaceRec(pd, workspace, breadcrumbs);
                        breadcrumbs.remove(pd);
                    }
                    projectDependencyNames.remove(pd.getName());
                }
            }
            for (String notFoundProjectDependencyName : projectDependencyNames) {
                log.warn("Dependency '{}' for project '{}' is not found.",
                        p.getName(),
                        notFoundProjectDependencyName);
            }
        }
    }
}
