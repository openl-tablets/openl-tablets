package org.openl.rules.project.instantiation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleProjectDependencyManager extends AbstractProjectDependencyManager {

    private final Logger log = LoggerFactory.getLogger(SimpleProjectDependencyManager.class);

    private List<IDependencyLoader> dependencyLoaders;

    private Collection<ProjectDescriptor> projects;

    private Collection<ProjectDescriptor> projectDescriptors;
    private Collection<String> dependencyNames = null;

    private boolean singleModuleMode = false;
    private boolean executionMode = true;

    @Override
    public Collection<String> getAllDependencies() {
        if (dependencyLoaders == null) {
            initDependencyLoaders();
        }
        return dependencyNames;
    }

    public SimpleProjectDependencyManager(Collection<ProjectDescriptor> projects,
            ClassLoader rootClassLoader,
            boolean singleModuleMode,
            boolean executionMode) {
        super(rootClassLoader);
        if (projects == null) {
            throw new IllegalArgumentException("projects must not be null!");
        }
        this.projects = projects;
        this.singleModuleMode = singleModuleMode;
        this.executionMode = executionMode;
        this.singleModuleMode = singleModuleMode;
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
                            dependencyLoaders.add(new SimpleProjectDependencyLoader(m
                                .getName(), Arrays.asList(m), singleModuleMode, executionMode, false));
                            dependencyNames.add(m.getName());
                        }
                    }

                    String dependencyName = ProjectExternalDependenciesHelper
                        .buildDependencyNameForProjectName(project.getName());
                    IDependencyLoader projectLoader = new SimpleProjectDependencyLoader(dependencyName,
                        project.getModules(),
                        singleModuleMode,
                        executionMode,
                        true);
                    projectDescriptors.add(project);
                    dependencyLoaders.add(projectLoader);
                } catch (Exception e) {
                    log.error("Failed to build dependency manager loaders for project '{}'!", project.getName(), e);
                }
            }
        }
    }
}