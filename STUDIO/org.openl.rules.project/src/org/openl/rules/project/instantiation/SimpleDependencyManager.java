package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

public class SimpleDependencyManager extends AbstractDependencyManager {

    private final Collection<ProjectDescriptor> projects;

    public SimpleDependencyManager(Collection<ProjectDescriptor> projects,
            ClassLoader rootClassLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(rootClassLoader, executionMode, externalParameters);
        this.projects = Objects.requireNonNull(projects, "projects cannot be null");
    }

    @Override
    protected Set<IDependencyLoader> initDependencyLoaders() {
        Set<IDependencyLoader> dependencyLoaders = new HashSet<>();
        for (ProjectDescriptor project : projects) {
            try {
                for (final Module m : project.getModules()) {
                    final SimpleDependencyLoader moduleDependencyLoader = new SimpleDependencyLoader(project,
                        m,
                        executionMode,
                        this);
                    dependencyLoaders.add(moduleDependencyLoader);
                }

                final SimpleDependencyLoader projectDependencyLoader = new SimpleDependencyLoader(project,
                    null,
                    executionMode,
                    this);
                dependencyLoaders.add(projectDependencyLoader);
            } catch (Exception e) {
                throw new DependencyLoaderInitializationException(
                    String.format("Failed to initialize dependency loaders for project '%s'.", project.getName()),
                    e);
            }
        }
        return dependencyLoaders;
    }
}