package org.openl.rules.webstudio.dependencies;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.CompiledOpenClass;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyType;
import org.openl.dependency.ResolvedDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLErrorMessage;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.DependencyLoaderInitializationException;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.types.NullOpenClass;

@Slf4j
public class WebStudioWorkspaceRelatedDependencyManager extends AbstractDependencyManager {

    private enum ThreadPriority {
        LOW,
        HIGH
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Set<ProjectDescriptor> projects;
    private final AtomicLong version = new AtomicLong(0);
    private final ThreadLocal<Long> threadVersion = new ThreadLocal<>();
    private final AtomicLong highThreadPriorityFlag = new AtomicLong(0);
    private final ThreadLocal<ThreadPriority> threadPriority = new ThreadLocal<>();
    private final AtomicLong backgroundGeneration = new AtomicLong(0);
    private final ThreadLocal<Long> threadBackgroundGeneration = new ThreadLocal<>();
    private final ThreadLocal<Boolean> threadRefused = new ThreadLocal<>();
    /** Whether this manager still compiles what it is asked for. */
    @Getter
    private volatile boolean active = true;
    private volatile boolean paused;
    private final List<BiConsumer<IDependencyLoader, CompiledDependency>> onCompilationCompleteListeners = new CopyOnWriteArrayList<>();
    private final List<BiConsumer<IDependencyLoader, CompiledDependency>> onResetCompleteListeners = new CopyOnWriteArrayList<>();
    @Getter
    private final boolean canUnload;

    public WebStudioWorkspaceRelatedDependencyManager(Collection<ProjectDescriptor> projects,
                                                      ClassLoader rootClassLoader,
                                                      boolean executionMode,
                                                      Map<String, Object> externalParameters,
                                                      boolean canUnload) {
        super(rootClassLoader, executionMode, externalParameters);
        this.projects = new LinkedHashSet<>(Objects.requireNonNull(projects, "projects cannot be null"));
        this.canUnload = canUnload;
    }

    private DependencyType resolveDependencyType(ResolvedDependency dependency) {
        var dependencyLoader = findDependencyLoader(dependency);
        return dependencyLoader != null && dependencyLoader.isProjectLoader() ? DependencyType.PROJECT
                : DependencyType.MODULE;
    }

