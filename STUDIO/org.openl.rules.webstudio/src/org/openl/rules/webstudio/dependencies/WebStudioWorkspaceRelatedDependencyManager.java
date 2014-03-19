package org.openl.rules.webstudio.dependencies;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.IDependency;

import java.net.URL;
import java.util.*;

public class WebStudioWorkspaceRelatedDependencyManager extends DependencyManager {

    private final Log log = LogFactoryImpl.getLog(WebStudioWorkspaceRelatedDependencyManager.class);

    private List<IDependencyLoader> dependencyLoaders;

    private List<ProjectDescriptor> dependentProjects;
    private boolean singleModuleMode;

    private final List<DependencyManagerListener> listeners = new ArrayList<DependencyManagerListener>();

    public WebStudioWorkspaceRelatedDependencyManager(List<ProjectDescriptor> dependentProjects, boolean singleModuleMode) {
        if (dependentProjects == null) {
            throw new IllegalArgumentException("dependentProjects can't be null!");
        }

        this.dependentProjects = dependentProjects;
        this.singleModuleMode = singleModuleMode;
    }

    // Disable cache of compiled dependencies. Use ehcache in loaders.
    @Override
    public synchronized CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        for (DependencyManagerListener listener : listeners) {
            listener.onLoadDependency(dependency);
        }

        String dependencyName = dependency.getNode().getIdentifier();
        CompiledDependency compiledDependency = handleLoadDependency(dependency);
        if (compiledDependency == null) {
            throw new OpenLCompilationException(String.format("Dependency with name '%s' wasn't found", dependencyName),
                null,
                dependency.getNode().getSourceLocation());
        }
        return compiledDependency;
    }

    private Deque<String> stack = new ArrayDeque<String>();
    private Map<String, ClassLoader> classLoaders = new HashMap<String, ClassLoader>();

    Deque<String> getStack() {
        return stack;
    }

    public ClassLoader getClassLoader(Collection<Module> modules) {
        Set<String> projectNames = new HashSet<String>();
        for (Module module : modules) {
            projectNames.add(module.getProject().getName());
        }
        if (projectNames.size() == 1) {
            String pn = projectNames.iterator().next();
            if (classLoaders.get(pn) != null) {
                return classLoaders.get(pn);
            }
            SimpleBundleClassLoader classLoader = new SimpleBundleClassLoader(WebStudioWorkspaceRelatedDependencyManager.class.getClassLoader());
            for (Module module : modules) {
                URL[] urls = module.getProject().getClassPathUrls();
                classLoader.addClassLoader(module.getProject().getClassLoader(false));
                OpenLClassLoaderHelper.extendClasspath(classLoader, urls);
            }
            classLoaders.put(pn, classLoader);
            return classLoader;
        }
        throw new IllegalStateException();
    }

    public void removeClassLoader(String projectName) {
        classLoaders.remove(projectName);
    }

    @Override
    public List<IDependencyLoader> getDependencyLoaders() {
        if (dependencyLoaders != null) {
            return dependencyLoaders;
        }
        dependencyLoaders = new ArrayList<IDependencyLoader>();
        for (ProjectDescriptor project : dependentProjects) {
            try {
                Collection<Module> modulesOfProject = project.getModules();
                if (!modulesOfProject.isEmpty()) {
                    for (final Module m : modulesOfProject) {
                        dependencyLoaders.add(new WebStudioDependencyLoader(m.getName(), Arrays.asList(m), singleModuleMode));
                    }
                }

                String dependencyName = ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(project.getName());
                IDependencyLoader projectLoader = new WebStudioDependencyLoader(dependencyName, project.getModules(), singleModuleMode);
                dependencyLoaders.add(projectLoader);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    String message = String.format("Build dependency manager loaders for project %s was failed!", project.getName());
                    log.error(message, e);
                }
            }
        }

        return dependencyLoaders;
    }

    @Override
    public void reset(IDependency dependency) {
        for (DependencyManagerListener listener : listeners) {
            listener.onResetDependency(dependency);
        }

        if (dependencyLoaders == null) {
            return;
        }

        String dependencyName = dependency.getNode().getIdentifier();

        ProjectDescriptor projectToReset = null;

        searchProject:
        for (ProjectDescriptor project : dependentProjects) {
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
            removeClassLoader(projectToReset.getName());
            String projectDependency = ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(projectToReset.getName());

            for (IDependencyLoader dependencyLoader : dependencyLoaders) {
                WebStudioDependencyLoader loader = (WebStudioDependencyLoader) dependencyLoader;
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

        classLoaders.clear();

        for (IDependencyLoader dependencyLoader : dependencyLoaders) {
            ((WebStudioDependencyLoader) dependencyLoader).reset();
        }
    }

    public void addListener(DependencyManagerListener listener) {
        for (DependencyManagerListener l : listeners) {
            if (l == listener) {
                // Already added
                return;
            }
        }
        listeners.add(listener);
    }

    public void removeListener(DependencyManagerListener listener) {
        for (Iterator<DependencyManagerListener> iterator = listeners.iterator(); iterator.hasNext(); ) {
            DependencyManagerListener next = iterator.next();
            if (next == listener) {
                iterator.remove();
            }
        }
    }

}
