package org.openl.rules.project.instantiation;

import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.IDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class SimpleProjectDependencyManager extends AbstractProjectDependencyManager {

    private final Logger log = LoggerFactory.getLogger(SimpleProjectDependencyManager.class);

    private List<IDependencyLoader> dependencyLoaders;

    private Collection<ProjectDescriptor> projects;

    private Collection<ProjectDescriptor> projectDescriptors;
    private Collection<String> dependencyNames = null;
    
    private boolean singleModuleMode = false;
    private boolean executionMode = true;

    @Override
    public Collection<String> listDependencies() {
        if (dependencyLoaders == null) {
            initDependencyLoaders();
        }
        return dependencyNames;
    }
    
    public SimpleProjectDependencyManager(Collection<ProjectDescriptor> projects,
                                          boolean singleModuleMode,
                                          boolean executionMode) {
        super();
        if (projects == null) {
            throw new IllegalArgumentException("projects can't be null!");
        }
        this.projects = projects;
        this.singleModuleMode = singleModuleMode;
        this.executionMode = executionMode;
        this.singleModuleMode = singleModuleMode;
    }

    public SimpleProjectDependencyManager(Collection<ProjectDescriptor> projects, boolean singleModuleMode) {
        this(projects, singleModuleMode, true);
    }
    
    @Override
    public Collection<ProjectDescriptor> getProjectDescriptors() {
        if (dependencyLoaders == null){
            initDependencyLoaders();
        }
        return projectDescriptors;
    }

    @Override
    public List<IDependencyLoader> getDependencyLoaders() {
        if (dependencyLoaders == null){
            initDependencyLoaders();
        }
        return dependencyLoaders;
    }

    private synchronized void initDependencyLoaders() {
        if (projectDescriptors == null && dependencyLoaders == null) {
            dependencyLoaders = new ArrayList<IDependencyLoader>();
            projectDescriptors = new ArrayList<ProjectDescriptor>();
            dependencyNames = new HashSet<String>();
            for (ProjectDescriptor project : projects) {
                try {
                    Collection<Module> modulesOfProject = project.getModules();
                    if (!modulesOfProject.isEmpty()) {
                        for (final Module m : modulesOfProject) {
                            dependencyLoaders.add(new SimpleProjectDependencyLoader(m.getName(),
                                Arrays.asList(m),
                                singleModuleMode,
                                executionMode));
                            dependencyNames.add(m.getName());
                        }
                    }

                    String dependencyName = ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(project.getName());
                    IDependencyLoader projectLoader = new SimpleProjectDependencyLoader(dependencyName,
                        project.getModules(),
                        singleModuleMode,
                        executionMode);
                    projectDescriptors.add(project);
                    dependencyLoaders.add(projectLoader);
                } catch (Exception e) {
                    log.error("Build dependency manager loaders for project {} was failed!", project.getName(), e);
                }
            }
        }
    }

    @Override
    public void reset(IDependency dependency) {
        if (dependencyLoaders == null) {
            return;
        }

        String dependencyName = dependency.getNode().getIdentifier();

        ProjectDescriptor projectToReset = null;

        searchProject:
        for (ProjectDescriptor project : projects) {
            if (dependencyName.equals(ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(project.getName()))) {
                projectToReset = project;
                break;
            }

            for (Module module : project.getModules()) {
                if (dependencyName.equals(module.getName())) {
                    projectToReset = project;
                    break searchProject;
                }
            }
        }

        if (projectToReset != null) {
            clearClassLoader(projectToReset.getName());
            String projectDependency = ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(projectToReset.getName());

            for (IDependencyLoader dependencyLoader : dependencyLoaders) {
                SimpleProjectDependencyLoader loader = (SimpleProjectDependencyLoader) dependencyLoader;
                String loaderDependencyName = loader.getDependencyName();

                if (loaderDependencyName.equals(projectDependency)) {
                    loader.reset();
                }

                for (Module module : projectToReset.getModules()) {
                    if (loaderDependencyName.equals(module.getName())) {
                        loader.reset();
                    }
                }
            }
        }
    }

    @Override
    public void resetAll() {
        if (dependencyLoaders == null) {
            return;
        }
        clearAllClassLoader();
    }
}