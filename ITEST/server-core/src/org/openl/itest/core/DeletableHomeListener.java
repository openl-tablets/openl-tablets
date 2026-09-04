package org.openl.itest.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Leaves the application state a suite writes to {@code target} deletable by the build.
 *
 * <p>A design repository the application creates on the file system is a Git repository, and Git marks the objects
 * it stores read-only. On Windows that attribute alone forbids deletion, so the next {@code mvn clean} fails on the
 * first object it meets. The attribute is cleared when the run ends, the way JUnit clears it for its own temporary
 * directories. The files themselves stay, so a failed suite can still be looked into.
 *
 * <p>The home the build configures still carries a placeholder the application resolves at its own start
 * ({@code openl-test-${openl.start.milli}}), and a suite that starts the application more than once leaves a home
 * per start. So every home the configured name begins with is cleared, the ones earlier runs left among them.
 *
 * <p>The listener is registered through {@code META-INF/services}, so every suite gets it with no wiring of its own.
 */
public class DeletableHomeListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        var configured = System.getProperty("openl.home");
        if (configured == null || configured.isBlank()) {
            return;
        }
        clearReadOnly(namedBefore(configured));
    }

    /**
     * The name a home starts with, cut at the placeholder the application resolves on its own.
     */
    private static Path namedBefore(String configured) {
        var placeholder = configured.indexOf("${");
        return Path.of(placeholder < 0 ? configured : configured.substring(0, placeholder));
    }

    private static void clearReadOnly(Path name) {
        var directory = name.getParent();
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        try (var homes = Files.list(directory)) {
            homes.filter(home -> home.getFileName().toString().startsWith(name.getFileName().toString()))
                    .forEach(DeletableHomeListener::clearReadOnlyWithin);
        } catch (IOException e) {
            // The build reports the file it cannot delete, so a failure here needs no report of its own.
        }
    }

    private static void clearReadOnlyWithin(Path home) {
        try (var tree = Files.walk(home)) {
            tree.forEach(DeletableHomeListener::clearReadOnlyOf);
        } catch (IOException e) {
            // The build reports the file it cannot delete, so a failure here needs no report of its own.
        }
    }

    /**
     * Clears the read-only attribute of one file. The attribute is absent on a file system that keeps no DOS
     * attributes, where deletion asks for permission to the directory instead.
     */
    private static void clearReadOnlyOf(Path path) {
        var attributes = Files.getFileAttributeView(path, DosFileAttributeView.class);
        if (attributes == null) {
            return;
        }
        try {
            attributes.setReadOnly(false);
        } catch (IOException e) {
            // The build reports the file it cannot delete, so a failure here needs no report of its own.
        }
    }
}
