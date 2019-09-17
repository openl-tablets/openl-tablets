package org.openl.rules.repository.git;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class NotResettableCredentialsProvider extends UsernamePasswordCredentialsProvider {
    private boolean hasAuthorizationFailure = false;

    NotResettableCredentialsProvider(String username, String password) {
        super(username, password);
    }

    @Override
    public void reset(URIish uri) {
        // This method is called when authentication attempt was unsuccessful and need to provide correct credentials.
        // Our application works in non-interactive mode so we just throw exception.
        hasAuthorizationFailure = true;
        throw new InvalidCredentialsException(JGitText.get().notAuthorized);
    }

    boolean isHasAuthorizationFailure() {
        return hasAuthorizationFailure;
    }
}
