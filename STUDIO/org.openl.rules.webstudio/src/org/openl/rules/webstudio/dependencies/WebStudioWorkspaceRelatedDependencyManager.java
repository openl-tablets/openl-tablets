package org.openl.rules.webstudio.dependencies;

import java.util.*;

import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.DependencyLoaderInitializationException;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

public class WebStudioWorkspaceRelatedDependencyManager extends AbstractDependencyManager {

    private final List<ProjectDescriptor> projects;
    private final boolean singleModuleMode;

    public WebStudioWorkspaceRelatedDependencyManager(List<ProjectDescriptor> projects,
            ClassLoader rootClassLoader,
            boolean singleModuleMode,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(rootClassLoader, executionMode, externalParameters);
        this.projects = new ArrayList<>(Objects.requireNonNull(projects, "projects cannot be null"));
        this.singleModuleMode = singleModuleMode;
    }

    protected Map<String, IDependencyLoader> initDependencyLoaders() {
        Map<String, IDependencyLoader> dependencyLoaders = new HashMap<>();
        for (ProjectDescriptor project : projects) {
            try {
                Collection<Module> modulesOfProject = project.getModules();
                if (!modulesOfProject.isEmpty()) {
                    for (final Module m : modulesOfProject) {
                        WebStudioDependencyLoader moduleDependencyLoader = new WebStudioDependencyLoader(project,
                            m,
                            singleModuleMode,
                            this);
                        dependencyLoaders.put(moduleDependencyLoader.getDependencyName(), moduleDependencyLoader);
                    }
                }

                WebStudioDependencyLoader projectDependencyLoader = new WebStudioDependencyLoader(project,
                    null,
                    singleModuleMode,
                    this);
                dependencyLoaders.put(projectDependencyLoader.getDependencyName(), projectDependencyLoader);
            } catch (Exception e) {
                throw new DependencyLoaderInitializationException(
                    String.format("Failed to initialize dependency loaders for project '%s'.", project.getName()),
                    e);
            }
        }
        return dependencyLoaders;
    }
}
