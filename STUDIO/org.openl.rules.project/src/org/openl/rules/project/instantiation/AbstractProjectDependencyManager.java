package org.openl.rules.project.instantiation;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.IDependency;

public abstract class AbstractProjectDependencyManager extends DependencyManager {

    protected List<IDependencyLoader> dependencyLoaders;

    // Disable cache. if cache required it should be used in loaders.
    @Override
    public synchronized CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        String dependencyName = dependency.getNode().getIdentifier();
        CompiledDependency compiledDependency = handleLoadDependency(dependency);
        if (compiledDependency == null) {
            if (ProjectExternalDependenciesHelper.isProject(dependencyName)) {
                String projectName = ProjectExternalDependenciesHelper.getProjectName(dependencyName);
                throw new OpenLCompilationException(String.format("Dependency with name '%s' wasn't found", projectName),
                    null,
                    dependency.getNode().getSourceLocation());
            } else {
                throw new OpenLCompilationException(String.format("Dependency with name '%s' wasn't found",
                    dependencyName), null, dependency.getNode().getSourceLocation());
            }
        }
        return compiledDependency;
    }

    private Deque<String> moduleCompilationStack = new ArrayDeque<String>();
    private Map<String, ClassLoader> classLoaders = new HashMap<String, ClassLoader>();

    public Deque<String> getCompilationStack() {
        return moduleCompilationStack;
    }

    protected abstract Collection<ProjectDescriptor> getProjectDescriptors();

    public ClassLoader getClassLoader(ProjectDescriptor project) {
        getDependencyLoaders();
        if (classLoaders.get(project.getName()) != null) {
            return classLoaders.get(project.getName());
        }
        SimpleBundleClassLoader classLoader = new SimpleBundleClassLoader(AbstractProjectDependencyManager.class.getClassLoader());
        URL[] urls = project.getClassPathUrls();
        classLoader.addClassLoader(project.getClassLoader(false));
        OpenLClassLoaderHelper.extendClasspath(classLoader, urls);
        if (project.getDependencies() != null) {
            for (ProjectDependencyDescriptor projectDependencyDescriptor : project.getDependencies()) {
                if (getProjectDescriptors() != null) {
                    for (ProjectDescriptor projectDescriptor : getProjectDescriptors()) {
                        if (projectDependencyDescriptor.getName().equals(projectDescriptor.getName())) {
                            classLoader.addClassLoader(getClassLoader(projectDescriptor));
                            break;
                        }
                    }
                }
            }
        }

        classLoaders.put(project.getName(), classLoader);
        return classLoader;
    }

    public void clearClassLoader(String projectName) {
        classLoaders.remove(projectName);
    }

    public void clearAllClassLoader() {
        classLoaders.clear();
    }

    public abstract void reset(IDependency dependency);

    public abstract void resetAll();

}
