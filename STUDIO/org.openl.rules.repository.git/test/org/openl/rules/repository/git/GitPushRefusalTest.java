package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.Test;

/**
 * A push the Git server refuses for the account it is made with is told apart from other transport failures, and
 * reported with the user it was refused to.
 *
 * @author Yury Molchan
 */
class GitPushRefusalTest {

    @Test
    void refusalsOfTheGitServersAreRecognized() {
        assertTrue(GitRepository.isRefused(new TransportException("https://example.com/r.git: not authorized")));
        assertTrue(GitRepository.isRefused(
                new TransportException("https://example.com/r.git: git-receive-pack not permitted on 'x'")));
        assertTrue(GitRepository.isRefused(new TransportException(
                "https://example.com/r.git: Authentication is required but no CredentialsProvider has been registered")));
        assertTrue(GitRepository.isRefused(new TransportException("https://example.com/r.git: 403 Forbidden")));
        assertTrue(GitRepository.isRefused(new TransportException("https://example.com/r.git: 401 Unauthorized")));
        assertFalse(GitRepository.isRefused(new TransportException("https://example.com/r.git: connection reset")));
        assertFalse(GitRepository.isRefused(new TransportException("https://example.com/401.git: connection reset")));
        assertFalse(GitRepository.isRefused(new TransportException("https://example.com:403/r.git: connection reset")));
        assertFalse(GitRepository.isRefused(new TransportException("connection reset for https://example.com:403")));
    }

    @Test
    void theRefusalNamesTheUser() {
        assertEquals("It does not let the user 'reader' write to the repository: check the password and the access "
                + "of the user.", GitRepository.refusedMessage("reader"));
        assertEquals("It does not let the account set for the repository write to the repository: check the password "
                + "and the access of the user.", GitRepository.refusedMessage(null));
    }
}
