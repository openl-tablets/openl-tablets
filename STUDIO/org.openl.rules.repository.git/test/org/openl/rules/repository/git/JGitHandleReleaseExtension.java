package org.openl.rules.repository.git;

import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Releases JGit file handles after every test so temporary repositories can be deleted on Windows.
 *
 * <p>JGit keeps {@code .pack} files open through a process-wide repository cache and window cache. Closing a repository
 * is not always enough: a cached repository or a cached window keeps the underlying file open. On Windows an open file
 * cannot be deleted, so the temporary folder of a finished test stays locked. Clearing both caches closes those files.
 *
 * <p>This runs between tests and is safe to repeat. Unlike {@link GitShutdownExtension}, it does not stop the shared
 * JGit work queue, so later tests keep working.
 *
 * <p>Auto-detected through {@code META-INF/services} and {@code junit-platform.properties}; no test needs to reference
 * it explicitly.
 */
public class JGitHandleReleaseExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) {
        RepositoryCache.clear();
        new WindowCacheConfig().install();
    }
}
