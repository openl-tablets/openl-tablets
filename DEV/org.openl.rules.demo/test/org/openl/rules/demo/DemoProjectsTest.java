package org.openl.rules.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.DefaultLocale;
import org.junitpioneer.jupiter.DefaultTimeZone;
import org.junitpioneer.jupiter.SetSystemProperty;

import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.test.RulesInFolderTestRunner;

/**
 * Compiles every demo project shipped with OpenL Studio and runs its test tables.
 *
 * <p>A project fails the build when it has a compilation error or a failed test. Each category is checked twice:
 * with the test tables run, and compiled in the execution mode that OpenL Rule Services uses.
 *
 * <p>The test tables run with dispatching validation on, as OpenL Studio runs them by default. The only version of a
 * rule with business dimension properties then runs only when the runtime context matches them. A rule called by its
 * id property runs whatever the runtime context is.
 */
@DefaultLocale("en-US")
@DefaultTimeZone("UTC")
final class DemoProjectsTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"examples", "templates", "tutorials"})
    @SetSystemProperty(key = OpenLSystemProperties.DISPATCHING_VALIDATION, value = "true")
    void testAll(String category) {
        assertFalse(new RulesInFolderTestRunner(false, false).run(categoryFolder(category)), "Test is failed.");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"examples", "templates", "tutorials"})
    void testAllInExecutionMode(String category) {
        assertFalse(new RulesInFolderTestRunner(false, true).run(categoryFolder(category)), "Test is failed.");
    }

    /**
     * Returns the folder with the demo projects of the category. The runner skips a missing folder silently, so its
     * absence fails the test here.
     */
    private static String categoryFolder(String category) {
        var folder = Path.of("src", "org.openl.rules.demo." + category);
        assertTrue(Files.isDirectory(folder), "The folder '%s' is not found.".formatted(folder));
        return folder + "/";
    }
}
