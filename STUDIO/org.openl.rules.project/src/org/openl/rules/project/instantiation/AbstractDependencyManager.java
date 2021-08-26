package org.openl.rules.project.instantiation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openl.OpenClassUtil;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.AmbiguousDependencyException;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyNotFoundException;
import org.openl.dependency.DependencyType;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.ResolvedDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDependencyManager implements IDependencyManager {

    private static final Pattern ASTERISK_SIGN = Pattern.compile("\\*");
    private static final Pattern QUESTION_SIGN = Pattern.compile("\\?");
    private static final Pattern SLASH_SIGN = Pattern.compile("\\s*/\\s*");

    private final Logger log = LoggerFactory.getLogger(AbstractDependencyManager.class);

    private volatile CopyOnWriteArraySet<IDependencyLoader> dependencyLoaders;
    private final Object dependencyLoadersFlag = new Object();
    private final LinkedHashSet<DependencyRelation> dependencyRelations = new LinkedHashSet<>();
    private final ThreadLocal<Deque<IDependencyLoader>> compilationStackThreadLocal = ThreadLocal
        .withInitial(ArrayDeque::new);
    private final Map<ProjectDescriptor, ClassLoader> externalJarsClassloaders = new HashMap<>();
    private final ClassLoader rootClassLoader;
    protected boolean executionMode;
    private Map<String, Object> externalParameters;

    public static ResolvedDependency buildResolvedDependency(String projectName) {
        return buildResolvedDependency(projectName, null);
    }

    public static ResolvedDependency buildResolvedDependency(String projectName, String moduleName) {
        if (moduleName == null) {
            return new ResolvedDependency(DependencyType.PROJECT, new IdentifierNode(null, null, projectName, null));
        }
        return new ResolvedDependency(DependencyType.MODULE,
            new IdentifierNode(null, null, projectName + "/" + moduleName, null));
    }

    public static ResolvedDependency buildResolvedDependency(ProjectDescriptor project) {
        return new ResolvedDependency(DependencyType.PROJECT, new IdentifierNode(null, null, project.getName(), null));
    }

    public static ResolvedDependency buildResolvedDependency(Module module) {
        return buildResolvedDependency(module.getProject().getName(), module.getName());
    }

    public static class DependencyRelation {
        IDependencyLoader dependOnThisDependency;
        IDependencyLoader dependency;

        public DependencyRelation(IDependencyLoader dependency, IDependencyLoader dependOnThisDependency) {
            this.dependency = dependency;
            this.dependOnThisDependency = dependOnThisDependency;
        }

        public IDependencyLoader getDependency() {
            return dependency;
        }

        public IDependencyLoader getDependOnThisDependency() {
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
                dependOnThisDependency.getDependency(),
                dependency.getDependency());
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

    protected void addDependencyLoaders(Collection<IDependencyLoader> dependencyLoadersToAdd) {
        if (dependencyLoadersToAdd != null) {
            synchronized (dependencyLoadersFlag) {
                dependencyLoaders.addAll(dependencyLoadersToAdd);
            }
        }
    }

    public Collection<IDependencyLoader> getDependencyLoaders() {
        if (dependencyLoaders == null) {
            synchronized (this) {
                if (dependencyLoaders == null) {
                    dependencyLoaders = new CopyOnWriteArraySet<>(initDependencyLoaders());
                }
            }
        }
        return Collections.unmodifiableSet(dependencyLoaders);
    }

    protected abstract Set<IDependencyLoader> initDependencyLoaders();

    private Deque<IDependencyLoader> getCompilationStack() {
        return compilationStackThreadLocal.get();
    }

    // Disable cache. if cache required it should be used in loaders.
    @Override
    public synchronized CompiledDependency loadDependency(
            ResolvedDependency dependency) throws OpenLCompilationException {
        final IDependencyLoader dependencyLoader = findDependencyLoaderByDependency(dependency);
        Deque<IDependencyLoader> compilationStack = getCompilationStack();
        try {
            if (log.isDebugEnabled()) {
                log.debug(
                    compilationStack
                        .contains(dependencyLoader) ? "Dependency '{}' in the compilation stack."
                                                    : "Dependency '{}' is not found in the compilation stack.",
                    dependency);
            }
            boolean isCircularDependency = compilationStack.contains(dependencyLoader);
            if (!isCircularDependency && !compilationStack.isEmpty()) {
                DependencyRelation dr = new DependencyRelation(getCompilationStack().getFirst(), dependencyLoader);
                this.addDependencyRelation(dr);
            }

            if (isCircularDependency) {
                throw new OpenLCompilationException(
                    String.format("Circular dependency is detected: %s.",
                        buildCircularDependencyDetails(dependencyLoader, compilationStack)),
                    null,
                    dependency.getNode().getSourceLocation(),
                    dependency.getNode().getModule());
            }

            CompiledDependency compiledDependency;
            try {
                compilationStack.push(dependencyLoader);
                log.debug("Dependency '{}' is added to the compilation stack.", dependencyLoader.getDependency());
                compiledDependency = dependencyLoader.getCompiledDependency();
            } finally {
                compilationStack.poll();
                log.debug("Dependency '{}' is removed from the compilation stack.", dependencyLoader.getDependency());
            }

            if (compiledDependency == null) {
                return throwDependencyNotFoundError(dependency);
            }
            return compiledDependency;
        } finally {
            if (compilationStack.isEmpty()) {
                compilationStackThreadLocal.remove(); // Clean thread
            }
        }
    }

    public IDependencyLoader findDependencyLoader(ResolvedDependency dependency) {
        return getDependencyLoaders().stream()
            .filter(
                e -> DependencyType.ANY.equals(dependency.getType())
                                                                     ? Objects.equals(dependency.getNode(),
                                                                         e.getDependency().getNode())
                                                                     : Objects.equals(dependency, e.getDependency()))
            .findFirst()
            .orElse(null);
    }

    public Collection<IDependencyLoader> findAllProjectDependencyLoaders(ProjectDescriptor project) {
        Collection<IDependencyLoader> dependencyLoadersForProject = new HashSet<>();
        Deque<ProjectDescriptor> queue = new ArrayDeque<>();
        queue.add(project);
        while (!queue.isEmpty()) {
            ProjectDescriptor projectDescriptor = queue.poll();
            getDependencyLoaders().stream()
                .filter(e -> Objects.equals(e.getProject(), projectDescriptor))
                .forEach(dependencyLoadersForProject::add);
            if (projectDescriptor.getDependencies() != null) {
                for (ProjectDependencyDescriptor pdd : projectDescriptor.getDependencies()) {
                    IDependencyLoader dl = this.findDependencyLoader(buildResolvedDependency(pdd.getName()));
                    if (dl != null && dl.isProjectLoader()) {
                        queue.add(dl.getProject());
                    }
                }
            }
        }
        return dependencyLoadersForProject;
    }

    @Override
    public Collection<ResolvedDependency> resolveDependency(IDependency dependency,
            boolean withWildcardSupport) throws AmbiguousDependencyException, DependencyNotFoundException {
        String value = dependency.getNode().getIdentifier().trim();
        boolean withWildcard;
        if (withWildcardSupport) {
            withWildcard = ASTERISK_SIGN.matcher(value).find() || QUESTION_SIGN.matcher(value).find();
            value = ASTERISK_SIGN.matcher(value).replaceAll("\\\\E.*\\\\Q");
            value = QUESTION_SIGN.matcher(value).replaceAll("\\\\E.\\\\Q");
            value = SLASH_SIGN.matcher(value).replaceAll("\\\\E\\\\s*/\\\\s*\\\\Q");
            value = "\\Q" + value + "\\E";
        } else {
            withWildcard = false;
        }
        IDependencyLoader currentDependencyLoader = !getCompilationStack().isEmpty() ? getCompilationStack().getFirst()
                                                                                     : null;
        Collection<IDependencyLoader> visibleDependencyLoaders = currentDependencyLoader != null ? findAllProjectDependencyLoaders(
            currentDependencyLoader.getProject()) : getDependencyLoaders();

        // Filter by dependency type
        if (DependencyType.PROJECT.equals(dependency.getType())) {
            visibleDependencyLoaders = visibleDependencyLoaders.stream()
                .filter(IDependencyLoader::isProjectLoader)
                .collect(Collectors.toSet());
        } else if (DependencyType.MODULE.equals(dependency.getType())) {
            visibleDependencyLoaders = visibleDependencyLoaders.stream()
                .filter(e -> !e.isProjectLoader())
                .collect(Collectors.toSet());
        }

        Set<IDependencyLoader> dependencyLoaders = new HashSet<>();
        for (IDependencyLoader dl : visibleDependencyLoaders) {
            if (!Objects.equals(currentDependencyLoader,
                dl) && !(dl.isProjectLoader() && currentDependencyLoader != null && Objects.equals(dl.getProject(),
                    currentDependencyLoader.getProject()))) {
                if (Pattern.matches(value, dl.getDependency().getNode().getIdentifier())) {
                    dependencyLoaders.add(dl);
                }
            }
        }

        for (IDependencyLoader dl : visibleDependencyLoaders) {
            if (!dl.isProjectLoader()) {
                if (!dl.isProjectLoader() && !Objects.equals(currentDependencyLoader, dl) && Pattern.matches(value,
                    dl.getModule().getName())) {
                    dependencyLoaders.add(dl);
                }
            }
        }

        if (dependencyLoaders.stream().anyMatch(e -> !e.isProjectLoader())) {
            dependencyLoaders = dependencyLoaders.stream()
                .filter(e -> !e.isProjectLoader())
                .collect(Collectors.toSet());
        }

        Collection<ResolvedDependency> ret = dependencyLoaders.stream()
            .map(e -> new ResolvedDependency(e.isProjectLoader() ? DependencyType.PROJECT : DependencyType.MODULE,
                new IdentifierNode(dependency.getNode().getType(),
                    dependency.getNode().getLocation(),
                    e.getDependency().getNode().getIdentifier(),
                    null)))
            .collect(Collectors.toSet());

        if (!withWildcard && ret.size() != 1) {
            if (ret.isEmpty()) {
                throw new DependencyNotFoundException(String.format("Dependency '%s' is not found.",
                    dependency.getNode().getIdentifier()), null, dependency.getNode().getSourceLocation(), null);
            } else {
                throw new AmbiguousDependencyException(
                    String.format("Multiple dependencies '%s' are found.", dependency.getNode().getIdentifier()),
                    null,
                    dependency.getNode().getSourceLocation(),
                    dependency.getNode().getModule());
            }
        }
        return ret;
    }

    protected IDependencyLoader findDependencyLoaderByDependency(
            ResolvedDependency dependency) throws OpenLCompilationException {
        IDependencyLoader dependencyLoader = findDependencyLoader(dependency);
        if (dependencyLoader == null) {
            throw new OpenLCompilationException(
                String.format("Dependency '%s' is not found.", dependency.getNode().getIdentifier()),
                null,
                dependency.getNode().getSourceLocation(),
                dependency.getNode().getModule());
        }
        return dependencyLoader;
    }

    private static String buildCircularDependencyDetails(IDependencyLoader dependencyLoader,
            Deque<IDependencyLoader> compilationStack) {
        StringBuilder sb = new StringBuilder();

        Map<String, List<Module>> p = compilationStack.stream()
            .filter(e -> !e.isProjectLoader())
            .map(IDependencyLoader::getModule)
            .collect(Collectors.groupingBy(Module::getName));

        Iterator<IDependencyLoader> itr = compilationStack.iterator();
        sb.append("'");
        sb.insert(0,
            !dependencyLoader.isProjectLoader() && p.get(dependencyLoader.getModule().getName())
                .size() == 1 ? dependencyLoader.getModule().getName() : dependencyLoader.getDependency());
        while (itr.hasNext()) {
            IDependencyLoader s = itr.next();
            sb.insert(0, "' -> '");
            sb.insert(0,
                !dependencyLoader.isProjectLoader() && p.get(s.getModule().getName()).size() == 1
                                                                                                  ? s.getModule()
                                                                                                      .getName()
                                                                                                  : s.getDependency());
            if (Objects.equals(dependencyLoader, s)) {
                break;
            }
        }
        sb.insert(0, "'");
        return sb.toString();

    }

    private CompiledDependency throwDependencyNotFoundError(IDependency dependency) throws OpenLCompilationException {
        IdentifierNode node = dependency.getNode();
        throw new OpenLCompilationException(String.format("Dependency '%s' is not found.", node.getIdentifier()),
            null,
            node.getSourceLocation(),
            node.getModule());
    }

    public synchronized ClassLoader getExternalJarsClassLoader(ProjectDescriptor project) {
        Set<ProjectDescriptor> breadcrumbs = new HashSet<>();
        breadcrumbs.add(project);
        return getExternalJarsClassLoaderRec(project, breadcrumbs);
    }

    public synchronized ClassLoader getExternalJarsClassLoaderRec(ProjectDescriptor project, Set<ProjectDescriptor> breadcrumbs) {
        getDependencyLoaders(); // Init dependency loaders
        if (externalJarsClassloaders.get(project) != null) {
            return externalJarsClassloaders.get(project);
        }
        ClassLoader parentClassLoader = rootClassLoader == null ? this.getClass().getClassLoader() : rootClassLoader;
        OpenLClassLoader externalJarsClassloader = new OpenLClassLoader(project.getClassPathUrls(), parentClassLoader);
        // To load classes from dependency jars first
        if (project.getDependencies() != null) {
            Collection<IDependencyLoader> projectDependencyLoaders = getDependencyLoaders().stream()
                    .filter(IDependencyLoader::isProjectLoader)
                    .collect(Collectors.toCollection(ArrayList::new));
            for (ProjectDependencyDescriptor projectDependencyDescriptor : project.getDependencies()) {
                for (IDependencyLoader dl : projectDependencyLoaders) {
                    if (Objects.equals(projectDependencyDescriptor.getName(), dl.getProject().getName())) {
                        if (!breadcrumbs.contains(dl.getProject())) {
                            breadcrumbs.add(dl.getProject());
                            externalJarsClassloader.addClassLoader(getExternalJarsClassLoaderRec(dl.getProject(), breadcrumbs));
                            breadcrumbs.remove(dl.getProject());
                        }
                        break;
                    }
                }
            }
        }
        externalJarsClassloaders.put(project, externalJarsClassloader);
        return externalJarsClassloader;
    }

    @Override
    public synchronized void resetOthers(ResolvedDependency... dependencies) {
        if (dependencies == null || dependencies.length == 0) {
            return;
        }
        Set<IDependencyLoader> dependenciesToKeep = new HashSet<>();
        Deque<IDependencyLoader> queue = new ArrayDeque<>();
        for (ResolvedDependency dependency : dependencies) {
            if (dependency != null) {
                IDependencyLoader dependencyLoader = findDependencyLoader(dependency);
                queue.add(dependencyLoader);
                dependenciesToKeep.add(dependencyLoader);
            }
        }
        while (!queue.isEmpty()) {
            IDependencyLoader depLoader = queue.poll();
            for (DependencyRelation dependencyReference : dependencyRelations) {
                if (dependencyReference.getDependency().equals(depLoader)) {
                    if (dependenciesToKeep.add(dependencyReference.getDependOnThisDependency())) {
                        queue.add(dependencyReference.getDependOnThisDependency());
                    }
                }
            }
        }
        for (IDependencyLoader depLoader : getDependencyLoaders()) {
            if (!dependenciesToKeep.contains(depLoader)) {
                reset(depLoader.getDependency());
            }
        }
    }

    @Override
    public synchronized void reset(ResolvedDependency dependency) {
        if (dependency == null) {
            return;
        }
        IDependencyLoader dependencyLoader = findDependencyLoader(dependency);
        Set<DependencyRelation> dependenciesReferencesToRemove = new HashSet<>();
        Deque<IDependencyLoader> queue = new ArrayDeque<>();
        queue.add(dependencyLoader);
        Set<IDependencyLoader> dependenciesToReset = new HashSet<>();
        dependenciesToReset.add(dependencyLoader);
        while (!queue.isEmpty()) {
            IDependencyLoader depLoader = queue.poll();
            for (DependencyRelation dependencyReference : dependencyRelations) {
                if (dependencyReference.getDependOnThisDependency().equals(depLoader)) {
                    if (dependenciesToReset.add(dependencyReference.getDependency())) {
                        queue.add(dependencyReference.getDependency());
                    }
                }
                if (dependencyReference.getDependency().equals(depLoader)) {
                    dependenciesReferencesToRemove.add(dependencyReference);
                }
            }
        }
        for (IDependencyLoader dependencyToReset : dependenciesToReset) {
            if (dependencyToReset.getRefToCompiledDependency() != null) {
                log.debug("Dependency '{}' is reset.", dependencyToReset.getDependency());
            }
            dependencyToReset.reset();
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
        getDependencyLoaders().forEach(IDependencyLoader::reset);
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
