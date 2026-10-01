package org.openl.rules.project.instantiation;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.OpenClassUtil;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.AmbiguousDependencyException;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyNotFoundException;
import org.openl.dependency.DependencyType;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.ResolvedDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

@Slf4j
public abstract class AbstractDependencyManager implements IDependencyManager {

    private static final String DEPENDENCY_NOT_FOUND = "Dependency '%s' is not found.";



    private final AtomicReference<CopyOnWriteArraySet<IDependencyLoader>> dependencyLoaders = new AtomicReference<>();
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
            var result = 1;
            result = 31 * result + (dependOnThisDependency == null ? 0 : dependOnThisDependency.hashCode());
            result = 31 * result + (dependency == null ? 0 : dependency.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DependencyRelation other)) {
                return false;
            }
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
            return "DependencyReference [dependOnThisDependency=%s, dependency=%s]".formatted(
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
                dependencyLoaders.get().addAll(dependencyLoadersToAdd);
            }
        }
    }

    public Collection<IDependencyLoader> getDependencyLoaders() {
        var loaders = dependencyLoaders.get();
        if (loaders == null) {
            synchronized (this) {
                loaders = dependencyLoaders.get();
                if (loaders == null) {
                    loaders = new CopyOnWriteArraySet<>(initDependencyLoaders());
                    dependencyLoaders.set(loaders);
                }
            }
        }
        return Collections.unmodifiableSet(loaders);
    }

    protected abstract Set<IDependencyLoader> initDependencyLoaders();

    private Deque<IDependencyLoader> getCompilationStack() {
        return compilationStackThreadLocal.get();
    }

    // Disable cache. if cache required it should be used in loaders.
    // Locked through a block: an override may then do its own bookkeeping outside the lock before calling this.
    @Override
    public CompiledDependency loadDependency(
            ResolvedDependency dependency) throws OpenLCompilationException {
        synchronized (this) {
            final var dependencyLoader = findDependencyLoaderByDependency(dependency);
            Deque<IDependencyLoader> compilationStack = getCompilationStack();
            try {
                if (log.isDebugEnabled()) {
                    log.debug(
                            compilationStack
                                    .contains(dependencyLoader) ? "Dependency '{}' in the compilation stack."
                                    : "Dependency '{}' is not found in the compilation stack.",
                            dependency);
                }
                var isCircularDependency = compilationStack.contains(dependencyLoader);
                if (!isCircularDependency && !compilationStack.isEmpty()) {
                    var dr = new DependencyRelation(getCompilationStack().getFirst(), dependencyLoader);
                    this.addDependencyRelation(dr);
                }

                if (isCircularDependency) {
                    throw new OpenLCompilationException(
                            "Circular dependency is detected: %s.".formatted(
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
                    log.debug("Dependency '{}' is removed from the compilation stack.",
                            dependencyLoader.getDependency());
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
    }

    public IDependencyLoader findDependencyLoader(ResolvedDependency dependency) {
        return getDependencyLoaders().stream()
                .filter(e -> Objects.equals(dependency, e.getDependency()))
                .findFirst()
                .orElse(null);
    }

    public Collection<IDependencyLoader> findAllProjectDependencyLoaders(ProjectDescriptor project) {
        var dependencyLoadersForProject = new HashSet<IDependencyLoader>();
        var queue = new ArrayDeque<ProjectDescriptor>();
        queue.add(project);
        var projectDescriptors = new HashSet<ProjectDescriptor>();
        while (!queue.isEmpty()) {
            var projectDescriptor = queue.poll();
            getDependencyLoaders().stream()
                    .filter(e -> Objects.equals(e.getProject(), projectDescriptor))
                    .forEach(dependencyLoadersForProject::add);
            if (projectDescriptor.getDependencies() != null) {
                for (ProjectDependencyDescriptor pdd : projectDescriptor.getDependencies()) {
                    if (pdd.getName() == null) {
                        // A <mavenArtifact> dependency with no <name> is a plain Maven artifact (jar),
                        // resolved via the project classpath — not an OpenL project reference.
                        continue;
                    }
                    var dl = this.findDependencyLoader(buildResolvedDependency(pdd.getName()));
                    if (dl != null && dl.isProjectLoader() && !projectDescriptors.contains(dl.getProject())) {
                        queue.add(dl.getProject());
                        projectDescriptors.add(dl.getProject());
                    }
                }
            }
        }
        return dependencyLoadersForProject;
    }

    /**
     * Builds the pattern that matches dependency names against the given identifier.
     *
     * <p>With wildcard support, {@code *} matches any sequence of characters, {@code ?} matches one character, and
     * {@code /} matches a slash with any whitespace around it. All other characters match only themselves.
     */
    static Pattern dependencyPattern(String identifier, boolean withWildcardSupport) {
        if (!withWildcardSupport) {
            return Pattern.compile(Pattern.quote(identifier));
        }
        var regex = new StringBuilder();
        var literal = new StringBuilder();
        boolean afterSlash = false;
        for (char c : identifier.toCharArray()) {
            switch (c) {
                case '*' -> appendWildcard(regex, literal, ".*");
                case '?' -> appendWildcard(regex, literal, ".");
                case '/' -> {
                    literal.setLength(literal.toString().stripTrailing().length());
                    appendWildcard(regex, literal, "\\s*/\\s*");
                }
                default -> {
                    if (!afterSlash || !Character.isWhitespace(c)) {
                        literal.append(c);
                    }
                }
            }
            afterSlash = c == '/' || afterSlash && Character.isWhitespace(c);
        }
        appendWildcard(regex, literal, "");
        return Pattern.compile(regex.toString());
    }

    private static void appendWildcard(StringBuilder regex, StringBuilder literal, String wildcard) {
        if (!literal.isEmpty()) {
            regex.append(Pattern.quote(literal.toString()));
            literal.setLength(0);
        }
        regex.append(wildcard);
    }

    private static boolean matchesDependencyName(IDependencyLoader loader,
                                                 Pattern pattern,
                                                 @Nullable IDependencyLoader currentLoader) {
        var ownProject = loader.isProjectLoader() && currentLoader != null
                && Objects.equals(loader.getProject(), currentLoader.getProject());
        return !ownProject && pattern.matcher(loader.getDependency().getNode().getIdentifier()).matches();
    }

    private static boolean matchesModuleName(IDependencyLoader loader, Pattern pattern) {
        return !loader.isProjectLoader() && pattern.matcher(loader.getModule().getName()).matches();
    }

    @Override
    public Collection<ResolvedDependency> resolveDependency(IDependency dependency,
                                                            boolean withWildcardSupport) throws AmbiguousDependencyException, DependencyNotFoundException {
        var identifier = dependency.getNode().getIdentifier();
        boolean withWildcard = withWildcardSupport && (identifier.indexOf('*') >= 0 || identifier.indexOf('?') >= 0);
        var pattern = dependencyPattern(identifier, withWildcardSupport);
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

        var matchedLoaders = visibleDependencyLoaders.stream()
                .filter(dl -> !Objects.equals(currentDependencyLoader, dl))
                .filter(dl -> matchesDependencyName(dl, pattern, currentDependencyLoader)
                        || matchesModuleName(dl, pattern))
                .collect(Collectors.toSet());

        if (matchedLoaders.stream().anyMatch(e -> !e.isProjectLoader())) {
            matchedLoaders = matchedLoaders.stream()
                    .filter(e -> !e.isProjectLoader())
                    .collect(Collectors.toSet());
        }

        var ret = matchedLoaders.stream()
                .map(e -> new ResolvedDependency(e.isProjectLoader() ? DependencyType.PROJECT : DependencyType.MODULE,
                        new IdentifierNode(dependency.getNode().getType(),
                                dependency.getNode().getLocation(),
                                e.getDependency().getNode().getIdentifier(),
                                null)))
                .collect(Collectors.toSet());

        if (!withWildcard && ret.size() != 1) {
            if (ret.isEmpty()) {
                throw new DependencyNotFoundException(
                        DEPENDENCY_NOT_FOUND.formatted(dependency.getNode().getIdentifier()),
                        null,
                        dependency.getNode().getSourceLocation(),
                        dependency.getNode().getModule());
            } else {
                throw new AmbiguousDependencyException(
                        "Multiple dependencies '%s' are found.".formatted(dependency.getNode().getIdentifier()),
                        null,
                        dependency.getNode().getSourceLocation(),
                        dependency.getNode().getModule());
            }
        }
        return ret;
    }

    protected IDependencyLoader findDependencyLoaderByDependency(
            ResolvedDependency dependency) throws OpenLCompilationException {
        var dependencyLoader = findDependencyLoader(dependency);
        if (dependencyLoader == null) {
            throw new OpenLCompilationException(
                    DEPENDENCY_NOT_FOUND.formatted(dependency.getNode().getIdentifier()),
                    null,
                    dependency.getNode().getSourceLocation(),
                    dependency.getNode().getModule());
        }
        return dependencyLoader;
    }

    private static String buildCircularDependencyDetails(IDependencyLoader dependencyLoader,
                                                         Deque<IDependencyLoader> compilationStack) {
        var sb = new StringBuilder();

        var p = compilationStack.stream()
                .filter(e -> !e.isProjectLoader())
                .map(IDependencyLoader::getModule)
                .collect(Collectors.groupingBy(Module::getName));

        Iterator<IDependencyLoader> itr = compilationStack.iterator();
        sb.append("'");
        sb.insert(0,
                !dependencyLoader.isProjectLoader() && p.get(dependencyLoader.getModule().getName())
                        .size() == 1 ? dependencyLoader.getModule().getName() : dependencyLoader.getDependency());
        while (itr.hasNext()) {
            var s = itr.next();
            sb.insert(0, "' -> '");
            sb.insert(0,
                    !s.isProjectLoader() && p.get(s.getModule().getName()).size() == 1 ? s.getModule().getName()
                            : s.getDependency());
            if (Objects.equals(dependencyLoader, s)) {
                break;
            }
        }
        sb.insert(0, "'");
        return sb.toString();

    }

    private CompiledDependency throwDependencyNotFoundError(IDependency dependency) throws OpenLCompilationException {
        var node = dependency.getNode();
        throw new OpenLCompilationException(DEPENDENCY_NOT_FOUND.formatted(node.getIdentifier()),
                null,
                node.getSourceLocation(),
                node.getModule());
    }

    public synchronized ClassLoader getExternalJarsClassLoader(ProjectDescriptor project) {
        var breadcrumbs = new HashSet<ProjectDescriptor>();
        breadcrumbs.add(project);
        return getExternalJarsClassLoaderRec(project, breadcrumbs);
    }

    private synchronized ClassLoader getExternalJarsClassLoaderRec(ProjectDescriptor project,
                                                                  Set<ProjectDescriptor> breadcrumbs) {
        getDependencyLoaders(); // Init dependency loaders
        if (externalJarsClassloaders.get(project) != null) {
            return externalJarsClassloaders.get(project);
        }
        ClassLoader parentClassLoader = rootClassLoader == null ? this.getClass().getClassLoader() : rootClassLoader;
        var externalJarsClassloader = new OpenLClassLoader(project.getClassPathUrls(), parentClassLoader);
        // To load classes from dependency jars first
        if (project.getDependencies() != null) {
            var loadedProjects = getDependencyLoaders().stream()
                    .filter(IDependencyLoader::isProjectLoader)
                    .map(IDependencyLoader::getProject)
                    .toList();
            for (ProjectDependencyDescriptor projectDependencyDescriptor : project.getDependencies()) {
                var dependencyProject = findProject(loadedProjects, projectDependencyDescriptor.getName());
                if (dependencyProject != null && breadcrumbs.add(dependencyProject)) {
                    externalJarsClassloader
                            .addClassLoader(getExternalJarsClassLoaderRec(dependencyProject, breadcrumbs));
                    breadcrumbs.remove(dependencyProject);
                }
            }
        }
        externalJarsClassloaders.put(project, externalJarsClassloader);
        return externalJarsClassloader;
    }

    private static @Nullable ProjectDescriptor findProject(Collection<ProjectDescriptor> projects, String name) {
        return projects.stream().filter(p -> Objects.equals(name, p.getName())).findFirst().orElse(null);
    }

    @Override
    public synchronized void resetOthers(ResolvedDependency... dependencies) {
        if (dependencies == null || dependencies.length == 0) {
            return;
        }
        var dependenciesToKeep = new HashSet<IDependencyLoader>();
        for (ResolvedDependency dependency : dependencies) {
            // A dependency this manager does not load is not among the ones it could keep.
            var dependencyLoader = dependency == null ? null : findDependencyLoader(dependency);
            if (dependencyLoader != null) {
                dependenciesToKeep.add(dependencyLoader);
            }
        }
        addUsedDependencies(dependenciesToKeep);
        for (IDependencyLoader depLoader : getDependencyLoaders()) {
            if (!dependenciesToKeep.contains(depLoader)) {
                reset(depLoader.getDependency());
            }
        }
    }

    /**
     * Adds to the given dependencies every dependency they use, directly or through other dependencies.
     */
    private void addUsedDependencies(Set<IDependencyLoader> dependencies) {
        var queue = new ArrayDeque<>(dependencies);
        while (!queue.isEmpty()) {
            var depLoader = queue.poll();
            for (DependencyRelation dependencyReference : dependencyRelations) {
                if (dependencyReference.getDependency().equals(depLoader)
                        && dependencies.add(dependencyReference.getDependOnThisDependency())) {
                    queue.add(dependencyReference.getDependOnThisDependency());
                }
            }
        }
    }

    @Override
    public synchronized void reset(ResolvedDependency dependency) {
        if (dependency == null) {
            return;
        }
        var dependencyLoader = findDependencyLoader(dependency);
        if (dependencyLoader == null) {
            // A dependency this manager does not load has nothing compiled here to drop: asked for later, it
            // is compiled from its sources as they stand then.
            return;
        }
        var projectClassloaderToReset = new HashSet<ProjectDescriptor>();
        var dependenciesToReset = collectDependenciesToReset(dependencyLoader, projectClassloaderToReset);
        var dependenciesReferencesToRemove = dependencyRelations.stream()
                .filter(dependencyReference -> dependenciesToReset.contains(dependencyReference.getDependency()))
                .toList();
        for (IDependencyLoader dependencyToReset : dependenciesToReset) {
            if (dependencyToReset.getRefToCompiledDependency() != null) {
                log.debug("Dependency '{}' is reset.", dependencyToReset.getDependency());
            }
            dependencyToReset.reset();
        }
        dependenciesReferencesToRemove.forEach(dependencyRelations::remove);
        for (ProjectDescriptor projectDescriptor : projectClassloaderToReset) {
            var cl = externalJarsClassloaders.get(projectDescriptor);
            if (cl != null) {
                OpenClassUtil.releaseClassLoader(cl);
            }
            externalJarsClassloaders.remove(projectDescriptor);
            projectDescriptor.releaseClassPath();
        }
    }

    /**
     * Collects the dependency and every dependency using it, directly or through other dependencies.
     *
     * <p>Datatypes are generated into the project classloader. When a collected dependency contains datatypes, the
     * whole project of the given dependency needs to be recompiled. That project and every project depending on it
     * are added to the given projects to reset, and every dependency loaded from them is collected too.
     */
    private Set<IDependencyLoader> collectDependenciesToReset(IDependencyLoader dependencyLoader,
                                                              Set<ProjectDescriptor> projectsToReset) {
        var queue = new ArrayDeque<IDependencyLoader>();
        queue.add(dependencyLoader);
        var dependenciesToReset = new HashSet<IDependencyLoader>();
        dependenciesToReset.add(dependencyLoader);
        while (!queue.isEmpty()) {
            var depLoader = queue.poll();
            for (DependencyRelation dependencyReference : dependencyRelations) {
                if (dependencyReference.getDependOnThisDependency().equals(depLoader)
                        && dependenciesToReset.add(dependencyReference.getDependency())) {
                    queue.add(dependencyReference.getDependency());
                }
            }
            if (isAppliedChangesToClasspath(depLoader)) {
                addProjectToReset(dependencyLoader.getProject(), projectsToReset, dependenciesToReset, queue);
            }
        }
        return dependenciesToReset;
    }

    private static boolean isAppliedChangesToClasspath(IDependencyLoader dependencyLoader) {
        var compiledDependency = dependencyLoader.getRefToCompiledDependency();
        return compiledDependency != null
                && compiledDependency.getCompiledOpenClass().getOpenClassWithErrors() instanceof XlsModuleOpenClass openClass
                && openClass.isAppliedChangesToClasspath();
    }

    /**
     * Adds the project and every project depending on it to the projects to reset. Each dependency loaded from
     * them, which is not reset yet, is added to the dependencies to reset and to the queue.
     */
    private void addProjectToReset(ProjectDescriptor project,
                                   Set<ProjectDescriptor> projectsToReset,
                                   Set<IDependencyLoader> dependenciesToReset,
                                   Queue<IDependencyLoader> queue) {
        var projects = new ArrayDeque<ProjectDescriptor>();
        projects.add(project);
        while (!projects.isEmpty()) {
            var pd = projects.poll();
            if (projectsToReset.add(pd)) {
                for (IDependencyLoader dl : getDependencyLoaders()) {
                    if (Objects.equals(dl.getProject(), pd) && dependenciesToReset.add(dl)) {
                        queue.add(dl);
                    }
                    if (dependsOnProject(dl, pd)) {
                        projects.add(dl.getProject());
                    }
                }
            }
        }
    }

    private static boolean dependsOnProject(IDependencyLoader dependencyLoader, ProjectDescriptor project) {
        if (!dependencyLoader.isProjectLoader()) {
            return false;
        }
        var dependencies = dependencyLoader.getProject().getDependencies();
        return dependencies != null && dependencies.stream()
                .anyMatch(dependency -> Objects.equals(dependency.getName(), project.getName()));
    }

    @Override
    public synchronized void resetAll() {
        for (var entry : externalJarsClassloaders.entrySet()) {
            OpenClassUtil.releaseClassLoader(entry.getValue());
            entry.getKey().releaseClassPath();
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
