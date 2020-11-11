package org.openl.rules.webstudio.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLErrorMessage;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.DependencyLoaderInitializationException;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.IDependency;
import org.openl.types.NullOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebStudioWorkspaceRelatedDependencyManager extends AbstractDependencyManager {
    private final Logger log = LoggerFactory.getLogger(WebStudioWorkspaceRelatedDependencyManager.class);

    private enum ThreadPriority {
        LOW,
        HIGH;
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final List<ProjectDescriptor> projects;
    private final AtomicLong version = new AtomicLong(0);
    private final ThreadLocal<Long> threadVersion = new ThreadLocal<>();
    private final AtomicLong highThreadPriorityFlag = new AtomicLong(0);
    private final ThreadLocal<ThreadPriority> threadPriority = new ThreadLocal<>();
    private volatile boolean shutdowned = false;

    public WebStudioWorkspaceRelatedDependencyManager(Collection<ProjectDescriptor> projects,
            ClassLoader rootClassLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(rootClassLoader, executionMode, externalParameters);
        this.projects = new ArrayList<>(Objects.requireNonNull(projects, "projects cannot be null"));
        initDependencyLoaders();
    }

    @Override
    public CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        ThreadPriority priority = threadPriority.get();
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
                        throw new OpenLCompilationException("Compilation is interrupted", e);
                    }
                }

                Long currentThreadVersion = threadVersion.get();
                if (currentThreadVersion == null) {
                    threadVersion.set(version.get());
                    try {
                        log.debug("Dependency '{}' is requested with '{}' priority.",
                            dependency.getNode().getIdentifier(),
                            priority == null ? ThreadPriority.HIGH : priority);
                        return super.loadDependency(dependency);
                    } finally {
                        threadVersion.remove();
                    }

                } else {
                    if (Objects.equals(currentThreadVersion, version.get())) {
                        log.debug("Dependency '{}' is requested with '{}' priority.",
                            dependency.getNode().getIdentifier(),
                            priority == null ? ThreadPriority.HIGH : priority);
                        return super.loadDependency(dependency);
                    } else {
                        return new CompiledDependency(dependency.getNode().getIdentifier(),
                            new CompiledOpenClass(NullOpenClass.the,
                                Collections.singletonList(new CompilationInterruptedOpenLErrorMessage())));
                    }
                }
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

    public ThreadLocal<Long> getThreadVersion() {
        return threadVersion;
    }

    public AtomicLong getVersion() {
        return version;
    }

    public void loadDependencyAsync(IDependency dependency, Consumer<CompiledDependency> consumer) {
        executorService.submit(() -> {
            try {
                threadPriority.set(ThreadPriority.LOW);
                CompiledDependency compiledDependency;
                try {
                    compiledDependency = this.loadDependency(dependency);
                } catch (OpenLCompilationException e) {
                    compiledDependency = new CompiledDependency(dependency.getNode().getIdentifier(),
                        new CompiledOpenClass(NullOpenClass.the, Collections.singletonList(new OpenLErrorMessage(e))));
                }
                if (compiledDependency.getCompiledOpenClass()
                    .getMessages()
                    .stream()
                    .anyMatch(e -> e instanceof CompilationInterruptedOpenLErrorMessage)) {
                    if (!shutdowned) {
                        loadDependencyAsync(dependency, consumer);
                    } else {
                        consumer.accept(null);
                    }
                } else {
                    consumer.accept(compiledDependency);
                }
            } finally {
                threadPriority.remove();
            }
        });
    }

    protected Map<String, Collection<IDependencyLoader>> initDependencyLoaders() {
        Map<String, Collection<IDependencyLoader>> dependencyLoaders = new HashMap<>();
        for (ProjectDescriptor project : projects) {
            try {
                Collection<Module> modulesOfProject = project.getModules();
                if (!modulesOfProject.isEmpty()) {
                    for (final Module m : modulesOfProject) {
                        WebStudioDependencyLoader moduleDependencyLoader = new WebStudioDependencyLoader(project,
                            m,
                            this);
                        Collection<IDependencyLoader> dependencyLoadersByName = dependencyLoaders
                            .computeIfAbsent(moduleDependencyLoader.getDependencyName(), e -> new ArrayList<>());
                        dependencyLoadersByName.add(moduleDependencyLoader);
                    }
                }

                WebStudioDependencyLoader projectDependencyLoader = new WebStudioDependencyLoader(project, null, this);
                Collection<IDependencyLoader> dependencyLoadersByName = dependencyLoaders
                    .computeIfAbsent(projectDependencyLoader.getDependencyName(), e -> new ArrayList<>());
                dependencyLoadersByName.add(projectDependencyLoader);

            } catch (Exception e) {
                throw new DependencyLoaderInitializationException(
                    String.format("Failed to initialize dependency loaders for project '%s'.", project.getName()),
                    e);
            }
        }
        return dependencyLoaders;
    }

    @Override
    public void resetOthers(IDependency... dependencies) {
        version.incrementAndGet();
        super.resetOthers(dependencies);
    }

    @Override
    public void reset(IDependency dependency) {
        version.incrementAndGet();
        super.reset(dependency);
    }

    @Override
    public void resetAll() {
        version.incrementAndGet();
        super.resetAll();
    }

    public void shutdown() {
        shutdowned = true;
        executorService.shutdown();
        version.incrementAndGet();
    }
}
