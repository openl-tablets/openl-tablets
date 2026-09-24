package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.webstudio.web.Props;
import org.openl.studio.projects.service.tables.TableTestProjects;

/** A test runs against what it was compiled into, and runs nothing once that is compiled again. */
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

    private void awaitCompiled() {
        try {
            model.getCurrentCompilation().future().join();
        } catch (CancellationException | CompletionException ended) {
            // However the compilation ended, what it built is what the model answers with.
        }
    }
}
