package org.openl.rules.webstudio.dependencies;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.WebStudio;

import java.util.ArrayList;
import java.util.List;

public class WebStudioDependencyManagerFactory {
    private final Log log = LogFactory.getLog(WebStudioDependencyManagerFactory.class);

    private final WebStudio studio;

    public WebStudioDependencyManagerFactory(WebStudio studio) {
        this.studio = studio;
    }

    public WebStudioWorkspaceRelatedDependencyManager getDependencyManager(Module module, boolean singleModuleMode) {
        List<ProjectDescriptor> projectDescriptors = getDependentProjects(module);

        WebStudioWorkspaceRelatedDependencyManager dependencyManager = new WebStudioWorkspaceRelatedDependencyManager(projectDescriptors, singleModuleMode);
        dependencyManager.setExternalParameters(studio.getSystemConfigManager().getProperties());
        dependencyManager.setExecutionMode(false);

        return dependencyManager;
    }

    public List<ProjectDescriptor> getDependentProjects(Module module) {
        ProjectDescriptor project = module.getProject();

        List<ProjectDescriptor> projectDescriptors = new ArrayList<ProjectDescriptor>();
        projectDescriptors.add(project);

        addDependentProjects(projectDescriptors, project);

        return projectDescriptors;
    }

    private void addDependentProjects(List<ProjectDescriptor> projectDescriptors, ProjectDescriptor project) {
        if (project.getDependencies() != null) {
            for (ProjectDependencyDescriptor dependencyDescriptor : project.getDependencies()) {
                boolean found = false;
                for (ProjectDescriptor projectDescriptor : studio.getAllProjects()) {
                    if (dependencyDescriptor.getName().equals(projectDescriptor.getName())) {
                        projectDescriptors.add(projectDescriptor);
                        addDependentProjects(projectDescriptors, projectDescriptor);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Dependency '%s' for project '%s' not found", dependencyDescriptor.getName(), project.getName()));
                    }
                }
            }
        }
    }
}
