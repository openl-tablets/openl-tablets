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

    public WebStudioWorkspaceRelatedDependencyManager(List<ProjectDescriptor> dependentProjects) {
        if (dependentProjects == null) {
            throw new IllegalArgumentException("dependentProjects can't be null!");
        }

        this.dependentProjects = dependentProjects;
    }

    // Disable cache of compiled dependencies. Use ehcache in loaders.
    @Override
    public CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
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

    @Override
    public synchronized List<IDependencyLoader> getDependencyLoaders() {
        if (dependencyLoaders != null) {
            return dependencyLoaders;
        }
        dependencyLoaders = new ArrayList<IDependencyLoader>();
        for (ProjectDescriptor project : dependentProjects) {
            try {
                Collection<Module> modulesOfProject = project.getModules();
                if (!modulesOfProject.isEmpty()) {
                    for (final Module m : modulesOfProject) {
                        dependencyLoaders.add(new WebStudioDependencyLoader(m.getName(), Arrays.asList(m)));
                    }
                }

                String dependencyName = ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(project.getName());
                IDependencyLoader projectLoader = new WebStudioDependencyLoader(dependencyName, project.getModules());
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
        if (dependencyLoaders == null) {
            return;
        }

        String dependencyName = dependency.getNode().getIdentifier();

        for (IDependencyLoader dependencyLoader : dependencyLoaders) {
            WebStudioDependencyLoader loader = (WebStudioDependencyLoader) dependencyLoader;
            if (dependencyName.equals(loader.getDependencyName())) {
                loader.reset();
                break;
            }
        }
    }

    @Override
    public void resetAll() {
        if (dependencyLoaders == null) {
            return;
        }

        for (IDependencyLoader dependencyLoader : dependencyLoaders) {
            ((WebStudioDependencyLoader) dependencyLoader).reset();
        }
    }
}
