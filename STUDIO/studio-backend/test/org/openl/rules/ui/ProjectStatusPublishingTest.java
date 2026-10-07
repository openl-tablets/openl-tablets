package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;

/**
 * How a compilation reports on itself.
 *
 * <p>Opening a module compiles it while the model's own monitor is held, which on a large project takes
 * minutes, and reading the status needs that same monitor. A status handed to another thread would therefore
 * wait for the compilation it reports on and arrive as a burst once it ended — which is no progress at all.
 */
class ProjectStatusPublishingTest {

    private static final Path PROJECT = Path.of("test/rules/EPBDS-16463");

    /** One status as it was published: what it could tell, and the thread that told it. */
    private record Published(boolean progressOnly, String thread) {
    }

    @Test
    void progress_leaves_the_model_while_the_module_is_still_compiling() throws Exception {
        var published = new CopyOnWriteArrayList<Published>();
        var studio = studioPublishing(published);
        var model = new ProjectModel(studio, null);
        var modules = ProjectResolver.getInstance().resolve(PROJECT).getModules();

        model.setModuleInfo(modules.getFirst());

        // Published by the thread doing the compiling, before setModuleInfo returned — not by the notifier
        // afterwards, which is where every update used to queue up.
        var whileCompiling = published.stream().filter(Published::progressOnly).toList();
        assertFalse(whileCompiling.isEmpty(), "a compilation must report its progress while it runs");
        assertEquals(List.of(Thread.currentThread().getName()),
                whileCompiling.stream().map(Published::thread).distinct().toList());
    }

    /** A studio that records every status the model publishes, as the WebSocket publisher would receive it. */
    private static WebStudio studioPublishing(List<Published> published) {
        return studioPublishing(published, status -> {
        });
    }

    /**
     * The same, with something to do as each status is published — which is the one moment a test can act while
     * the compilation is between its steps.
     */
    private static WebStudio studioPublishing(List<Published> published, Consumer<Published> onPublished) {
        var studio = mock(WebStudio.class);
        var project = mock(RulesProject.class);
        ApplicationEventPublisher publisher = event -> {
            if (event instanceof ProjectStatusChangedEvent status) {
                var told = new Published(status.isProgressOnly(), Thread.currentThread().getName());
                published.add(told);
                onPublished.accept(told);
            }
        };
        when(studio.getEventPublisher()).thenReturn(publisher);
        when(studio.getCurrentProject()).thenReturn(project);
        when(studio.getCurrentUsername()).thenReturn("jane");
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(studio).runAsSessionUser(any());
        return studio;
    }

    @Test
    void a_status_told_after_the_compilation_tells_everything() throws Exception {
        var published = new CopyOnWriteArrayList<Published>();
        var model = new ProjectModel(studioPublishing(published), null);
        var modules = ProjectResolver.getInstance().resolve(PROJECT).getModules();

        model.setModuleInfo(modules.getFirst());
        model.compileProject(true, false);

        // Progress is what a compilation can tell about itself; the whole status follows it, so a compile
        // that publishes nothing more of its own still leaves the reader with everything.
        assertTrue(waitFor(published, status -> !status.progressOnly()),
                "the status that follows a compilation must carry more than its progress");
    }

