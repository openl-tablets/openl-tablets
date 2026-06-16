package org.openl.rules.repository.git;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class JGitCleanupTest {

    @Test
    void shutdownReleasesJGitResources() {
        // Git.shutdown() is process-wide and terminal, so it is mocked: this verifies the delegation without actually
        // tearing JGit down for the rest of the test run.
        try (MockedStatic<Git> git = Mockito.mockStatic(Git.class)) {
            new JGitCleanup().shutdown();
            git.verify(Git::shutdown);
        }
    }
}
