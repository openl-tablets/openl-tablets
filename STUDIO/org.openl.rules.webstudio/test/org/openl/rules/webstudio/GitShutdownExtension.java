package org.openl.rules.webstudio;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Stops JGit once, after the whole test run, so the JGit worker thread does not outlive the test JVM.
 *
 * <p>The cleanup is registered on the root context, so it runs only when the test session ends — never per test class.
 * The JGit work queue is process-wide and terminal, so shutting it down between classes would break the git tests that
 * run afterwards in the same JVM.
 *
 * <p>Auto-detected through {@code META-INF/services} and {@code junit-platform.properties}; no test needs to reference
 * it explicitly.
 */
public class GitShutdownExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot()
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .computeIfAbsent(GitShutdownExtension.class, key -> (AutoCloseable) Git::shutdown);
    }
}
