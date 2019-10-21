package org.openl.rules.webstudio.dependencies;

import java.util.*;

import org.openl.dependency.CompiledDependency;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.IDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebStudioWorkspaceRelatedDependencyManager extends AbstractDependencyManager {

    private final Logger log = LoggerFactory.getLogger(WebStudioWorkspaceRelatedDependencyManager.class);

    private List<IDependencyLoader> dependencyLoaders = null;

    private List<ProjectDescriptor> projects;

    private final List<String> moduleNames = new ArrayList<>();

    private Collection<ProjectDescriptor> projectDescriptors = null;

    private Collection<String> dependencyNames = null;

    private boolean singleModuleMode = false;

    @Override
    public Collection<String> getAllDependencies() {
        if (dependencyLoaders == null) {
            initDependencyLoaders();
        }
        return dependencyNames;
    }

    public WebStudioWorkspaceRelatedDependencyManager(List<ProjectDescriptor> projects,
            ClassLoader rootClassLoader,
            boolean singleModuleMode) {
        super(rootClassLoader);
        this.projects = Objects.requireNonNull(projects, "projects cannot be null");
        this.singleModuleMode = singleModuleMode;
    }

    @Override
    public CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        return super.loadDependency(dependency);
    }

    @Override
    public Collection<ProjectDescriptor> getProjectDescriptors() {
        if (dependencyLoaders == null) {
            initDependencyLoaders();
        }
        return projectDescriptors;
    }

    @Override
    public List<IDependencyLoader> getDependencyLoaders() {
        if (dependencyLoaders == null) {
            initDependencyLoaders();
        }
        return dependencyLoaders;
    }

    private synchronized void initDependencyLoaders() {
        if (projectDescriptors == null && dependencyLoaders == null) {
            dependencyLoaders = new ArrayList<>();
            projectDescriptors = new ArrayList<>();
            dependencyNames = new HashSet<>();
            for (ProjectDescriptor project : projects) {
                try {
                    Collection<Module> modulesOfProject = project.getModules();
                    if (!modulesOfProject.isEmpty()) {
                        for (final Module m : modulesOfProject) {
                            dependencyLoaders.add(new WebStudioDependencyLoader(m.getName(),
                                Collections.singletonList(m),
                                singleModuleMode,
                                false));
                            dependencyNames.add(m.getName());
                            moduleNames.add(m.getName());
                        }
                    }

                    String dependencyName = ProjectExternalDependenciesHelper
                        .buildDependencyNameForProjectName(project.getName());
                    IDependencyLoader projectLoader = new WebStudioDependencyLoader(dependencyName,
                        project.getModules(),
                        singleModuleMode,
                        true);
                    projectDescriptors.add(project);
                    dependencyLoaders.add(projectLoader);
                } catch (Exception e) {
                    log.error("Failed to build dependency manager loaders for project '{}'!", project.getName(), e);
                }
            }
        }
    }

    public Collection<CompiledDependency> getCompiledDependencies() {
        Collection<CompiledDependency> dependencies = new ArrayList<>();

        for (IDependencyLoader dependencyLoader : getDependencyLoaders()) {
            CompiledDependency compiledDependency = ((WebStudioDependencyLoader) dependencyLoader)
                .getCompiledDependency();
            if (compiledDependency != null) {
                dependencies.add(compiledDependency);
            }
        }

        return dependencies;
    }
}
