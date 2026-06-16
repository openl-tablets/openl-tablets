package org.openl.rules.repository.git;

import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;

/**
 * Releases JGit's process-wide resources when the application context closes.
 *
 * <p>JGit keeps a background worker thread and a thread-inheritable {@code NLS} localization cache. A servlet container
 * reports both as memory leaks when the application is undeployed. {@link Git#shutdown()} stops the worker and clears
 * the cache.
 *
 * <p>The shutdown is process-wide and irreversible, so it runs once when the context closes — never from
 * {@link GitRepository#close()}, which is per-instance and would tear the shared worker down under other live
 * repositories, such as the instances created by {@link GitRepository#forBranch(String)}.
 *
 * <p>This bean is contributed to the rule services context through {@code META-INF/openl/extension-git-beans.xml}, so it
 * is active only when the git repository is on the classpath.
 */
@Slf4j
public class JGitCleanup {

    @PreDestroy
    public void shutdown() {
        log.info("Releasing JGit process-wide resources (worker thread and NLS cache) on context shutdown.");
        Git.shutdown();
    }
}
