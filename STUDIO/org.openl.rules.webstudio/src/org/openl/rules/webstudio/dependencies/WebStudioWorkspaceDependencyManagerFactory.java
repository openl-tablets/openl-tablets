package org.openl.rules.webstudio.dependencies;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.WebStudio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebStudioWorkspaceDependencyManagerFactory {
    private final Logger log = LoggerFactory.getLogger(WebStudioWorkspaceDependencyManagerFactory.class);

    private final WebStudio studio;

    public WebStudioWorkspaceDependencyManagerFactory(WebStudio studio) {
        this.studio = studio;
    }

    public WebStudioWorkspaceRelatedDependencyManager buildDependencyManager(ProjectDescriptor project) {
        Set<ProjectDescriptor> workspaceProjectsToResolveDependencies = resolveWorkspace(project);
        ClassLoader rootClassLoader = WebStudioWorkspaceRelatedDependencyManager.class.getClassLoader();
        return new WebStudioWorkspaceRelatedDependencyManager(workspaceProjectsToResolveDependencies,
            rootClassLoader,
            false,
            studio.getExternalProperties(), studio.isAutoCompile());
    }

    public Set<ProjectDescriptor> resolveWorkspace(ProjectDescriptor project) {
        Set<ProjectDescriptor> workspace = new LinkedHashSet<>();
        Set<ProjectDescriptor> breadcrumbs = new HashSet<>();
        workspace.add(project);
        breadcrumbs.add(project);
        resolveWorkspaceRec(project, workspace, breadcrumbs);
        return workspace;
    }

    private void resolveWorkspaceRec(ProjectDescriptor p, Set<ProjectDescriptor> workspace, Set<ProjectDescriptor> breadcrumbs) {
        if (p.getDependencies() != null && !p.getDependencies().isEmpty()) {
            Set<String> projectDependencyNames = p.getDependencies()
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
