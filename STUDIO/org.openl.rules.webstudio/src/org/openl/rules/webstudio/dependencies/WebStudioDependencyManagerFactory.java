package org.openl.rules.webstudio.dependencies;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.WebStudio;

import java.util.ArrayList;
import java.util.List;

public class WebStudioDependencyManagerFactory {
    private final Log log = LogFactory.getLog(WebStudioDependencyManagerFactory.class);

    private final WebStudio studio;

    private ProjectDescriptor lastProject;
    private IDependencyManager lastDependencyManager;

    public WebStudioDependencyManagerFactory(WebStudio studio) {
        this.studio = studio;
    }

    public IDependencyManager getDependencyManager(Module module, boolean includeDependentProjects) {
        ProjectDescriptor project = module.getProject();

        if (lastDependencyManager != null && lastProject == project) {
            return lastDependencyManager;
        }

        List<ProjectDescriptor> projectDescriptors = new ArrayList<ProjectDescriptor>();
        projectDescriptors.add(project);

        if (includeDependentProjects) {
            addDependentProjects(projectDescriptors, project);
        }

        WebStudioWorkspaceRelatedDependencyManager dependencyManager = new WebStudioWorkspaceRelatedDependencyManager(projectDescriptors);
        dependencyManager.setExternalParameters(studio.getSystemConfigManager().getProperties());
        dependencyManager.setExecutionMode(false);

        lastProject = project;
        lastDependencyManager = dependencyManager;

        return dependencyManager;
    }

    public void reset() {
        lastProject = null;
        lastDependencyManager = null;
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
