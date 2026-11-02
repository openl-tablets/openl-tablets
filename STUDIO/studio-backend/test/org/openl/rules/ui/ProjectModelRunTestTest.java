package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.webstudio.web.Props;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * A test runs against what it was compiled into, and runs nothing once that is compiled again. It holds nothing the
 * editor waits for, and what it runs on is let go of only once the case it runs has ended.
 */
class ProjectModelRunTestTest {

    private static final String[][] GREETINGS = {
            {"SimpleRules String greeting(String name)"},
            {"name", "greeting"},
            {"John", "Hi, John"},
            {},
            {"Test greeting greetingTest"},
            {"name", "_res_"},
            {"Name", "Greeting"},
            {"John", "Hi, John"},
    };

    private ProjectModel model;
    private Environment previousEnvironment;

    @BeforeEach
    void openModule(@TempDir Path dir) throws Exception {
        // A test run reads how many threads it may take from the settings.
        previousEnvironment = Props.getEnvironment();
        Props.setEnvironment(new MockEnvironment());
        TableTestProjects.writeProject(dir, "Rules", "Rules", GREETINGS);
        model = TableTestProjects.projectModel(dir);
        awaitCompiled();
    }

    @AfterEach
    void restoreSettings() {
        Props.setEnvironment(previousEnvironment);
    }

    @Test
    void runsATestWhileTheProjectIsCompiledTheWayTheTestWasRead() {
        var suite = new TestSuite(model.getAllTestMethods()[0]);

        var results = model.runTest(suite, false, model.getCompiledOpenClass());

        assertNotNull(results);
        assertEquals(TestStatus.TR_OK, results.getTestUnits().getFirst().getResultStatus());
    }

    @Test
    void runsATestOfTheOpenModuleWhileItIsCompiledTheWayTheTestWasRead() {
        var suite = new TestSuite(model.getOpenedModuleTestMethods()[0]);

        var results = model.runTest(suite, true, model.getOpenedModuleCompiledOpenClass());

        assertNotNull(results);
        assertEquals(TestStatus.TR_OK, results.getTestUnits().getFirst().getResultStatus());
    }

    @Test
    void runsNothingOnceTheProjectIsCompiledAgain() throws Exception {
        var suite = new TestSuite(model.getAllTestMethods()[0]);
        var compiled = model.getCompiledOpenClass();
        var openedModule = model.getOpenedModuleCompiledOpenClass();

        model.reset(ReloadType.SINGLE, model.getModuleInfo());
        awaitCompiled();

        assertNull(model.runTest(suite, false, compiled));
        assertNull(model.runTest(suite, true, openedModule));
    }

    @Test
    void runsNothingOfTheOpenModuleOnceTheProjectIsClosed() {
        var suite = mock(TestSuite.class);
        var openedModule = model.getOpenedModuleCompiledOpenClass();

        model.clearModuleInfo();

        // The model still hands out what the module was compiled into, but its classes are let go of.
        assertSame(openedModule, model.getOpenedModuleCompiledOpenClass());
        assertFalse(model.isCompiledAsRead(openedModule, true));
        assertNull(model.runTest(suite, true, openedModule));
        verifyNoInteractions(suite);
    }

    @Test
    void runsATestWhileAnotherRequestOfTheSessionHoldsTheProject() throws Exception {
        var suite = new TestSuite(model.getAllTestMethods()[0]);
        var compiled = model.getCompiledOpenClass();
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var holder = Thread.ofVirtual().start(() -> {
            synchronized (model) {
                held.countDown();
                awaitQuietly(release);
            }
        });
        try {
            assertTrue(held.await(5, TimeUnit.SECONDS));

            var results = CompletableFuture.supplyAsync(() -> model.runTest(suite, false, compiled));

            assertNotNull(results.get(30, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            holder.join();
        }
    }

    @Test
    void closingTheProjectStopsARunningTestAndWaitsForTheCaseItRuns() throws Exception {
        var running = new CountDownLatch(1);
        var caseEnded = new AtomicBoolean();
        var suite = mock(TestSuite.class);
        when(suite.invokeSequentially(any(), eq(1))).thenAnswer(call -> {
            running.countDown();
            try {
                // The case runs until it is asked to stop.
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                // The case running ends a moment later, on the classes it started with. The interruption is taken
                // back afterwards: a pause would end at once while the thread is interrupted.
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(200));
                caseEnded.set(true);
                Thread.currentThread().interrupt();
            }
            return new TestUnitsResults(suite);
        });
        var test = CompletableFuture.supplyAsync(() -> model.runTest(suite, false));
        assertTrue(running.await(5, TimeUnit.SECONDS));

        model.clearModuleInfo();

        assertTrue(caseEnded.get());
        assertNotNull(test.get(5, TimeUnit.SECONDS));
    }

    @Test
    void startsNoTestWhileWhatWasCompiledIsLetGoOf() throws Exception {
        var compiled = model.getCompiledOpenClass();
        var running = new CountDownLatch(1);
        var releaseBegun = new CountDownLatch(1);
        var endCase = new CountDownLatch(1);
        var suite = mock(TestSuite.class);
        when(suite.invokeSequentially(any(), eq(1))).thenAnswer(call -> {
            running.countDown();
            try {
                // The case runs until it is asked to stop.
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                // Asked to stop by the release, the case runs on until the test lets it end, and the project is let
                // go of only then.
                releaseBegun.countDown();
                awaitQuietly(endCase);
                Thread.currentThread().interrupt();
            }
            return new TestUnitsResults(suite);
        });
        var test = CompletableFuture.supplyAsync(() -> model.runTest(suite, false));
        assertTrue(running.await(5, TimeUnit.SECONDS));
        var closing = Thread.ofPlatform().start(model::clearModuleInfo);
        assertTrue(releaseBegun.await(5, TimeUnit.SECONDS));
        var later = mock(TestSuite.class);

        // Asked for while the project is let go of, the next test waits, and then finds the project gone.
        var next = CompletableFuture.supplyAsync(() -> model.runTest(later, false, compiled));
        // Closing asked to stop still waits for the case running.
        closing.interrupt();
        assertThrows(TimeoutException.class, () -> next.get(200, TimeUnit.MILLISECONDS));
        assertTrue(closing.isAlive());
        endCase.countDown();

        assertTrue(closing.join(Duration.ofSeconds(5)));
        assertNotNull(test.get(5, TimeUnit.SECONDS));
        assertNull(next.get(5, TimeUnit.SECONDS));
        verifyNoInteractions(later);
    }

    @Test
    void runsAnExecutionWithTheClassesOfTheProject() {
        var loader = model.execute(() -> Thread.currentThread().getContextClassLoader());

        assertSame(model.getCompiledOpenClass().getClassLoader(), loader);
        assertNotSame(loader, Thread.currentThread().getContextClassLoader());
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void awaitCompiled() {
        try {
            model.getCurrentCompilation().future().join();
        } catch (CancellationException | CompletionException ended) {
            // However the compilation ended, what it built is what the model answers with.
        }
    }
}
