package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

public class SimpleDependencyManager extends AbstractDependencyManager {

    private Collection<ProjectDescriptor> projects;
    private boolean singleModuleMode = false;

    public SimpleDependencyManager(Collection<ProjectDescriptor> projects,
            ClassLoader rootClassLoader,
            boolean singleModuleMode,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(rootClassLoader, executionMode, externalParameters);
        this.projects = Objects.requireNonNull(projects, "projects cannot be null");
        this.singleModuleMode = singleModuleMode;
    }

    @Override
    protected Map<String, IDependencyLoader> initDependencyLoaders() {
        Map<String, IDependencyLoader> dependencyLoaders = new HashMap<>();
        for (ProjectDescriptor project : projects) {
            try {
                Collection<Module> modulesOfProject = project.getModules();
                if (!modulesOfProject.isEmpty()) {
                    for (final Module m : modulesOfProject) {
                        final SimpleDependencyLoader moduleDependencyLoader = SimpleDependencyLoader
                            .forModule(m, singleModuleMode, executionMode, this);
                        dependencyLoaders.put(moduleDependencyLoader.getDependencyName(), moduleDependencyLoader);
                    }
                }

                final SimpleDependencyLoader projectDependencyLoader = SimpleDependencyLoader
                    .forProject(project, singleModuleMode, executionMode, this);
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