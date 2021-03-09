package org.openl.rules.webstudio.dependencies;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
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
        List<ProjectDescriptor> workspaceProjectsToResolveDependencies = resolveWorkspace(project);
        ClassLoader rootClassLoader = WebStudioWorkspaceRelatedDependencyManager.class.getClassLoader();
        return new WebStudioWorkspaceRelatedDependencyManager(workspaceProjectsToResolveDependencies,
            rootClassLoader,
            false,
            studio.getExternalProperties());
    }

    public List<ProjectDescriptor> resolveWorkspace(ProjectDescriptor project) {
        List<ProjectDescriptor> projectsInWorkspace = new ArrayList<>();
        Queue<ProjectDescriptor> queue = new ArrayDeque<>();
        queue.add(project);
        while (!queue.isEmpty()) {
            ProjectDescriptor p = queue.poll();
            projectsInWorkspace.add(p);
            if (p.getDependencies() != null && !p.getDependencies().isEmpty()) {
                Set<String> projectDependencyNames = p.getDependencies()
                    .stream()
                    .map(ProjectDependencyDescriptor::getName)
                    .collect(Collectors.toSet());
                for (ProjectDescriptor pd : studio.getAllProjects()) {
                    if (projectDependencyNames.contains(pd.getName())) {
                        queue.add(pd);
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
        return projectsInWorkspace;
    }
}
