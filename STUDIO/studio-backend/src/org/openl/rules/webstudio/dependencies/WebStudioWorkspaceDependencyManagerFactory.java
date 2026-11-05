package org.openl.rules.webstudio.dependencies;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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

    /**
     * The project and every project it depends on, directly or through another one.
     *
     * <p>The projects of the session are read once for the whole walk, so every dependency is found in the same
     * reading of them, even when the session reads its projects again meanwhile.
     */
    public Set<ProjectDescriptor> resolveWorkspace(ProjectDescriptor project) {
        var workspace = new LinkedHashSet<ProjectDescriptor>();
        var breadcrumbs = new HashSet<ProjectDescriptor>();
        workspace.add(project);
        breadcrumbs.add(project);
        resolveWorkspaceRec(project, studio.getAllProjects(), workspace, breadcrumbs);
        return workspace;
    }

    private void resolveWorkspaceRec(ProjectDescriptor p,
                                     List<ProjectDescriptor> allProjects,
                                     Set<ProjectDescriptor> workspace,
                                     Set<ProjectDescriptor> breadcrumbs) {
        if (p.getDependencies() != null && !p.getDependencies().isEmpty()) {
            var projectDependencyNames = p.getDependencies()
                    .stream()
                    .map(ProjectDependencyDescriptor::getName)
                    .collect(Collectors.toSet());
            for (ProjectDescriptor pd : allProjects) {
                if (projectDependencyNames.contains(pd.getName())) {
                    if (!breadcrumbs.contains(pd)) {
                        workspace.add(pd);
                        breadcrumbs.add(pd);
                        resolveWorkspaceRec(pd, allProjects, workspace, breadcrumbs);
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
