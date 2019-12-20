package org.openl.rules.project.instantiation;

import java.util.ArrayList;
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
    protected Map<String, Collection<IDependencyLoader>> initDependencyLoaders() {
        Map<String, Collection<IDependencyLoader>> dependencyLoaders = new HashMap<>();
        for (ProjectDescriptor project : projects) {
            try {
                for (final Module m : project.getModules()) {
                    final SimpleDependencyLoader moduleDependencyLoader = new SimpleDependencyLoader(project,
                        m,
                        singleModuleMode,
                        executionMode,
                        this);
                    Collection<IDependencyLoader> dependencyLoadersByName = dependencyLoaders
                        .computeIfAbsent(moduleDependencyLoader.getDependencyName(), e -> new ArrayList<>());
                    dependencyLoadersByName.add(moduleDependencyLoader);
                }

                final SimpleDependencyLoader projectDependencyLoader = new SimpleDependencyLoader(project,
                    null,
                    singleModuleMode,
                    executionMode,
                    this);
                Collection<IDependencyLoader> dependencyLoadersByName = dependencyLoaders
                    .computeIfAbsent(projectDependencyLoader.getDependencyName(), e -> new ArrayList<>());
                dependencyLoadersByName.add(projectDependencyLoader);

            } catch (Exception e) {
                throw new DependencyLoaderInitializationException(
                    String.format("Failed to initialize dependency loaders for project '%s'.", project.getName()),
                    e);
            }
        }
        return dependencyLoaders;
    }
}