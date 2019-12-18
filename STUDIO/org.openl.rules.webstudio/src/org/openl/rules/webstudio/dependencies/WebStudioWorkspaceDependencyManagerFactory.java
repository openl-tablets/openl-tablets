package org.openl.rules.webstudio.dependencies;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.project.model.Module;
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

    public WebStudioWorkspaceRelatedDependencyManager getDependencyManager(Module module, boolean singleModuleMode) {
        List<ProjectDescriptor> projectDescriptors = new ArrayList<>();
        projectDescriptors.add(module.getProject());
        projectDescriptors.addAll(getDependentProjects(module));

        ClassLoader rootClassLoader = WebStudioWorkspaceRelatedDependencyManager.class.getClassLoader();
        return new WebStudioWorkspaceRelatedDependencyManager(projectDescriptors,
            rootClassLoader,
            singleModuleMode,
            false,
            studio.getExternalProperties());
    }

    private List<ProjectDescriptor> getDependentProjects(Module module) {
        ProjectDescriptor project = module.getProject();

        List<ProjectDescriptor> projectDescriptors = new ArrayList<>();
        addDependentProjects(projectDescriptors, project);

        return projectDescriptors;
    }

    private void addDependentProjects(List<ProjectDescriptor> projectDescriptors, ProjectDescriptor project) {
        if (project.getDependencies() != null) {
            for (ProjectDependencyDescriptor dependencyDescriptor : project.getDependencies()) {
                boolean found = false;
                for (ProjectDescriptor projectDescriptor : studio.getAllProjects()) {
                    if (dependencyDescriptor.getName().equals(projectDescriptor.getName())) {
                        if (!projectDescriptors.contains(projectDescriptor)) {
                            projectDescriptors.add(projectDescriptor);
                            addDependentProjects(projectDescriptors, projectDescriptor);
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    log.warn("Dependency '{}' for project '{}' is not found.",
                        dependencyDescriptor.getName(),
                        project.getName());
                }
            }
        }
    }
}