    @Override
    public CompiledDependency loadDependency(ResolvedDependency dependency) throws OpenLCompilationException {
        var priority = threadPriority.get();
        if (priority == null) {
            threadPriority.set(ThreadPriority.HIGH);
        }
        if (priority == null || priority == ThreadPriority.HIGH) {
            highThreadPriorityFlag.incrementAndGet();
        }
        try {
            synchronized (this) {
                if (priority == ThreadPriority.LOW) {
                    // Low priority threads wait here
                    try {
                        while (highThreadPriorityFlag.get() > 0) {
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new OpenLCompilationException("Compilation is interrupted", e);
                    }
                }
                return loadForThreadVersion(dependency, priority);
            }
        } finally {
            if (priority == null) {
                threadPriority.remove();
            }
            if (priority == null || priority == ThreadPriority.HIGH) {
                synchronized (this) {
                    highThreadPriorityFlag.decrementAndGet();
                    this.notifyAll();
                }
            }
        }
    }

    /**
     * Loads the dependency for the compilation the thread takes part in. The first request of a thread fixes the
     * version it compiles; a nested request is served only while that version is still the current one. A stopped
     * manager loads nothing, and neither does a background load another one has replaced.
     */
    private CompiledDependency loadForThreadVersion(ResolvedDependency dependency,
                                                    @Nullable ThreadPriority priority)
            throws OpenLCompilationException {
        var currentThreadVersion = threadVersion.get();
        if (currentThreadVersion == null) {
            threadVersion.set(version.get());
            try {
                log.debug("Dependency '{}' is requested with '{}' priority.",
                        dependency.getNode().getIdentifier(),
                        priority == null ? ThreadPriority.HIGH : priority);
                if (isWanted()) {
                    return loadDependencySync(dependency);
                }
                return interrupted(dependency);
            } finally {
                threadVersion.remove();
            }
        } else {
            if (isWanted() && Objects.equals(currentThreadVersion, version.get())) {
                log.debug("Dependency '{}' is requested with '{}' priority.",
                        dependency.getNode().getIdentifier(),
                        priority == null ? ThreadPriority.HIGH : priority);
                return loadDependencySync(dependency);
            }
            return interrupted(dependency);
        }
    }

    private synchronized CompiledDependency loadDependencySync(ResolvedDependency dependency) throws OpenLCompilationException {
        while (paused && isWanted()) {
            try {
                this.wait(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OpenLCompilationException("Compilation is interrupted", e);
            }
        }
        // What was told to stop while it waited starts nothing: the wait ends on the stop as well.
        if (!isWanted()) {
            return interrupted(dependency);
        }
        return super.loadDependency(dependency);
    }

    /**
     * Whether what the current thread compiles is still wanted: the manager runs, and the thread does not serve a
     * background load that another one has replaced.
     */
    private boolean isWanted() {
        var generation = threadBackgroundGeneration.get();
        return active && (generation == null || generation == backgroundGeneration.get());
    }

    /**
     * Whether what the current thread has just compiled may be kept: nothing it was compiled from has been reset
     * since, and no dependency it asked for was refused. A module a replaced background load finishes is kept.
     */
    boolean isActualForThread() {
        return threadVersion.get() >= version.get() && !Boolean.TRUE.equals(threadRefused.get());
    }

    /** What a request answers once this manager has been told to stop: nothing compiled, and why. */
    private CompiledDependency interrupted(ResolvedDependency dependency) {
        if (threadBackgroundGeneration.get() != null) {
            // What this background thread compiles from here on lacks the dependency, so it is not kept.
            threadRefused.set(true);
        }
        return new CompiledDependency(dependency,
                new CompiledOpenClass(NullOpenClass.the, List.of(new CompilationInterruptedOpenLErrorMessage())),
                resolveDependencyType(dependency));
    }

    /**
     * Stops compiling.
     *
     * <p>What is being compiled at this moment is finished — a module cannot be abandoned halfway — and nothing
     * after it is started: every further request answers as an interrupted compilation, which the caller reports
     * rather than retries. What is already compiled stays as it is and can still be read.
     *
     * <p>A manager told to stop compiles nothing again; asking for a compilation after this builds a new one.
     */
    public void cancel() {
        active = false;
        version.incrementAndGet();
    }

    public void pause() {
        paused = true;
        awaitSynchronizedBlocks();
    }

    /**
     * Returns only after the synchronized blocks running at the moment have been executed.
     */
    private synchronized void awaitSynchronizedBlocks() {
        // Taking the lock is all it needs: a block that holds it finishes first
    }

    public void resume() {
        paused = false;
    }

    /**
     * Loads the dependency in the background, yielding to every load asked for in the foreground, and hands the
     * result to the consumer.
     *
     * <p>A load interrupted by a reset of what it compiles starts again. A load replaced by
     * {@link #stopBackgroundLoads()} starts nothing more; left unfinished by it, the load ends without calling the
     * consumer.
     */
    public void loadDependencyAsync(ResolvedDependency dependency, Consumer<CompiledDependency> consumer) {
        var generation = backgroundGeneration.get();
        executorService.submit(() -> loadInBackground(dependency, consumer, generation));
    }

    private void loadInBackground(ResolvedDependency dependency,
                                  Consumer<CompiledDependency> consumer,
                                  long generation) {
        try {
            threadPriority.set(ThreadPriority.LOW);
            threadBackgroundGeneration.set(generation);
            if (!isWanted()) {
                return;
            }
            CompiledDependency compiledDependency;
            try {
                compiledDependency = this.loadDependency(dependency);
            } catch (OpenLCompilationException e) {
                compiledDependency = new CompiledDependency(dependency,
                        new CompiledOpenClass(NullOpenClass.the, List.of(new OpenLErrorMessage(e))),
                        resolveDependencyType(dependency));
            }
            if (compiledDependency.getCompiledOpenClass()
                    .getAllMessages()
                    .stream()
                    .anyMatch(CompilationInterruptedOpenLErrorMessage.class::isInstance)) {
                if (isWanted()) {
                    executorService.submit(() -> loadInBackground(dependency, consumer, generation));
                }
            } else {
                consumer.accept(compiledDependency);
            }
        } finally {
            threadPriority.remove();
            threadBackgroundGeneration.remove();
            threadRefused.remove();
        }
    }

    /**
     * Stops the loads asked for in the background so far.
     *
     * <p>The module being compiled at this moment is finished and kept, and nothing after it is started. What waits
     * for a module the stop refuses — the project above all — is not kept, and its load ends without calling the
     * consumer. Every module compiled stays as it is and is reused by the next load.
     *
     * <p>Loads asked for afterwards run as usual, and so do the loads asked for in the foreground.
     */
    public void stopBackgroundLoads() {
        backgroundGeneration.incrementAndGet();
    }

    protected Set<IDependencyLoader> initDependencyLoaders() {
        return buildDependencyLoaders(projects);
    }

    private Set<IDependencyLoader> buildDependencyLoaders(Set<ProjectDescriptor> projects) {
        var dependencyLoaders = new HashSet<IDependencyLoader>();
        for (ProjectDescriptor project : projects) {
            try {
                Collection<Module> modulesOfProject = project.getModules();
                if (!modulesOfProject.isEmpty()) {
                    for (final Module m : modulesOfProject) {
                        var moduleDependencyLoader = new WebStudioDependencyLoader(project,
                                m,
                                this);
                        dependencyLoaders.add(moduleDependencyLoader);
                    }
                }

                var projectDependencyLoader = new WebStudioDependencyLoader(project, null, this);
                dependencyLoaders.add(projectDependencyLoader);
            } catch (Exception e) {
                throw new DependencyLoaderInitializationException(
                        "Failed to initialize dependency loaders for project '%s'.".formatted(project.getName()),
                        e);
            }
        }
        return dependencyLoaders;
    }

    @Override
    public synchronized void resetOthers(ResolvedDependency... dependencies) {
        version.incrementAndGet();
        super.resetOthers(dependencies);
    }

    @Override
    public synchronized void reset(ResolvedDependency dependency) {
        if (findDependencyLoader(dependency) == null) {
            // Nothing of it was compiled here, so nothing is dropped - and no compilation in flight is made
            // stale over it.
            return;
        }
        version.incrementAndGet();
        super.reset(dependency);
    }

    @Override
    public synchronized void resetAll() {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    /**
     * Stops compiling and lets go of everything compiled.
     *
     * <p>The module being compiled at this moment is finished first, and nothing after it is started: the caller
     * waits for that module only, not for the rest of the compilation. What was compiled, the jars of the projects
     * included, is released before this returns, so a manager built next reads the classpath of the projects afresh.
     *
     * <p>The compilation in flight hands its result over once this returns, if at all. The caller may hold a lock
     * it takes to do so, so it is never waited for.
     */
    public void shutdown() {
        resume();
        synchronized (listenersMutex) {
            onCompilationCompleteListeners.clear();
            onResetCompleteListeners.clear();
        }
        active = false;
        version.incrementAndGet();
        executorService.shutdown();
        super.resetAll();
    }

    public void expand(Set<ProjectDescriptor> projects) {
        var dependencyLoaders = buildDependencyLoaders(projects);
        addDependencyLoaders(dependencyLoaders);
    }

    /**
     * Finds what a compilation of the project takes in: the project, every project it includes automatically, all
     * their modules, and every module these use.
     *
     * <p>A dependency project that is not included automatically ({@code autoIncluded} is {@code false}) takes part
     * only with the modules the others use, and with what those use in turn. Which modules a module uses is known
     * once the module is compiled, so what is found grows while the project compiles.
     *
     * @return the project and module loaders of the compilation, the project and its modules first, in the order
     *     the project declares them; empty when the project is not loaded here
     */
    public Set<IDependencyLoader> findLoadersCompiledWith(ProjectDescriptor project) {
        var loaders = getDependencyLoaders().stream()
                .collect(Collectors.toMap(IDependencyLoader::getDependency, Function.identity(), (one, other) -> one));
        var found = new LinkedHashSet<IDependencyLoader>();
        var queue = new ArrayDeque<IDependencyLoader>();
        var projectLoader = loaders.get(buildResolvedDependency(project));
        if (projectLoader != null) {
            queue.add(projectLoader);
        }
        while (!queue.isEmpty()) {
            var loader = queue.poll();
            if (found.add(loader)) {
                queue.addAll(usedBy(loader, loaders));
            }
        }
        return found;
    }

    /**
     * What the compilation of the loader takes in directly: the projects its project includes automatically, and
     * the modules of a project or the modules a compiled module uses.
     */
    private static List<IDependencyLoader> usedBy(IDependencyLoader loader,
                                                  Map<ResolvedDependency, IDependencyLoader> loaders) {
        var project = loader.getProject();
        var used = loader.isProjectLoader()
                ? project.getModules().stream().map(AbstractDependencyManager::buildResolvedDependency)
                : usedByCompiled(loader);
        return Stream.concat(autoIncluded(project), used)
                .map(loaders::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private static Stream<ResolvedDependency> usedByCompiled(IDependencyLoader loader) {
        var compiled = loader.getRefToCompiledDependency();
        if (compiled != null
                && compiled.getCompiledOpenClass().getOpenClassWithErrors() instanceof ModuleOpenClass module) {
            return module.getDependencies().stream().map(CompiledDependency::getDependency);
        }
        return Stream.empty();
    }

    private static Stream<ResolvedDependency> autoIncluded(ProjectDescriptor project) {
        // A Maven artifact without a name is a jar on the classpath, not a project.
        return ProjectExternalDependenciesHelper.autoIncludedProjects(project)
                .stream()
                .filter(Objects::nonNull)
                .map(AbstractDependencyManager::buildResolvedDependency);
    }

    public void registerOnCompilationCompleteListener(
            BiConsumer<IDependencyLoader, CompiledDependency> onCompilationCompleteListener) {
        onCompilationCompleteListeners.add(onCompilationCompleteListener);
    }

    public void registerOnResetCompleteListener(
            BiConsumer<IDependencyLoader, CompiledDependency> onResetCompleteListener) {
        onResetCompleteListeners.add(onResetCompleteListener);
    }

    private final Object listenersMutex = new Object();

    public void fireOnCompilationCompleteListeners(IDependencyLoader dependencyLoader,
                                                   CompiledDependency compiledDependency) {
        synchronized (listenersMutex) {
            for (BiConsumer<IDependencyLoader, CompiledDependency> listener : onCompilationCompleteListeners) {
                try {
                    listener.accept(dependencyLoader, compiledDependency);
                } catch (Exception e) {
                    log.error("Fail during on compilation complete listener invocation.", e);
                }
            }
        }
    }

    public void fireOnResetCompleteListeners(IDependencyLoader dependencyLoader,
                                             CompiledDependency compiledDependency) {
        synchronized (listenersMutex) {
            for (BiConsumer<IDependencyLoader, CompiledDependency> listener : onResetCompleteListeners) {
                try {
                    listener.accept(dependencyLoader, compiledDependency);
                } catch (Exception e) {
                    log.error("Fail during on reset complete listener invocation.", e);
                }
            }
        }
    }
}
