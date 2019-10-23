package org.openl.rules.webstudio.dependencies;

import java.util.*;

import org.openl.dependency.CompiledDependency;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.DependencyLoaderInitializationException;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

public class WebStudioWorkspaceRelatedDependencyManager extends AbstractDependencyManager {

    private List<ProjectDescriptor> projects;
    private final List<String> moduleNames = new ArrayList<>();
    private boolean singleModuleMode = false;

    public WebStudioWorkspaceRelatedDependencyManager(List<ProjectDescriptor> projects,
            ClassLoader rootClassLoader,
            boolean singleModuleMode,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(rootClassLoader, executionMode, externalParameters);
        this.projects = Objects.requireNonNull(projects, "projects cannot be null");
        this.singleModuleMode = singleModuleMode;
    }

    protected Map<String, IDependencyLoader> initDependencyLoaders() {
        Map<String, IDependencyLoader> dependencyLoaders = new HashMap<>();
        for (ProjectDescriptor project : projects) {
            try {
                Collection<Module> modulesOfProject = project.getModules();
                if (!modulesOfProject.isEmpty()) {
                    for (final Module m : modulesOfProject) {
                        WebStudioDependencyLoader moduleDependencyLoader = WebStudioDependencyLoader
                            .forModule(m, singleModuleMode, this);
                        dependencyLoaders.put(moduleDependencyLoader.getDependencyName(),
                            WebStudioDependencyLoader.forModule(m, singleModuleMode, this));
                        moduleNames.add(m.getName());
                    }
                }

                WebStudioDependencyLoader projectDependencyLoader = WebStudioDependencyLoader
                    .forProject(project, singleModuleMode, this);
                dependencyLoaders.put(projectDependencyLoader.getDependencyName(), projectDependencyLoader);
            } catch (Exception e) {
                throw new DependencyLoaderInitializationException(
                    String.format("Failed to initialize dependency loaders for project '%s'.", project.getName()),
                    e);
            }
        }
        return dependencyLoaders;
    }

    public Collection<CompiledDependency> getCompiledDependencies() {
        Collection<CompiledDependency> dependencies = new ArrayList<>();
        for (IDependencyLoader dependencyLoader : getDependencyLoaders().values()) {
            CompiledDependency compiledDependency = ((WebStudioDependencyLoader) dependencyLoader)
                .getRefToCompiledDependency();
            if (compiledDependency != null) {
                dependencies.add(compiledDependency);
            }
        }
        return dependencies;
    }
}
