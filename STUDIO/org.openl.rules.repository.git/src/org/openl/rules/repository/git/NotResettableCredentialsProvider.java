package org.openl.rules.repository.git;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotResettableCredentialsProvider extends UsernamePasswordCredentialsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(NotResettableCredentialsProvider.class);

    private boolean hasAuthorizationFailure = false;

    NotResettableCredentialsProvider(String username, String password) {
        super(username, password);
    }

    @Override
    public void reset(URIish uri) {
        // This method is called when authentication attempt was unsuccessful and need to provide correct credentials.
        // Our application works in non-interactive mode so we just throw exception.
        LOG.info("Reset the credentials provider for the URI: {}", uri);
        hasAuthorizationFailure = true;
        throw new InvalidCredentialsException(JGitText.get().notAuthorized);
    }

    boolean isHasAuthorizationFailure() {
        return hasAuthorizationFailure;
    }

    @Override
    public void clear() {
        // Do nothing to ensure that username and password isn't cleared.
        LOG.warn("clear() method should never be invoked.");
    }
}
