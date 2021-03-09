package org.openl.rules.project.instantiation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.openl.OpenClassUtil;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDependencyManager implements IDependencyManager {

    private final Logger log = LoggerFactory.getLogger(AbstractDependencyManager.class);

    private volatile Map<String, Collection<IDependencyLoader>> dependencyLoaders;
    private final Object dependencyLoadersFlag = new Object();
    private final LinkedHashSet<DependencyRelation> dependencyRelations = new LinkedHashSet<>();
    private final ThreadLocal<Deque<String>> compilationStackThreadLocal = ThreadLocal.withInitial(ArrayDeque::new);
    private final Map<String, ClassLoader> externalJarsClassloaders = new HashMap<>();
    private final ClassLoader rootClassLoader;
    protected boolean executionMode;
    private Map<String, Object> externalParameters;

    public static class DependencyRelation {
        String dependOnThisDependency;
        String dependency;

        public DependencyRelation(String dependency, String dependOnThisDependency) {
            this.dependency = dependency;
            this.dependOnThisDependency = dependOnThisDependency;
        }

        public String getDependency() {
            return dependency;
        }

        public String getDependOnThisDependency() {
            return dependOnThisDependency;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + (dependOnThisDependency == null ? 0 : dependOnThisDependency.hashCode());
            result = 31 * result + (dependency == null ? 0 : dependency.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            DependencyRelation other = (DependencyRelation) obj;
            if (dependOnThisDependency == null) {
                if (other.dependOnThisDependency != null) {
                    return false;
                }
            } else if (!dependOnThisDependency.equals(other.dependOnThisDependency)) {
                return false;
            }
            if (dependency == null) {
                return other.dependency == null;
            } else {
                return dependency.equals(other.dependency);
            }
        }

        @Override
        public String toString() {
            return String.format("DependencyReference [dependOnThisDependency=%s, dependency=%s]",
                dependOnThisDependency,
                dependency);
        }
    }

    protected AbstractDependencyManager(ClassLoader rootClassLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        this.rootClassLoader = rootClassLoader;
        this.executionMode = executionMode;
        this.externalParameters = new HashMap<>();
        if (externalParameters != null) {
            this.externalParameters.putAll(externalParameters);
        }
        this.externalParameters = Collections.unmodifiableMap(this.externalParameters);
    }

    public Collection<IDependencyLoader> findDependencyLoadersByName(String dependencyName) {
        Collection<IDependencyLoader> dependencyLoaders = getDependencyLoaders().get(dependencyName);
        if (dependencyLoaders != null) {
            return Collections.unmodifiableCollection(dependencyLoaders);
        }
        return null;
    }

    public Collection<IDependencyLoader> getAllDependencyLoaders() {
        return Collections.unmodifiableCollection(
            getDependencyLoaders().values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
    }

    protected void addDependencyLoaders(Collection<IDependencyLoader> dependencyLoadersToAdd) {
        if (dependencyLoadersToAdd != null) {
            synchronized (dependencyLoadersFlag) {
                for (IDependencyLoader dependencyLoader : dependencyLoadersToAdd) {
                    Collection<IDependencyLoader> dependencyLoadersByDependencyName = this.dependencyLoaders
                        .computeIfAbsent(dependencyLoader.getDependencyName(), e -> new CopyOnWriteArrayList<>());
                    boolean f = false;
                    for (IDependencyLoader dl : dependencyLoadersByDependencyName) {
                        if (dl.isProjectLoader() && dependencyLoader.isProjectLoader() && Objects
                            .equals(dl.getProject().getName(), dependencyLoader.getProject().getName())) {
                            f = true;
                            break;
                        } else if (!dl.isProjectLoader() && !dependencyLoader.isProjectLoader() && dl
                            .getModule() != null && dependencyLoader.getModule() != null && Objects
                                .equals(dl.getModule().getName(), dependencyLoader.getModule().getName()) && Objects
                                    .equals(dl.getModule().getProject().getName(),
                                        dependencyLoader.getModule().getProject().getName())) {
                            f = true;
                            break;
                        }
                    }
                    if (!f) {
                        dependencyLoadersByDependencyName.add(dependencyLoader);
                    }
                }
            }
        }
    }

    private Map<String, Collection<IDependencyLoader>> getDependencyLoaders() {
        if (dependencyLoaders == null) {
            synchronized (this) {
                if (dependencyLoaders == null) {
                    Map<String, Collection<IDependencyLoader>> initDependencyLoaders = initDependencyLoaders();
                    if (initDependencyLoaders != null) {
                        Map<String, Collection<IDependencyLoader>> modifiableAndConcurrentDependencyLoaders = new ConcurrentHashMap<>();
                        for (Map.Entry<String, Collection<IDependencyLoader>> entry : initDependencyLoaders
                            .entrySet()) {
                            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                                Collection<IDependencyLoader> c = modifiableAndConcurrentDependencyLoaders
                                    .computeIfAbsent(entry.getKey(), e -> new CopyOnWriteArrayList<>());
                                c.addAll(entry.getValue());
                            }
                        }
                        dependencyLoaders = initDependencyLoaders;
                    } else {
                        dependencyLoaders = new HashMap<>();
                    }
                }
            }
        }
        return dependencyLoaders;
    }

    protected abstract Map<String, Collection<IDependencyLoader>> initDependencyLoaders();

    private Deque<String> getCompilationStack() {
        return compilationStackThreadLocal.get();
    }

    @Override
    public Collection<String> getAllDependencies() {
        return Collections.unmodifiableSet(getDependencyLoaders().keySet());
    }

    @Override
    public final Collection<String> getAvailableDependencies() {
        Set<String> availableDependencies = new HashSet<>();
        for (Map.Entry<String, Collection<IDependencyLoader>> entry : getDependencyLoaders().entrySet()) {
            if (entry.getValue().stream().noneMatch(IDependencyLoader::isProjectLoader)) {
                availableDependencies.add(entry.getKey());
            }
        }
        return availableDependencies;
    }

    // Disable cache. if cache required it should be used in loaders.
    @Override
    public synchronized CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        final IDependencyLoader dependencyLoader = findDependencyLoadersByName(dependency);
        final String dependencyName = dependency.getNode().getIdentifier();
        Deque<String> compilationStack = getCompilationStack();
        try {
            if (log.isDebugEnabled()) {
                log.debug(
                    compilationStack
                        .contains(dependencyName) ? "Dependency '{}' in the compilation stack."
                                                  : "Dependency '{}' is not found in the compilation stack.",
                    dependencyName);
            }
            boolean isCircularDependency = !dependencyLoader.isProjectLoader() && compilationStack
                .contains(dependencyName);
            if (!isCircularDependency && !compilationStack.isEmpty()) {
                DependencyRelation dr = new DependencyRelation(getCompilationStack().getFirst(), dependencyName);
                this.addDependencyRelation(dr);
            }

            if (isCircularDependency) {
                throw new OpenLCompilationException(String.format("Circular dependency is detected: %s.",
                    buildCircularDependencyDetails(dependencyName, compilationStack)));
            }

            CompiledDependency compiledDependency;
            try {
                compilationStack.push(dependencyName);
                log.debug("Dependency '{}' is added to the compilation stack.", dependencyName);
                compiledDependency = dependencyLoader.getCompiledDependency();
            } finally {
                compilationStack.poll();
                log.debug("Dependency '{}' is removed from the compilation stack.", dependencyName);
            }

            if (compiledDependency == null) {
                if (dependencyLoader.isProjectLoader()) {
                    return throwCompilationError(dependency, dependencyLoader.getProject().getName());
                } else {
                    return throwCompilationError(dependency, dependencyName);
                }
            }
            return compiledDependency;
        } finally {
            if (compilationStack.isEmpty()) {
                compilationStackThreadLocal.remove(); // Clean thread
            }
        }
    }

    protected IDependencyLoader findDependencyLoadersByName(IDependency dependency) throws OpenLCompilationException {
        final String dependencyName = dependency.getNode().getIdentifier();
        Collection<IDependencyLoader> loaders = getDependencyLoaders().get(dependencyName);
        if (loaders == null || loaders.isEmpty()) {
            throw new OpenLCompilationException(String.format("Dependency '%s' is not found.", dependencyName),
                null,
                dependency.getNode().getSourceLocation());
        }
        if (loaders.size() > 1) {
            throw new OpenLCompilationException(
                String.format("Multiple modules with the same name '%s' are found.", dependencyName));
        }
        return loaders.iterator().next();
    }

    private static String buildCircularDependencyDetails(String dependencyName, Deque<String> compilationStack) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = compilationStack.iterator();
        sb.append("'").append(dependencyName).append("'");
        while (itr.hasNext()) {
            String s = itr.next();
            sb.insert(0, "' -> ");
            sb.insert(0, s);
            sb.insert(0, "'");
            if (Objects.equals(dependencyName, s)) {
                break;
            }
        }
        return sb.toString();
    }

    private CompiledDependency throwCompilationError(IDependency dependency,
            String dependencyName) throws OpenLCompilationException {
        IdentifierNode node = dependency.getNode();
        throw new OpenLCompilationException(String.format("Dependency '%s' is not found.", dependencyName),
            null,
            node.getSourceLocation(),
            node.getModule());
    }

    public synchronized ClassLoader getExternalJarsClassLoader(ProjectDescriptor project) {
        getDependencyLoaders(); // Init dependency loaders
        if (externalJarsClassloaders.get(project.getName()) != null) {
            return externalJarsClassloaders.get(project.getName());
        }
        ClassLoader parentClassLoader = rootClassLoader == null ? this.getClass().getClassLoader() : rootClassLoader;
        OpenLClassLoader externalJarsClassloader = new OpenLClassLoader(project.getClassPathUrls(), parentClassLoader);
        // To load classes from dependency jars first
        if (project.getDependencies() != null) {
            for (ProjectDependencyDescriptor projectDependencyDescriptor : project.getDependencies()) {
                for (ProjectDescriptor projectDescriptor : getProjectDescriptors()) {
                    if (projectDependencyDescriptor.getName().equals(projectDescriptor.getName())) {
                        externalJarsClassloader.addClassLoader(getExternalJarsClassLoader(projectDescriptor));
                        break;
                    }
                }
            }
        }
        externalJarsClassloaders.put(project.getName(), externalJarsClassloader);
        return externalJarsClassloader;
    }

    public Collection<ProjectDescriptor> getProjectDescriptors() {
        return getDependencyLoaders().values()
            .stream()
            .flatMap(Collection::stream)
            .filter(IDependencyLoader::isProjectLoader)
            .map(IDependencyLoader::getProject)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public synchronized void resetOthers(IDependency... dependencies) {
        if (dependencies == null || dependencies.length == 0) {
            return;
        }
        Set<String> dependenciesToKeep = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        for (IDependency dependency : dependencies) {
            if (dependency != null) {
                queue.add(dependency.getNode().getIdentifier());
                dependenciesToKeep.add(dependency.getNode().getIdentifier());
            }
        }
        while (!queue.isEmpty()) {
            String depName = queue.poll();
            for (DependencyRelation dependencyReference : dependencyRelations) {
                if (dependencyReference.getDependency().equals(depName)) {
                    if (dependenciesToKeep.add(dependencyReference.getDependOnThisDependency())) {
                        queue.add(dependencyReference.getDependOnThisDependency());
                    }
                }
            }
        }
        for (String depName : getAllDependencies()) {
            if (!dependenciesToKeep.contains(depName)) {
                reset(new Dependency(DependencyType.MODULE,
                    new IdentifierNode(DependencyType.MODULE.name(), null, depName, null)));
            }
        }
    }

    @Override
    public synchronized void reset(IDependency dependency) {
        if (dependency == null) {
            return;
        }
        final String dependencyName = dependency.getNode().getIdentifier();
        Set<String> dependenciesToReset = new HashSet<>();
        Set<DependencyRelation> dependenciesReferencesToRemove = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(dependencyName);
        dependenciesToReset.add(dependencyName);
        while (!queue.isEmpty()) {
            String depName = queue.poll();
            for (DependencyRelation dependencyReference : dependencyRelations) {
                if (dependencyReference.getDependOnThisDependency().equals(depName)) {
                    if (dependenciesToReset.add(dependencyReference.getDependency())) {
                        queue.add(dependencyReference.getDependency());
                    }
                }
                if (dependencyReference.getDependency().equals(depName)) {
                    dependenciesReferencesToRemove.add(dependencyReference);
                }
            }
        }
        for (String dependencyNameToReset : dependenciesToReset) {
            Collection<IDependencyLoader> loaders = getDependencyLoaders().get(dependencyNameToReset);
            if (loaders != null) {
                for (IDependencyLoader loader : loaders) {
                    if (loader.getRefToCompiledDependency() != null) {
                        log.debug("Dependency '{}' is reset.", dependencyNameToReset);
                    }
                    loader.reset();
                }
                loaders.forEach(IDependencyLoader::reset);
            }
        }
        for (DependencyRelation dependencyReference : dependenciesReferencesToRemove) {
            dependencyRelations.remove(dependencyReference);
        }
    }

    @Override
    public synchronized void resetAll() {
        for (ClassLoader classLoader : externalJarsClassloaders.values()) {
            OpenClassUtil.releaseClassLoader(classLoader);
        }
        externalJarsClassloaders.clear();
        for (Collection<IDependencyLoader> loaders : getDependencyLoaders().values()) {
            loaders.forEach(IDependencyLoader::reset);
        }
        dependencyRelations.clear();
    }

    protected synchronized void addDependencyRelation(DependencyRelation dependencyRelation) {
        dependencyRelations.add(dependencyRelation);
    }

    /**
     * In execution mode all meta info that is not used in rules running is being cleaned.
     */
    @Override
    public boolean isExecutionMode() {
        return executionMode;
    }

    @Override
    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

}
