package org.openl.rules.webstudio.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import org.openl.dependency.DependencyType;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.ResolvedDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.impl.IdentifierNode;

class WebStudioWorkspaceRelatedDependencyManagerTest {

    public static class WebStudioWorkspaceRelatedSimpleProjectEngineFactoryBuilder<T> extends SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<T> {
        @Override
        public WebStudioWorkspaceRelatedSimpleProjectEngineFactory<T> build() {
            if (project == null || project.isEmpty()) {
                throw new IllegalArgumentException("project cannot be null or empty");
            }
            var projectFile = Path.of(project);
            var dependencies = getProjectDependencies();
            return new WebStudioWorkspaceRelatedSimpleProjectEngineFactory<>(projectFile,
                    dependencies,
                    classLoader,
                    interfaceClass,
                    externalParameters,
                    executionMode);
        }
    }

    public static class WebStudioWorkspaceRelatedSimpleProjectEngineFactory<T> extends SimpleProjectEngineFactory<T> {
        public WebStudioWorkspaceRelatedSimpleProjectEngineFactory(Path project,
                                                                   List<Path> projectDependencies,
                                                                   ClassLoader classLoader,
                                                                   Class<T> interfaceClass,
                                                                   Map<String, Object> externalParameters,
                                                                   boolean executionMode) {
            super(project,
                    projectDependencies,
                    classLoader,
                    interfaceClass,
                    externalParameters,
                    executionMode);
        }

        @Override
        protected IDependencyManager buildDependencyManager() throws ProjectResolvingException {
            return new WebStudioWorkspaceRelatedDependencyManager(buildProjectDescriptors(),
                    classLoader,
                    isExecutionMode(),
                    externalParameters,
                    true);

        }
    }

    @Test
    void test() throws ProjectResolvingException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 5);

        final var factory = (WebStudioWorkspaceRelatedSimpleProjectEngineFactory<?>) (new WebStudioWorkspaceRelatedSimpleProjectEngineFactoryBuilder<>()
                .setProject("test/rules/compilation")
                .setExecutionMode(false)
                .build());

        var webStudioWorkspaceRelatedDependencyManager = (WebStudioWorkspaceRelatedDependencyManager) factory
                .getDependencyManager();
        var rnd = new Random();

        final var times = 200;
        var countDownLatch = new CountDownLatch(times);
        var c = 0;
        for (var i = 0; i < times; i++) {
            if (i % 4 == 0) {
                c++;
            }
        }
        var countDownLatchLambda = new CountDownLatch(c);
        var count = new AtomicLong(0);
        var count1 = new AtomicLong(0);
        for (var i = 0; i < times; i++) {
            final var i0 = i;
            executorService.submit(() -> {
                try {
                    if (i0 % 4 != 0) {
                        var p = (rnd.nextInt(3) + 1);
                        Collection<ResolvedDependency> resolvedDependencies;
                        try {
                            resolvedDependencies = webStudioWorkspaceRelatedDependencyManager
                                    .resolveDependency(
                                            new Dependency(DependencyType.MODULE,
                                                    new IdentifierNode(null, null, "Module" + p, null)),
                                            false);
                        } catch (OpenLCompilationException e) {
                            throw new RuntimeException(e);
                        }
                        webStudioWorkspaceRelatedDependencyManager.reset(resolvedDependencies.iterator().next());
                        try {
                            webStudioWorkspaceRelatedDependencyManager
                                    .loadDependency(resolvedDependencies.iterator().next());
                        } catch (OpenLCompilationException e) {
                            e.printStackTrace();
                        }
                    } else {
                        count1.incrementAndGet();
                        try {
                            Thread.sleep(rnd.nextInt(50));
                        } catch (InterruptedException ignored) {
                        }
                        try {
                            webStudioWorkspaceRelatedDependencyManager.loadDependencyAsync(
                                    AbstractDependencyManager.buildResolvedDependency(factory.getProjectDescriptor()),
                                    (e) -> {
                                        try {
                                            if (!e.getCompiledOpenClass().hasErrors()) {
                                                count.incrementAndGet();
                                            }
                                        } finally {
                                            countDownLatchLambda.countDown();
                                        }
                                    });
                        } catch (ProjectResolvingException e) {
                            e.printStackTrace();
                        }
                    }
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        countDownLatchLambda.await();
        assertEquals(count.get(), count1.get(), "Unexpected number of successfully compilations");
    }
}