    @Test
    void the_status_answers_while_a_compilation_holds_the_model() throws Exception {
        var model = new ProjectModel(studioPublishing(new CopyOnWriteArrayList<>()), null);
        var holding = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var compiling = new Thread(() -> {
            synchronized (model) {
                holding.countDown();
                try {
                    release.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "compiling-thread");
        compiling.start();
        assertTrue(holding.await(5, TimeUnit.SECONDS), "the compiling thread must take the model first");

        try {
            // Asking a project how its compilation is going is what every screen does, and a compilation holds
            // the model for minutes — so the question is answered without waiting for it.
            var answered = CompletableFuture.supplyAsync(model::getCompilationStatus);
            assertNotNull(answered.get(5, TimeUnit.SECONDS), "a status read must not wait for the compilation");
        } finally {
            release.countDown();
            compiling.join();
        }
    }

    @Test
    void a_compilation_told_to_stop_compiles_no_more_until_it_is_asked_for_again() throws Exception {
        var published = new CopyOnWriteArrayList<Published>();
        var model = new ProjectModel(studioPublishing(published), null);
        var modules = ProjectResolver.getInstance().resolve(PROJECT).getModules();
        model.setModuleInfo(modules.getFirst());

        model.cancelCompilation();

        // Nothing more is compiled, and the status can tell that from a compilation still running.
        assertTrue(model.isCompilationCancelled(), "a compilation told to stop is marked as stopped");
        assertFalse(model.isCompilationInProgress(), "a compilation told to stop is no longer running");
        assertFalse(model.getWebStudioWorkspaceDependencyManager().isActive(), "the manager compiles no more");
        assertTrue(waitFor(published, status -> true), "the reader is told the compilation stopped");

        // Asking for the module again builds it: what was stopped is replaced rather than reused.
        model.setModuleInfo(modules.getFirst(), ReloadType.RELOAD);

        assertFalse(model.isCompilationCancelled(), "asking for a compilation clears the stop");
        assertTrue(model.getWebStudioWorkspaceDependencyManager().isActive(), "the new manager compiles");
    }

    @Test
    void asking_for_the_open_module_again_compiles_nothing() throws Exception {
        var published = new CopyOnWriteArrayList<Published>();
        var model = new ProjectModel(studioPublishing(published), null);
        model.setModuleInfo(ProjectResolver.getInstance().resolve(PROJECT).getModules().getFirst());
        assertNotNull(model.getXlsModuleNode(), "the module the reader opened is compiled");
        // The project's own compilation follows the module's and reports on itself; it is left to finish.
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> !model.isCompilationInProgress());
        Awaitility.await().during(Duration.ofMillis(300)).atMost(Duration.ofSeconds(5))
                .until(publishedCount(published), count -> count.equals(published.size()));
        var told = published.size();

        // The descriptors are resolved again on every workspace refresh, so a request asking for the same
        // module brings a new object naming it. Taking that for another module would compile it once more —
        // and every read that names a module would set the status back to compiling.
        var open = model.getModuleInfo();
        model.setModuleInfo(namedAgain(open));

        assertSame(open, model.getModuleInfo(), "the module stays the one that is open");
        assertEquals(told, published.size(), "asking for the open module again starts no compilation");
    }

    @Test
    void a_stop_that_lands_mid_compilation_leaves_the_open_module_readable() throws Exception {
        var model = new AtomicReference<ProjectModel>();
        var stops = new AtomicInteger();
        // The stop is asked for from inside the compilation's own progress report, so the load the compiling
        // thread is on lands after it — which is where what it had compiled used to be thrown away.
        var studio = studioPublishing(new CopyOnWriteArrayList<>(), status -> {
            var current = model.get();
            if (current != null && current.isCompilationInProgress() && stops.incrementAndGet() == 1) {
                current.cancelCompilation();
            }
        });
        model.set(new ProjectModel(studio, null));
        var modules = ProjectResolver.getInstance().resolve(PROJECT).getModules();

        model.get().setModuleInfo(modules.getFirst());
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> !model.get().isCompilationInProgress());

        assertTrue(model.get().isCompilationCancelled(), "the compilation was stopped while it ran");
        assertTrue(model.get().isOpenedModuleCompiledSuccessfully(),
                "the module the reader has open stays readable after the stop");
        assertNotNull(model.get().getXlsModuleNode(), "its tables are still there to read");
        assertFalse(model.get().isProjectCompilationCompleted(),
                "a stopped compilation has not compiled the project through");
    }

    @Test
    void a_compilation_told_to_stop_leaves_the_module_it_had_compiled_readable() throws Exception {
        var model = new ProjectModel(studioPublishing(new CopyOnWriteArrayList<>()), null);
        var modules = ProjectResolver.getInstance().resolve(PROJECT).getModules();
        // Opening a module compiles that module; the rest of the project follows it.
        model.setModuleInfo(modules.getFirst());
        assertTrue(model.isOpenedModuleCompiledSuccessfully(), "the module the reader opened is compiled");

        model.cancelCompilation();
        // Whatever the stopped compilation still had on its way lands after the stop and must change nothing.
        model.compileProject(true, false);

        assertTrue(model.isOpenedModuleCompiledSuccessfully(),
                "what was compiled stays readable after the reader stops the compilation");
        assertNotNull(model.getXlsModuleNode(), "the tables of the open module are still there to read");
        // And the project is not passed off as compiled through, which would send every reader to an empty class.
        assertFalse(model.isProjectCompilationCompleted(), "a stopped compilation has not compiled the project");
    }

    /** The same module, as a descriptor resolved afresh describes it: another object naming the same thing. */
    private static Module namedAgain(Module module) {
        var again = new Module();
        again.setName(module.getName());
        again.setRulesRootPath(module.getRulesRootPath());
        again.setProject(module.getProject());
        again.setWebstudioConfiguration(module.getWebstudioConfiguration());
        return again;
    }

    /** How many statuses have been published, read again on each poll. */
    private static java.util.concurrent.Callable<Integer> publishedCount(List<Published> published) {
        return published::size;
    }

    /** Waits for a status matching the given rule, which the notifier delivers on a thread of its own. */
    private static boolean waitFor(List<Published> published, Predicate<Published> rule) {
        try {
            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .pollInterval(Duration.ofMillis(50))
                    .until(() -> published.stream().anyMatch(rule));
            return true;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }
}
