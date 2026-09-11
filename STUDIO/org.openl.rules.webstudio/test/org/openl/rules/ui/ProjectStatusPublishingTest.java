package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import org.openl.rules.project.abstraction.RulesProject;
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
        var studio = mock(WebStudio.class);
        var project = mock(RulesProject.class);
        ApplicationEventPublisher publisher = event -> {
            if (event instanceof ProjectStatusChangedEvent status) {
                published.add(new Published(status.isProgressOnly(), Thread.currentThread().getName()));
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

    /** Waits for a status matching the given rule, which the notifier delivers on a thread of its own. */
    private static boolean waitFor(List<Published> published, Predicate<Published> rule) throws Exception {
        var deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (published.stream().anyMatch(rule)) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }
}
