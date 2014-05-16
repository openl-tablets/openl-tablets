package org.openl.rules.project.instantiation;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.model.Module;
import org.openl.syntax.code.IDependency;

public abstract class AbstractProjectDependencyManager extends DependencyManager {

    protected List<IDependencyLoader> dependencyLoaders;

    //Disable cache. if cache required it should be used in loaders.
    @Override
    public synchronized CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        String dependencyName = dependency.getNode().getIdentifier();
        CompiledDependency compiledDependency = handleLoadDependency(dependency);
        if (compiledDependency == null) {
            throw new OpenLCompilationException(String.format("Dependency with name '%s' wasn't found", dependencyName),
                null,
                dependency.getNode().getSourceLocation());
        }
        return compiledDependency;
    }

    private Deque<String> moduleCompilationStack = new ArrayDeque<String>();
    private Map<String, ClassLoader> classLoaders = new HashMap<String, ClassLoader>();

    public Deque<String> getModuleCompilationStack() {
        return moduleCompilationStack;
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
            SimpleBundleClassLoader classLoader = new SimpleBundleClassLoader(AbstractProjectDependencyManager.class.getClassLoader());
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

    public void clearClassLoader(String projectName) {
        classLoaders.remove(projectName);
    }
    
    public void clearAllClassLoader() {
        classLoaders.clear();
    }

    public abstract void reset(IDependency dependency);
    public abstract void resetAll();

}
